package org.grails.datastore.gorm.neo4j

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.neo4j.engine.CypherEngine
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
import org.grails.datastore.mapping.model.types.ToOne
import org.grails.datastore.mapping.proxy.EntityProxy
import org.grails.datastore.mapping.query.Query
import org.neo4j.helpers.collection.IteratorUtil
import org.springframework.context.ApplicationEventPublisher

import static org.grails.datastore.mapping.query.Query.*

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
    Neo4jSession getSession() {
        (Neo4jSession) (super.session)
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity pe, Serializable[] keys) {
        throw new UnsupportedOperationException()
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity pe, Iterable<Serializable> keys) {
        List<Criterion> criterions = [new In("id", keys as Collection)] as List<Criterion>
        Junction junction = new Conjunction(criterions)
        new Neo4jQuery(session, pe, this).executeQuery(pe, junction)

/*
        cypherEngine.execute("match (n:${pe.discriminator}) where id(n) in {keys} return ${"id(n) as id, labels(n) as labels, n as data, collect({type: type(r), endNodeIds: id(endnode(r))}) as endNodeId"}", [keys: keys] as Map<String,Object>).collect { Map<String,Object> map ->
            retrieveEntityAccess(pe, map["n"] as Node).entity
        }
*/
    }

    @Override
    protected List<Serializable> persistEntities(PersistentEntity pe, @SuppressWarnings("rawtypes") Iterable objs) {
        objs.collect {
            persistEntity(pe, it)
        }
    }

    @Override
    protected Object retrieveEntity(PersistentEntity pe, Serializable key) {
        List<Criterion> criteria = [new IdEquals(key)] as List<Criterion>
        IteratorUtil.single(new Neo4jQuery(session, pe, this).executeQuery(pe, new Conjunction(criteria)).iterator())
    }

    public EntityAccess retrieveEntityAccess(PersistentEntity defaultPersistentEntity, Long id, Collection<String> labels,
                                             Map<String, Object> data, Map<String, Collection<Long>> relationships) {
        session.setPersistentRelationships(id, relationships)
        PersistentEntity p = mostSpecificPersistentEntity(defaultPersistentEntity, labels)
        EntityAccess entityAccess = new EntityAccess(p, p.newInstance())
        entityAccess.conversionService = p.mappingContext.conversionService
        unmarshall(entityAccess, id, labels, data, relationships)
        entityAccess
    }

    private PersistentEntity mostSpecificPersistentEntity(PersistentEntity pe, Collection<String> labels) {
        if (labels.size() == 1) {
            return pe
        }
        PersistentEntity result
        int longestInheritenceChain = -1

        for (String l in labels) {
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
        if (pe == null) {
            0
        } else {
            calcInheritenceChain(pe.parentEntity) + 1
        }
    }

    def unmarshall(EntityAccess entityAccess, Long id, Collection<String> labels,
                   Map<String, Object> data, Map<String, Collection<Long>> relationships) {

        log.warn "unmarshalling entity $id, props $data, $relationships"
        if (entityAccess.entity.hasProperty("version")) {
            entityAccess.setProperty("version", data.version ?: 0)
        }
        entityAccess.setProperty("id", id)
        for (PersistentProperty property in entityAccess.persistentEntity.persistentProperties) {
            switch (property) {
                case Simple:
                    entityAccess.setProperty(property.name, data[property.name])
                    //entity.mappingContext.conversionService.convert(map[property.name], property.type)
                    break
                case OneToOne:
                    log.error "property $property.name is of type ${property.class.superclass}"
                    break
                case ManyToOne:
                    ManyToOne mto = property as ManyToOne
                    Long otherId = IteratorUtil.single(relationships[property.name.toUpperCase()])
                    entityAccess.setProperty(property.name, mappingContext.proxyFactory.createProxy(session, mto.associatedEntity.javaClass, otherId))
                    break
                case OneToMany:
                    OneToMany otm = property as OneToMany
                    Collection proxies = [].asType(property.type) as Collection

                    log.warn "prop: $property.name"

                    relationships[property.name.toUpperCase()]?.each { Long it ->
                        proxies << mappingContext.proxyFactory.createProxy(session, otm.associatedEntity.javaClass, it)
                    }
                    entityAccess.setProperty(property.name, proxies)

                    break
                default:
                    throw new IllegalArgumentException("property $property.name is of type ${property.class.superclass}")
            }
        }
        firePostLoadEvent(entityAccess.persistentEntity, entityAccess)
        return entityAccess.entity
    }


    private boolean isProxy(object) {
        object instanceof EntityProxy
    }

    @Override
    protected Serializable persistEntity(PersistentEntity pe, Object obj) {
        if (obj == null) {
            return null
        }
        EntityAccess entityAccess = createEntityAccess(pe, obj)

        if (isProxy(obj)) {
            return entityAccess.getIdentifier() as Serializable
        }

        // cancel operation if vetoed
        boolean isUpdate = entityAccess.identifier != null
        if (isUpdate) {
            if (cancelUpdate(pe, entityAccess)) {
                return null
            }
            // TODO: check for dirty object
            if (pe.hasProperty("version", Long)) {
                obj["version"] = ((Number) obj["version"]) + 1
            }
            session.addPendingUpdate(new NodePendingUpdate(entityAccess, cypherEngine, mappingContext))

        } else {
            if (cancelInsert(pe, entityAccess)) {
                return null
            }
            session.addPendingInsert(new NodePendingInsert(0 as Long, entityAccess, cypherEngine, mappingContext))
        }

        for (PersistentProperty pp in pe.persistentProperties) {
            def propertyValue = entityAccess.getProperty(pp.name)
            switch (pp) {
                case Simple:
                    break
                case OneToMany:
                    def otm = pp as OneToMany

                    persistEntities(otm.associatedEntity, propertyValue as Iterable)
                    session.addPendingInsert(new RelationshipPendingInsert(entityAccess, otm, cypherEngine, mappingContext, session))

                    break
                case ToOne:
                    def to = pp as ToOne

                    if (propertyValue != null) {
                        persistEntity(to.associatedEntity, propertyValue)
                        session.addPendingInsert(new RelationshipPendingInsert(entityAccess, to, cypherEngine, mappingContext, session))

                    }
                    break
                default:
                    throw new IllegalArgumentException("wtf don't know how to handle $pp (${pp.getClass()}")
            }
        }

/*
        try {
            Map<String, Object> params = [
                    props: simpleProperties,
                    id: obj["id"]
            ] as Map<String, Object>
            Map<String, Object> firstRow = IteratorUtil.first(cypherEngine.execute(cypher,
                    params))

            if (isUpdate) {
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
*/
    }

    @Override
    protected void deleteEntity(PersistentEntity pe, Object obj) {
        EntityAccess entityAccess = createEntityAccess(pe, obj)
        if (cancelDelete(pe, entityAccess)) {
            return
        }
        cypherEngine.execute("match (n:${pe.discriminator}) match n-[r?]-m where id(n)={id} delete r,n", [id: obj["id"]])
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
        return new Neo4jQuery(session, persistentEntity, this)
    }

    @Override
    Serializable refresh(Object o) {
        throw new UnsupportedOperationException()
    }

    @Override
    protected EntityAccess createEntityAccess(PersistentEntity pe, Object obj) {
        return new EntityAccess(pe, obj);
    }

    CypherEngine getCypherEngine() {
        session.nativeInterface as CypherEngine
    }
}
