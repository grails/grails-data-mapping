package org.grails.datastore.gorm.neo4j

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
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
import org.neo4j.graphdb.Label
import org.neo4j.helpers.collection.IteratorUtil
import org.neo4j.graphdb.Node
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
        executionEngine.execute("match (n:${pe.discriminator}) where id(n) in {keys} return ${Neo4jUtils.cypherReturnColumnsForType(pe)}", [keys: keys] as Map<String,Object>).collect { Map<String,Object> map ->
            retrieveEntityAccess(pe, map["n"] as Node).entity
        }
    }

    @Override
    protected List<Serializable> persistEntities(PersistentEntity pe, @SuppressWarnings("rawtypes") Iterable objs) {
        objs.collect {
             persistEntity(pe, it)
        }
    }

    @Override
    protected Object retrieveEntity(PersistentEntity pe, Serializable key) {

        try {
            Map<String,Object> map = IteratorUtil.first(executionEngine.execute("match (n:${pe.discriminator}) where id(n)={key} return ${Neo4jUtils.cypherReturnColumnsForType(pe)}", [key: key] as Map<String,Object>))

            EntityAccess entityAccess = retrieveEntityAccess(pe, map["n"] as org.neo4j.graphdb.Node)
            if (cancelLoad(pe, entityAccess)) {
                return null
            }
            entityAccess.entity

        } catch (EntityNotFoundException  | NoSuchElementException e) {
            null
        }
    }

    public EntityAccess retrieveEntityAccess(PersistentEntity defaultPersistentEntity, Node node) {
        PersistentEntity p = mostSpecificPersistentEntity(defaultPersistentEntity,
                node.labels.collect { Label l -> l.name() })
        EntityAccess entityAccess = new EntityAccess(p, p.newInstance())
        entityAccess.conversionService = p.mappingContext.conversionService
        unmarshall(node, entityAccess)
        entityAccess
    }

    private PersistentEntity mostSpecificPersistentEntity(PersistentEntity pe, Collection<String> labels) {
        if (labels.size()==1) {
            return pe
        }
        PersistentEntity result
        int longestInheritenceChain = -1

        for (String l in labels)  {
            PersistentEntity persistentEntity = mappingContext.persistentEntities.find { PersistentEntity p ->
                p.discriminator == l
            }

            int inheritenceChain = calcInheritenceChain(persistentEntity)
            if (inheritenceChain > longestInheritenceChain) {
                longestInheritenceChain = inheritenceChain
                result = persistentEntity
            }

        }
        return result
    }

    int calcInheritenceChain(PersistentEntity pe) {
        if (pe==null) {
            0
        } else {
            calcInheritenceChain(pe.parentEntity) + 1
        }
    }

    def unmarshall(Node node, EntityAccess entityAccess) {
        if (entityAccess.entity.hasProperty("version")) {
            entityAccess.setProperty("version", node.getProperty("version", 0))
        }
        entityAccess.setProperty("id", node.getId())
        for (PersistentProperty property in entityAccess.persistentEntity.persistentProperties) {
            switch (property) {
                case Simple:
                    entityAccess.setProperty(property.name, node.getProperty(property.name, null))
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

        EntityAccess entityAccess = createEntityAccess(pe, obj)
        String cypher
        if (entityAccess.getProperty("id")) {
            if (cancelUpdate(pe, entityAccess)) {
                return null
            }
            if (pe.hasProperty("version", Long)) {
                obj["version"] = ((Number)obj["version"]) + 1
            }
            cypher = "match (n:${pe.discriminator}) where id(n)={id} set n={props} return id(n) as id"
//            cypher = "start n=node({id}) set n={props} return id(n) as id"
        } else {
            if (cancelInsert(pe, entityAccess)) {
                return null
            }
            def labels = buildAllLabelsWithInheritence(pe)
            cypher = "create (n$labels {props}) return id(n) as id"
        }

        def simpleProperties = pe.persistentProperties
                .findAll { it instanceof Simple && obj[it.name] != null }
                .collectEntries { PersistentProperty it ->
                    [(it.name): Neo4jUtils.mapToAllowedNeo4jType(obj[it.name], mappingContext)] //TODO: use entityaccess
                }
        try {
            Map<String, Object> params = ["props": simpleProperties, "id": obj["id"]] as Map<String, Object>
            log.info "running cypher $cypher"
            log.info "   with params $params"
            Map<String, Object> firstRow = IteratorUtil.first(executionEngine.execute(cypher,
                    params))

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

    def buildAllLabelsWithInheritence(PersistentEntity persistentEntity) {
        if (persistentEntity==null) {
            return ""
        } else {
            ":${persistentEntity.discriminator}${buildAllLabelsWithInheritence(persistentEntity.parentEntity)}"
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
