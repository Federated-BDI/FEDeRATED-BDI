package nl.tno.federated.corda.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.ReceiveFinalityFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step
import nl.tno.federated.corda.contracts.EventContract
import nl.tno.federated.corda.services.ishare.ISHARECordaService
import nl.tno.federated.corda.states.EventState
import org.slf4j.LoggerFactory

@StartableByRPC
@InitiatingFlow
class NewEventFlow(
    val destinations: Collection<CordaX500Name>,
    val event: String,
    val eventType: String,
    val eventUUID: String
) : FlowLogic<SignedTransaction>() {

    private val log = LoggerFactory.getLogger(NewEventFlow::class.java)

    /**
     * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
     * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call() function.
     */
    companion object {
        object GENERATING_TRANSACTION : Step("Generating transaction.")
        object VERIFYING_TRANSACTION : Step("Verifying contract constraints.")
        object SIGNING_TRANSACTION : Step("Signing transaction with our private key.")
        object GATHERING_SIGS : Step("Gathering the counterparty's signature.") {
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }

        object FINALISING_TRANSACTION : Step("Obtaining notary signature and recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
            GENERATING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            GATHERING_SIGS,
            FINALISING_TRANSACTION
        )
    }

    override val progressTracker = tracker()

    /**
     * The flow logic is encapsulated within the call() method.
     */
    @Suspendable
    override fun call(): SignedTransaction {
        val notaryIdentities = serviceHub.networkMapCache.notaryIdentities
        if (notaryIdentities.isEmpty()) throw FlowException("Expected at least one Notary, but none are present in the NetworkMap.")

        val notary = notaryIdentities.first()

        // Stage 1.
        progressTracker.currentStep = GENERATING_TRANSACTION

        // Retrieving counterparties (sending to all nodes, for now)
        val me = serviceHub.myInfo.legalIdentities.first()
        val counterParties = findMatchingParties(destinations)

        val newEventState = EventState(
            event = event,
            eventType = eventType,
            participants = listOf(me) + counterParties,
            linearId = UniqueIdentifier(externalId = eventUUID)
        )

        val txBuilder = TransactionBuilder(notary = notary)
            .addOutputState(newEventState, EventContract.ID)
            .addCommand(EventContract.Commands.Create(), newEventState.participants.map { it.owningKey })

        // Stage 2.
        progressTracker.currentStep = VERIFYING_TRANSACTION
        // Verify that the transaction is valid.
        try {
            txBuilder.verify(serviceHub)
        } catch (e: Exception) {
            log.warn("Verification of transaction failed because: {}", e.message, e)
            throw e
        }

        // Stage 3.
        progressTracker.currentStep = SIGNING_TRANSACTION

        /*
          When an Event is sent, the same event is sent to all parties
          This needs to change, because if ISHARE is used, every party hands out his own accesstoken,
          which is stored in the event.
        */
        log.info("Event flow using iSHARE: {}", serviceHub.cordaService(ISHARECordaService::class.java).ishareEnabled())

        if (serviceHub.cordaService(ISHARECordaService::class.java).ishareEnabled()) {
            // create an eventstate for every party , since they all have different accesstokens
            log.info("Gathering Access Tokens for the Event counterparties")
            counterParties.forEach {
                //  create new session to get the tokens ?
                log.info("Retrieving iSHARE access token for: {}", it.name)
                val tokenResponse = subFlow(ISHARETokenFlow(it))
                newEventState.accessTokens[it] = tokenResponse.access_token
            }
        }

        // Sign the transaction.
        val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

        // Stage 4.
        progressTracker.currentStep = GATHERING_SIGS
        // Send the state to the counterparty, and receive it back with their signature.
        log.info("Sending new Event to counterparties")

        val otherPartySessions = counterParties.map {
            log.info("Initiating flow for party: {}", it.name)
            initiateFlow(it)
        }
        val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, otherPartySessions, GATHERING_SIGS.childProgressTracker()))

        // Stage 5.
        progressTracker.currentStep = FINALISING_TRANSACTION
        // Notarise and record the transaction in both parties' vaults.
        storeEventInLocalTripleStore(newEventState, me)
        return subFlow(FinalityFlow(fullySignedTx, otherPartySessions, FINALISING_TRANSACTION.childProgressTracker()))
    }

    @Suspendable
    private fun storeEventInLocalTripleStore(newEventState: EventState, me: Party) {
        log.info("Inserting event data into the triple store... {}", me.name)
        val await = await(GraphDBInsert(graphdb(), newEventState.event, false))
        require(await) {
            "Unable to insert event data into the triple store at ${me.name}."
        }.also {
            log.info("Inserted event data into the triple store: {} at: {}", await, me.name)
        }
    }

    private fun findMatchingParties(destinations: Collection<CordaX500Name>): Set<Party> = destinations.flatMap { destination ->
        val me = serviceHub.myInfo.legalIdentities.first()
        val parties = serviceHub.networkMapCache.allNodes
            .flatMap { it.legalIdentities }
            .minus(me)
            .filter {
                (destination.commonName == null || it.name.commonName.equals(destination.commonName, ignoreCase = true))
                    && (destination.organisationUnit == null || it.name.organisationUnit.equals(destination.organisationUnit, ignoreCase = true))
                    && (destination.state == null || it.name.state.equals(destination.state, ignoreCase = true))
                    && it.name.organisation.equals(destination.organisation, ignoreCase = true)
                    && it.name.locality.equals(destination.locality, ignoreCase = true)
                    && it.name.country.equals(destination.country, ignoreCase = true)
            }

        if (parties.isEmpty()) {
            log.warn("One of the requested counterparties was not found: $destination")
            throw IllegalArgumentException("One of the requested counterparties was not found: $destination")
        }

        log.info("Found: {} counterparties for destination: {}", parties.size, destination)
        parties

    }.toSet()
}

