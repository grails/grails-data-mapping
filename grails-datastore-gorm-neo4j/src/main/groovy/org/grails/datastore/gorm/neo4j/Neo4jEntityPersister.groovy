package org.grails.datastore.gorm.neo4j

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.neo4j.Neo4jUtils
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.engine.EntityPersister
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.ManyToOne
import org.grails.datastore.mapping.model.types.OneToMany
import org.grails.datastore.mapping.model.types.OneToOne
import org.grails.datastore.mapping.model.types.Simple
import org.grails.datastore.mapping.query.Query
import org.neo4j.cypher.EntityNotFoundException
import org.neo4j.cypher.javacompat.ExecutionEngine
import org.neo4j.helpers.collection.IteratorUtil
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

        def obj = pe.newInstance()
        EntityAccess entityAccess = createEntityAccess(pe, obj)
        entityAccess.conversionService = mappingContext.conversionService
        if (cancelLoad(pe, entityAccess)) {
            return null
        }

        try {
            Map<String,Object> map = IteratorUtil.firstOrNull(executionEngine.execute("match (n:${pe.discriminator}) where id(n)={key} return ${Neo4jUtils.cypherReturnColumnsForType(pe)}", [key: key] as Map<String,Object>))
//            Map<String,Object> map = IteratorUtil.firstOrNull(executionEngine.execute("start n=node({key}) return ${Neo4jUtils.cypherReturnColumnsForType(pe)}", [key: key] as Map<String,Object>))
            map ? unmarshall(map, entityAccess) : null
        } catch (EntityNotFoundException e ) {
            null
        }

/*        ResourceIterator<Map<String,Object>> iterator = executionEngine.execute("start n=node({key}) return ${Neo4jUtils.cypherReturnColumnsForType(pe)}", [key: key] as Map<String,Object>).iterator()
        try {
            if (iterator.hasNext()) {
                unmarshall(iterator.next() as Map<String,Object>, entityAccess)
                return entityAccess.entity
            } else {
                return null
            }
        } catch (EntityNotFoundException e) {
            return null
        } finally {
            iterator.close()
        }*/
    }

    def unmarshall(Map<String, Object> map, EntityAccess entityAccess) {
        entityAccess.setProperty("version", (map as Map<String, Object>)["version"])
        entityAccess.setProperty("id", (map as Map<String, Object>)["id"])
        for (PersistentProperty property in entityAccess.persistentEntity.persistentProperties) {
            switch (property) {
                case Simple:
                    entityAccess.setProperty(property.name, (map as Map<String, Object>)[property.name])
                    //entity.mappingContext.conversionService.convert(map[property.name], property.type)
                    break
                case OneToOne:
                    log.error "property $property.name is of type ${property.class.superclass}"
                    break
                case ManyToOne:
                    log.error "property $property.name is of type ${property.class.superclass}"
                    break
                case OneToMany:
                    log.error "property $property.name is of type ${property.class.superclass}"
                    break
                default:
                    throw new IllegalArgumentException("property $property.name is of type ${property.class.superclass}")
            }
        }
        firePostLoadEvent(entityAccess.persistentEntity, entityAccess)
        return entityAccess.entity
    }

    @Override
    protected Serializable persistEntity(PersistentEntity pe, Object obj) {

        EntityAccess entityAccess =  createEntityAccess(pe, obj)
        String cypher
        if (entityAccess.getProperty("id")) {
            if (cancelUpdate(pe, entityAccess)) {
                return null
            }
            cypher = "match (n:${pe.discriminator}) where id(n)={id} set n={props} return id(n) as id"
//            cypher = "start n=node({id}) set n={props} return id(n) as id"
        } else {
            if (cancelInsert(pe, entityAccess)) {
                return null
            }
            cypher = "create (n:$pe.discriminator {props}) return id(n) as id"
        }

        def simpleProperties = pe.persistentProperties
                .findAll { it instanceof Simple && obj[it.name] != null }
                .collectEntries { PersistentProperty it ->
                    [(it.name): Neo4jUtils.mapToAllowedNeo4jType(obj[it.name], mappingContext)] //TODO: use entityaccess
                }

        try {

            Map<String, Object> firstRow = IteratorUtil.first(executionEngine.execute(cypher,
                    ["props": simpleProperties, "id": obj["id"]] as Map<String, Object>))

            if (entityAccess.getProperty("id")) {
                firePostUpdateEvent(pe, entityAccess)
            } else {
                firePostInsertEvent(pe, entityAccess)
                entityAccess.setProperty("id", firstRow["id"])
            }
            return entityAccess.getProperty("id") as Long
        } catch (Exception e) {
            throw e
//            null
        }
    }

    @Override
    protected void deleteEntity(PersistentEntity pe, Object obj) {
        EntityAccess entityAccess = createEntityAccess(pe, obj)
        if (cancelDelete(pe, entityAccess)) {
            return
        }
        executionEngine.execute("match (n:${pe.discriminator}) match n-[r?]-m where id(n)={id} delete r,n", [id: obj["id"]])
        firePostDeleteEvent(pe, entityAccess)
    }

    @Override
    protected void deleteEntities(PersistentEntity pe, @SuppressWarnings("rawtypes") Iterable objects) {
        for (obj in objects) {
            deleteEntity(pe, obj)
        }
    }

    @Override
    Query createQuery() {
        return new Neo4jQuery(session, persistentEntity, this )
    }

    @Override
    Serializable refresh(Object o) {
        throw new UnsupportedOperationException()
    }

    ExecutionEngine getExecutionEngine() {
        session.nativeInterface as ExecutionEngine
    }
}
