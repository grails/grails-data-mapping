package org.grails.datastore.gorm.neo4j

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.engine.EntityPersister
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Simple
import org.grails.datastore.mapping.query.Query
import org.neo4j.cypher.EntityNotFoundException
import org.neo4j.cypher.javacompat.ExecutionEngine
import org.neo4j.graphdb.ResourceIterator
import org.springframework.context.ApplicationEventPublisher

/**
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
@CompileStatic
@SuppressWarnings("unchecked")
@Slf4j
class Neo4jEntityPersister extends EntityPersister {

    Neo4jEntityPersister(MappingContext mappingContext, PersistentEntity entity, Session session, ApplicationEventPublisher publisher) {
        super(mappingContext, entity, session, publisher)
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity pe, Serializable[] keys) {
        throw new UnsupportedOperationException()
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity pe, Iterable<Serializable> keys) {
        throw new UnsupportedOperationException()
    }

    @Override
    protected List<Serializable> persistEntities(PersistentEntity pe, @SuppressWarnings("rawtypes") Iterable objs) {
        throw new UnsupportedOperationException()
    }

    @Override
    protected Object retrieveEntity(PersistentEntity pe, Serializable key) {

        ResourceIterator<Map<String,Object>> iterator = executionEngine.execute("start n=node({key}) return ${Neo4jUtils.cypherReturnColumnsForType(pe)}", [key: key] as Map<String,Object>).iterator()
        try {
            assert iterator.hasNext()
            Neo4jUtils.unmarshall(iterator.next() as Map<String,Object>, pe)
        } catch (EntityNotFoundException e) {
            return null
        } finally {
            iterator.close()
        }
    }

    @Override
    protected Serializable persistEntity(PersistentEntity pe, Object obj) {

        def simpleProperties = pe.persistentProperties
                .findAll { it instanceof Simple && obj[it.name] != null }
                .collectEntries { PersistentProperty it ->

                    [(it.name): Neo4jUtils.mapToAllowedNeo4jType(obj[it.name], mappingContext)]
                }

        assert obj["id"] == null
        ResourceIterator<Map<String,Object>> iterator = executionEngine.execute("create (n:$pe.discriminator {props}) return id(n) as id",
                Collections.singletonMap("props", simpleProperties) as Map<String, Object>).iterator()
        try {
            Long id = ((Map<String, Object>) (iterator.next())).get("id") as Long
            obj["id"] = id
            return id
        } finally {
            iterator.close()
        }
    }

    @Override
    protected void deleteEntity(PersistentEntity pe, Object obj) {
        executionEngine.execute("start n=node({id}) match n-[r?]-m delete r,n", [id: obj["id"]])
    }

    @Override
    protected void deleteEntities(PersistentEntity pe, @SuppressWarnings("rawtypes") Iterable objects) {
        for (obj in objects) {
            deleteEntity(pe, obj)
        }
    }

    @Override
    Query createQuery() {
        return new Neo4jQuery(session, persistentEntity )
    }

    @Override
    Serializable refresh(Object o) {
        throw new UnsupportedOperationException()
    }

    ExecutionEngine getExecutionEngine() {
        session.nativeInterface as ExecutionEngine
    }
}