@InitiatedBy(NewEventFlow::class)
class NewEventResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {

    private val log = LoggerFactory.getLogger(NewEventResponder::class.java)

    companion object {
        object VERIFYING_STRING_INTEGRITY : Step("Verifying that accompanying full event is acceptable.")
        object SIGNING : Step("Responding to CollectSignaturesFlow.")
        object FINALISATION : Step("Finalising a transaction.")

        fun tracker() = ProgressTracker(
            VERIFYING_STRING_INTEGRITY,
            SIGNING,
            FINALISATION
        )
    }

    override val progressTracker: ProgressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = VERIFYING_STRING_INTEGRITY

        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            @Suspendable
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val outputState = stx.tx.outputStates.single() as EventState
                val ishareService = serviceHub.cordaService(ISHARECordaService::class.java)
                log.info("Responder flow using iSHARE: {}", ishareService.ishareEnabled())
                // Check accesstoken if required and available in the eventState
                if (ishareService.ishareEnabled()) {
                    require(outputState.accessTokens.contains(serviceHub.myInfo.legalIdentities.first())) {
                        "ISHARE accessToken not found while it is required"
                    }
                    outputState.accessTokens[serviceHub.myInfo.legalIdentities.first()]?.let {
                        require(ishareService.checkAccessToken(it).first) {
                            "ISHARE AccessToken is invalid."
                        }
                        // TODO optional check if insert of event is allowed by the iSHARE AR
                    }
                    log.info("iSHARE AccessToken provided and valid, accepting new event!")
                }

                val await = await(GraphDBInsert(graphdb(), outputState.event, false))

                require(await) {
                    "Unable to insert event data into the triple store at " + serviceHub.myInfo.legalIdentities.first().name
                }.also {
                    log.info("Inserted event data into the triple store: {} at: {}", await, serviceHub.myInfo.legalIdentities.first().name)
                }
                // TODO what to check in the counterparty flow?
                // especially: if I'm not passing all previous states in the tx (see "requires" in the flow)
                // then I want the counterparties to check by themselves that everything's legit
            }
        }
        progressTracker.currentStep = SIGNING
        val txId = subFlow(signTransactionFlow).id

        progressTracker.currentStep = FINALISATION
        return subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))
    }
}

