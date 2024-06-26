package nl.tno.federated.api.event.distribution

import nl.tno.federated.api.corda.CordaNodeService
import nl.tno.federated.api.event.distribution.corda.CordaEventDestination
import nl.tno.federated.api.event.distribution.rules.BroadcastEventDistributionRule
import nl.tno.federated.api.event.distribution.rules.EventDistributionRule
import nl.tno.federated.api.event.distribution.rules.FailedEventDistributionRule
import nl.tno.federated.api.event.distribution.rules.SparqlEventDistributionRule
import nl.tno.federated.api.event.distribution.rules.StaticDestinationEventDistributionRule
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

class UnsupportedEventDistributionRuleType(msg: String) : Exception(msg)
class EventDistributionRuleConfigurationException(msg: String) : Exception(msg)

@Component
class EventDistributionRuleConfiguration(
    private val environment: Environment,
    private val cordaNodeService: CordaNodeService
) {

    private val log = LoggerFactory.getLogger(EventDistributionRuleConfiguration::class.java)
    private val rules: List<EventDistributionRule<CordaEventDestination>> = setupRules()

    private fun setupRules(): List<EventDistributionRule<CordaEventDestination>> {
        val userDefinedRulesList = environment.getProperty("federated.node.event.distribution.rules.list")?.trim()?.split(",")
        return if (userDefinedRulesList.isNullOrEmpty()) listOf(broadcastEventDistributionRule())
        else mutableListOf<EventDistributionRule<CordaEventDestination>>().apply {
            userDefinedRulesList.forEach {
                when (it.lowercase()) {
                    "static" -> add(staticDestinationEventDistributionRule())
                    "broadcast" -> add(broadcastEventDistributionRule())
                    "sparql" -> add(sparqlEventDistributionRule())
                    else -> throw UnsupportedEventDistributionRuleType("$it is not a supported rule type")
                }
            }
        }
    }

    private fun broadcastEventDistributionRule(): EventDistributionRule<CordaEventDestination> {
        return try {
            BroadcastEventDistributionRule(cordaNodeService)
        } catch (e: Exception) {
            log.error("Failed to add distribution rule: BroadcastEventDistributionRule, error: ${e.message}", e)
            FailedEventDistributionRule(e.message)
        }
    }

    private fun sparqlEventDistributionRule(): SparqlEventDistributionRule {
        TODO("SparqlEventDistributionRule not configurable yet")
    }

    private fun staticDestinationEventDistributionRule(): StaticDestinationEventDistributionRule {
        val destinations =
            environment.getProperty("federated.node.event.distribution.rules.static.destinations")?.trim()?.split(";") ?: throw EventDistributionRuleConfigurationException("No static destinations defined for static destination rule")
        val cordaEventDestinations = destinations.map { CordaEventDestination.parse(it) }.toSet()
        return StaticDestinationEventDistributionRule(cordaEventDestinations)
    }

    fun getDistributionRules(): List<EventDistributionRule<CordaEventDestination>> {
        return rules
    }
}