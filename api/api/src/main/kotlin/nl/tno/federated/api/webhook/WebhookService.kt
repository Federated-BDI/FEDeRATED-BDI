package nl.tno.federated.api.webhook

import com.fasterxml.jackson.annotation.JsonIgnore
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

data class GenericEvent<T>(val eventType: String, @JsonIgnore val eventData: T, val eventUUID: String)

@Service
class WebhookService(private val webhookHttpClient: WebhookHttpClient) {

    /**
     * This method is being invoked whenever new GenericEvent's are published by the ApplicationEventPublisher
     */
    @EventListener
    fun handleEvent(event: GenericEvent<Any>) {
        log.info("Event of type: {} with UUID: {} received for publication...", event.eventType, event.eventUUID)
        val filter = webhooks.values.filter { it.eventType == event.eventType }
        log.info("{} webhooks registered for eventType: {}", filter.size, event.eventType)
        filter.forEach { webhookHttpClient.send(event, it) }
    }

    fun getWebhooks(): List<Webhook> {
        return webhooks.values.toList()
    }

    fun register(registration: Webhook) {
        webhooks[registration.clientId] = registration
    }

    fun unregister(clientId: String): Boolean {
        return webhooks.remove(clientId) != null
    }

    companion object {
        private val webhooks = ConcurrentHashMap<String, Webhook>()
        private val log = LoggerFactory.getLogger(WebhookService::class.java)
    }
}