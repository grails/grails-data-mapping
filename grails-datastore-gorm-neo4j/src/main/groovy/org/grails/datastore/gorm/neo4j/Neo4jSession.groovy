package org.grails.datastore.gorm.neo4j

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.core.AbstractSession
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.engine.Persister
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.transactions.Transaction
import org.neo4j.cypher.javacompat.ExecutionEngine
import org.springframework.context.ApplicationEventPublisher

@CompileStatic
class Neo4jSession extends AbstractSession<ExecutionEngine> {

    ExecutionEngine executionEngine

    Neo4jSession(Datastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher, boolean stateless, ExecutionEngine executionEngine) {
        super(datastore, mappingContext, publisher, stateless)
        this.executionEngine = executionEngine
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        final PersistentEntity entity = mappingContext.getPersistentEntity(cls.name)
        entity ? new Neo4jEntityPersister(mappingContext, entity, this, publisher) : null
    }

    @Override
    protected Transaction beginTransactionInternal() {
        throw new UnsupportedOperationException()
    }

    @Override
    ExecutionEngine getNativeInterface() {
        executionEngine
    }
}
