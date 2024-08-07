package nl.tno.federated.api.event.distribution.rules

import org.eclipse.rdf4j.repository.Repository
import org.eclipse.rdf4j.repository.sail.SailRepository
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.sail.memory.MemoryStore
import java.io.StringReader

class SparqlEventDistributionRule(
    private val sparql: String,
    private val destinations: Set<String>
) : EventDistributionRule {

    override fun getDestinations() = destinations

    override fun appliesTo(ttl: String): Boolean {
        val db: Repository = SailRepository(MemoryStore())
        db.init()

        try {
            db.connection.use { conn ->
                StringReader(ttl).use {
                    conn.add(it, "", RDFFormat.TURTLE)
                }
                val query = conn.prepareBooleanQuery(sparql)
                return query.evaluate()
            }
        } finally {
            db.shutDown()
        }
    }

    override fun toString(): String {
        return "SparqlEventDistributionRule(destinations='${getDestinations().joinToString(",")}'"
    }
}