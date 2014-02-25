package org.grails.datastore.gorm.neo4j

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.neo4j.engine.CypherEngine
import org.grails.datastore.gorm.neo4j.engine.EmbeddedCypherResult
import org.grails.datastore.mapping.core.AbstractSession
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.core.impl.PendingInsert
import org.grails.datastore.mapping.core.impl.PendingOperation
import org.grails.datastore.mapping.core.impl.PendingOperationExecution
import org.grails.datastore.mapping.engine.Persister
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.transactions.Transaction
import org.neo4j.cypher.javacompat.ExecutionEngine
import org.springframework.context.ApplicationEventPublisher
import org.springframework.dao.InvalidDataAccessResourceUsageException

import javax.persistence.FlushModeType

@CompileStatic
@Slf4j
class Neo4jSession extends AbstractSession<ExecutionEngine> {

    CypherEngine cypherEngine

    /**
     * provides the number for variable identifier to be used in cypher create statements
     */
    private int cypherReferenceNumber = 0

    private Map<Long, Map<String, Collection<Long>>> persistentRelationships = new HashMap<>()

    Neo4jSession(Datastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher, boolean stateless, CypherEngine cypherEngine) {
        super(datastore, mappingContext, publisher, stateless)
        this.cypherEngine = cypherEngine
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        final PersistentEntity entity = mappingContext.getPersistentEntity(cls.name)
        entity ? new Neo4jEntityPersister(mappingContext, entity, this, publisher) : null
    }

    @Override
    protected Transaction beginTransactionInternal() {
        // TODO: think about transaction handling
        new Neo4jTransaction(null)
//        new Neo4jTransaction(((Neo4jDatastore)datastore).graphDatabaseService)
    }

    @Override
    CypherEngine getNativeInterface() {
        cypherEngine
    }

    @Override
    protected void flushPendingInserts(Map<PersistentEntity, Collection<PendingInsert>> inserts) {
        def flattened = inserts.values().flatten()
        flushPendingOperations(flattened.findAll { it instanceof NodePendingInsert})
        flushPendingOperations(flattened.findAll { it instanceof RelationshipPendingInsert})
    }

    /**
     * TODO: find clean solution instead of copying from base class
     * @param operations
     */
    private void flushPendingOperations(Collection operations) {
        for (Object o : operations) {
            PendingOperation pendingOperation = (PendingOperation) o;
            try {
                PendingOperationExecution.executePendingOperation(pendingOperation);
            } catch (RuntimeException e) {
                setFlushMode(FlushModeType.COMMIT);
                throw new InvalidDataAccessResourceUsageException("Do not flush() the Session after an exception occurs", e);
            }
        }
    }

    void setPersistentRelationships(Long id, Map<String, Collection<Long>> relationships) {
        persistentRelationships.put(id, relationships)
    }

    Map<String, Collection<Long>> getPersistentRelationships(Long id) {
        persistentRelationships.get(id)
    }

    @Override
    void flush() {
        super.flush()
        EmbeddedCypherResult result = cypherEngine.execute("optional match p = (n)-->(m) return p") as EmbeddedCypherResult
        log.warn "flush, db is: \n${result.executionResult.dumpToString()}"
    }
}
