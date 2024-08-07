package nl.tno.federated.api.controllers

import nl.tno.federated.api.corda.NodeRPCConnection
import nl.tno.federated.api.event.distribution.rules.EventDistributionRuleService
import nl.tno.federated.api.webhook.WebhookService
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class IndexController(
    private val environment: Environment,
    private val rules: EventDistributionRuleService,
    private val rpc: NodeRPCConnection,
    private val webhookService: WebhookService
) {

    companion object {
        private val log = LoggerFactory.getLogger(IndexController::class.java)
    }

    @RequestMapping("/")
    fun index(model: Model): String {
        model.addAttribute("version", environment.getProperty("federated.node.version"))
        model.addAttribute("graphdbSparqlUrl", environment.getProperty("federated.node.graphdb.sparql.url"))
        model.addAttribute("cordaNmsUrl", environment.getProperty("federated.node.corda.nms.url"))
        model.addAttribute("cordaRpcUrl", (environment.getProperty("federated.node.corda.rpc.host") ?: "localhost") + ":" + environment.getProperty("federated.node.corda.rpc.port"))
        model.addAttribute("eventDistributionRules", getDistributionRules())
        model.addAttribute("identities", getIdentities())
        model.addAttribute("notaries", getNotaries())
        model.addAttribute("peers", getPeers())
        model.addAttribute("webhooks", webhookService.getWebhooks())
        return "index"
    }

    private fun getDistributionRules() = rules.getDistributionRules()

    private fun getIdentities() : String {
        return try {
            rpc.client().nodeInfo().legalIdentities.map { it.name }.jts()
        } catch (e: Exception) {
            log.warn(e.message)
            "ERROR: Cant retrieve information about the identities, maybe the Corda RPC URL is not reachable? See logs for details."
        }
    }

    private fun getNotaries() : String {
        return try {
            rpc.client().notaryIdentities().map { it.name }.jts()
        } catch (e: Exception) {
            log.warn(e.message)
            "ERROR: Cant retrieve information about notaries, maybe the Corda RPC URL is not reachable? See logs for details."
        }
    }

    private fun getPeers() : String {
        return try {
            rpc.client().networkMapSnapshot()
                .flatMap { it.legalIdentities }
                .minus(rpc.client().nodeInfo().legalIdentities.toSet())
                .minus(rpc.client().notaryIdentities().toSet())
                .map { it.name }.jts()
        } catch (e: Exception) {
            log.warn(e.message)
            "ERROR: Cant retrieve information about peers, maybe the Corda RPC URL is not reachable? See logs for details."
        }
    }
}

private fun <T> Iterable<T>.jts(transform: ((T) -> CharSequence)? = null): String = this.joinToString(prefix = "[", postfix = "]", separator = "], [", transform = transform)