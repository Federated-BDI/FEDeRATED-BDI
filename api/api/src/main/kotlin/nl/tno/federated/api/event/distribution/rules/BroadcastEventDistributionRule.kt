package nl.tno.federated.api.event.distribution.rules

import com.fasterxml.jackson.annotation.JsonIgnore
import nl.tno.federated.api.corda.CordaNodeService
import nl.tno.federated.api.event.distribution.corda.CordaEventDestination
import org.slf4j.LoggerFactory

/**
 * This rule refreshes its destinations list based on the latest networkMapSnapshot.
 */
class BroadcastEventDistributionRule(@JsonIgnore private val cordaNodeService: CordaNodeService) : EventDistributionRule {

    private val log = LoggerFactory.getLogger(BroadcastEventDistributionRule::class.java)

    override fun getDestinations() = try {
        cordaNodeService.getPeersExcludingSelfAndNotary().map { it.name.toString() }.toSet()
    }catch (e: Exception) {
        log.error("Failed to retrieve destinations for BroadcastEventDistributionRule: ${e.message}")
        emptySet()
    }

    override fun appliesTo(ttl: String): Boolean = true

    override fun toString(): String {
        val cordaEventDestinations = try {
            getDestinations()
        } catch (e: Exception) {
            emptySet<CordaEventDestination>()
        }
        return "BroadcastToAllEventDistributionRule(destinations='${cordaEventDestinations.joinToString(",")}')"
    }
}