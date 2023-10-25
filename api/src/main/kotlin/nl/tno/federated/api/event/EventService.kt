package nl.tno.federated.api.event

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.node.ObjectNode
import nl.tno.federated.api.event.distribution.corda.CordaEventDestination
import nl.tno.federated.api.event.distribution.corda.CordaEventDistributionService
import nl.tno.federated.api.event.mapper.EventMapper
import nl.tno.federated.api.event.mapper.EventType
import nl.tno.federated.api.event.mapper.UnsupportedEventTypeException
import nl.tno.federated.api.event.query.EventQuery
import nl.tno.federated.api.event.query.corda.CordaEventQueryService
import nl.tno.federated.api.event.validation.ShaclValidator
import org.springframework.stereotype.Service
import java.util.*

const val EVENT_UUID_FIELD = "UUID"
const val EVENT_TYPE_FIELD = "eventType"

@Service
class EventService(
    private val eventMapper: EventMapper,
    private val eventDistributionService: CordaEventDistributionService,
    private val eventQueryService: CordaEventQueryService,
    private val eventTypeMapping: EventTypeMapping
) {

    private val shaclValidator = ShaclValidator(eventTypeMapping.readShaclShapes())

    /**
     * Convert the given event to RDF.
     *
     * @throws UnsupportedEventTypeException is an unsupported Event type is encountered.
     */
    fun newJsonEvent(event: String, eventType: EventType, eventDestinations: Set<String>? = null): EnrichedEvent {
        val enrichedEvent = enrichJsonEvent(event, eventType)
        validateWithShacl(enrichedEvent)
        publishRDFEvent(enrichedEvent, eventDestinations)
        return enrichedEvent
    }

    fun validateNewJsonEvent(event: String, eventType: EventType): EnrichedEvent {
        val enrichedEvent = enrichJsonEvent(event, eventType)
        validateWithShacl(enrichedEvent)
        return enrichedEvent
    }

    fun findEventById(id: String): JsonNode? {
        val rdf = eventQueryService.findById(id) ?: return null
        return eventMapper.toCompactedJSONLDMap(rdf)
    }

    fun findAll(page: Int, size: Int): List<JsonNode> {
        val result = eventQueryService.findAll(page, size)
        return result.map { eventMapper.toCompactedJSONLDMap(it) }
    }

    fun query(eventQuery: EventQuery): JsonNode? {
        val rdf = eventQueryService.executeQuery(eventQuery) ?: return null
        return eventMapper.toCompactedJSONLDMap(rdf)
    }

    private fun enrichJsonEvent(jsonEvent: String, eventType: EventType) : EnrichedEvent {
        val node = eventMapper.toJsonNode(jsonEvent)
        if(node.nodeType != JsonNodeType.OBJECT) throw UnsupportedEventTypeException("Unexpected Event content, not parsable as JSON!")

        node as ObjectNode
        val uuid = UUID.randomUUID()
        node.put(EVENT_UUID_FIELD, uuid.toString())
        node.put(EVENT_TYPE_FIELD, eventType.name)

        val rdf = eventMapper.toRDFTurtle(jsonNode = node, eventType = eventType)
        return EnrichedEvent(jsonEvent, eventType, uuid, rdf)
    }

    private fun validateWithShacl(enrichedEvent: EnrichedEvent) {
        if(enrichedEvent.eventType.shacl != null) shaclValidator.validate(enrichedEvent.eventRDF)
    }

    private fun publishRDFEvent(enrichedEvent: EnrichedEvent, destinations: Set<String>? = null): UUID {
        val dest = destinations?.map { CordaEventDestination.parse(it) }?.toSet()
        return eventDistributionService.distributeEvent(enrichedEvent = enrichedEvent, destinations = dest)
    }
}