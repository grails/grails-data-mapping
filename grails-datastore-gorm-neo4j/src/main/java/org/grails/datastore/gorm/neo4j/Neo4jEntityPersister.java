package org.grails.datastore.gorm.neo4j;

import org.grails.datastore.gorm.neo4j.engine.CypherEngine;
import org.grails.datastore.gorm.neo4j.simplegraph.Relationship;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.engine.EntityPersister;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.ManyToOne;
import org.grails.datastore.mapping.model.types.OneToMany;
import org.grails.datastore.mapping.model.types.OneToOne;
import org.grails.datastore.mapping.model.types.Simple;
import org.grails.datastore.mapping.model.types.ToOne;
import org.grails.datastore.mapping.proxy.EntityProxy;
import org.grails.datastore.mapping.query.Query;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.io.Serializable;
import java.util.*;

import static org.grails.datastore.mapping.query.Query.*;

/**
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
public class Neo4jEntityPersister extends EntityPersister {

    private static Logger log = LoggerFactory.getLogger(Neo4jEntityPersister.class);


    public Neo4jEntityPersister(MappingContext mappingContext, PersistentEntity entity, Session session, ApplicationEventPublisher publisher) {
        super(mappingContext, entity, session, publisher);
    }

    @Override
    public Neo4jSession getSession() {
        return (Neo4jSession) super.session;
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity pe, Serializable[] keys) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity pe, Iterable<Serializable> keys) {

        List<Criterion> criterions = new ArrayList<Criterion>(1);
        criterions.add(new In("id", IteratorUtil.asCollection(keys)));
        Junction junction = new Conjunction(criterions);
        return new Neo4jQuery(session, pe, this).executeQuery(pe, junction);

/*
        cypherEngine.execute("match (n:${pe.discriminator}) where id(n) in {keys} return ${"id(n) as id, labels(n) as labels, n as data, collect({type: type(r), endNodeIds: id(endnode(r))}) as endNodeId"}", [keys: keys] as Map<String,Object>).collect { Map<String,Object> map ->
            retrieveEntityAccess(pe, map["n"] as Node).entity
        }
*/
    }

    @Override
    protected List<Serializable> persistEntities(PersistentEntity pe, @SuppressWarnings("rawtypes") Iterable objs) {
        List<Serializable> result = new ArrayList<Serializable>();
        for (Object obj: objs) {
            result.add(persistEntity(pe, obj));
        }
        return result;
    }

    @Override
    protected Object retrieveEntity(PersistentEntity pe, Serializable key) {
        List<Criterion> criteria = new ArrayList<Criterion>(1);
        criteria.add(new IdEquals(key));
        return IteratorUtil.singleOrNull(new Neo4jQuery(session, pe, this).executeQuery(pe, new Conjunction(criteria)).iterator());
    }

    public EntityAccess retrieveEntityAccess(PersistentEntity defaultPersistentEntity,
             Long id, Collection<String> labels,
             Map<String, Object> data, Collection<Relationship> relationships) {
        getSession().addPersistentRelationships(relationships);
        PersistentEntity p = mostSpecificPersistentEntity(defaultPersistentEntity, labels);
        EntityAccess entityAccess = new EntityAccess(p, p.newInstance());
        entityAccess.setConversionService(p.getMappingContext().getConversionService());
        unmarshall(entityAccess, id, labels, data, relationships);
        return entityAccess;
    }

    private PersistentEntity mostSpecificPersistentEntity(PersistentEntity pe, Collection<String> labels) {
        if (labels.size() == 1) {
            return pe;
        }
        PersistentEntity result = null;
        int longestInheritenceChain = -1;

        for (String l: labels) {
            PersistentEntity persistentEntity = findPersistentEntityWithDiscriminator(l);

            int inheritenceChain = calcInheritenceChain(persistentEntity);
            if (inheritenceChain > longestInheritenceChain) {
                longestInheritenceChain = inheritenceChain;
                result = persistentEntity;
            }
        }
        return result;
    }

    private PersistentEntity findPersistentEntityWithDiscriminator(String discriminator) {
        for (PersistentEntity pe: getMappingContext().getPersistentEntities()) {
            if (pe.getDiscriminator().equals(discriminator)) {
                return pe;
            }
        }
        throw new IllegalStateException("no persistententity with discriminator " + discriminator);
    }

    int calcInheritenceChain(PersistentEntity pe) {
        if (pe == null) {
            return 0;
        } else {
            return calcInheritenceChain(pe.getParentEntity()) + 1;
        }
    }

    private Object unmarshall(EntityAccess entityAccess, Long id, Collection<String> labels,
                   Map<String, Object> data, Collection<Relationship> relationships) {

        log.warn( "unmarshalling entity {}, props {}, {}", id, data, relationships);
        if (entityAccess.getPersistentEntity().hasProperty("version", Long.class)) {
            Object version = data.get("version");
            if (version==null) {
                version = 0;
            }
            entityAccess.setProperty("version", version);
        }
        entityAccess.setIdentifier(id);

        for (PersistentProperty property: entityAccess.getPersistentEntity().getPersistentProperties()) {

            if (property instanceof Simple) {
                entityAccess.setProperty(property.getName(), data.get(property.getName()));
//            } else if (property instanceof OneToOne) {
//                log.error("property " + property.getName() + " is of type " + property.getClass().getSuperclass());
            } else if (property instanceof ToOne) {
                ToOne to = (ToOne) property;

                String relType = RelationshipUtils.relationshipTypeUsedFor(to);
                boolean reversed = RelationshipUtils.useReversedMappingFor(to);

                Relationship r = getSession().findPersistentRelationshipByType(relType, id, reversed);
                if (r!=null) {
                    long otherId = r.getOtherId(id);
                    entityAccess.setProperty(property.getName(),
                            getMappingContext().getProxyFactory().createProxy(
                                    session,
                                    to.getAssociatedEntity().getJavaClass(),
                                    otherId
                            )
                    );
                }
            } else if (property instanceof OneToMany) {
                OneToMany otm = (OneToMany) property;

                String relType = RelationshipUtils.relationshipTypeUsedFor(otm);
                boolean reversed = RelationshipUtils.useReversedMappingFor(otm);
                Collection proxies = new HashSet();

                for (Relationship r : getSession().findPersistentRelationshipsByType(relType, id, reversed)) {
                    proxies.add(getMappingContext().getProxyFactory().createProxy(session, otm.getAssociatedEntity().getJavaClass(), r.getOtherId(id) ));
                }
                entityAccess.setProperty(property.getName(), proxies);
            } else {
                    throw new IllegalArgumentException("property $property.name is of type ${property.class.superclass}");
            }
        }
        firePostLoadEvent(entityAccess.getPersistentEntity(), entityAccess);
        return entityAccess.getEntity();
    }


    private boolean isProxy(Object object) {
        return object instanceof EntityProxy;
    }

    @Override
    protected Serializable persistEntity(PersistentEntity pe, Object obj) {
        if ((obj == null) || (getSession().containsPersistingInstance(obj))) {
            return null;
        }
        EntityAccess entityAccess = createEntityAccess(pe, obj);

        if (isProxy(obj)) {
            return (Serializable) entityAccess.getIdentifier();
        }

        getSession().addPersistingInstance(obj);

        // cancel operation if vetoed
        boolean isUpdate = entityAccess.getIdentifier() != null;
        if (isUpdate) {
            if (cancelUpdate(pe, entityAccess)) {
                return null;
            }
            // TODO: check for dirty object
            if (pe.hasProperty("version", Long.class)) {
                long version = (Long) entityAccess.getProperty("version");
                version++;
                entityAccess.setProperty("version", version);
            }
            getSession().addPendingUpdate(new NodePendingUpdate(entityAccess, getCypherEngine(), getMappingContext()));
            firePostUpdateEvent(pe, entityAccess);

        } else {
            if (cancelInsert(pe, entityAccess)) {
                return null;
            }
            getSession().addPendingInsert(new NodePendingInsert(getSession().createIdentifierForNotYetPersistedInstance(), entityAccess, getCypherEngine(), getMappingContext()));
            firePostInsertEvent(pe, entityAccess);
        }

        for (PersistentProperty pp: pe.getPersistentProperties()) {
            Object propertyValue = entityAccess.getProperty(pp.getName());

            if ( pp instanceof Simple) {
                // nothing
            } else if (pp instanceof OneToMany) {
                OneToMany otm = (OneToMany) pp;

                if (propertyValue!= null) {

                    if (otm.isBidirectional()) {  // Populate other side of bidi
                        for (Object associatedObject: (Iterable)propertyValue) {
                            EntityAccess assocEntityAccess = createEntityAccess(otm.getAssociatedEntity(), associatedObject);
                            assocEntityAccess.setProperty(otm.getReferencedPropertyName(), obj);
                        }
                    }

                    persistEntities(otm.getAssociatedEntity(), (Iterable) propertyValue);
                    getSession().addPendingInsert(new RelationshipPendingInsert(entityAccess, otm, getCypherEngine(), getMappingContext(), getSession()));
                }


            } else if (pp instanceof ToOne) {
                if (propertyValue != null) {
                    ToOne to = (ToOne) pp;

                    if (to.isBidirectional()) {  // Populate other side of bidi
                        EntityAccess assocEntityAccess = createEntityAccess(to.getAssociatedEntity(), propertyValue);
                        if (to instanceof OneToOne) {
                            assocEntityAccess.setProperty(to.getReferencedPropertyName(), obj);
                        } else {
                            Collection collection = (Collection) assocEntityAccess.getProperty(to.getReferencedPropertyName());
                            collection.add(obj);
                        }
                    }

                    persistEntity(to.getAssociatedEntity(), propertyValue);
                    getSession().addPendingInsert(new RelationshipPendingInsert(entityAccess, to, getCypherEngine(), getMappingContext(), getSession()));

                }

            } else {
                throw new IllegalArgumentException("wtf don't know how to handle $pp (${pp.getClass()}");

            }

        }
        return (Serializable) entityAccess.getIdentifier();
    }

    @Override
    protected void deleteEntity(PersistentEntity pe, Object obj) {
        EntityAccess entityAccess = createEntityAccess(pe, obj);
        if (cancelDelete(pe, entityAccess)) {
            return;
        }
        getCypherEngine().execute(
                String.format("MATCH (n:%s) WHERE id(n)={id} OPTIONAL MATCH (n)-[r]-() DELETE r,n", pe.getDiscriminator()),
                Collections.singletonMap("id", entityAccess.getIdentifier()));
        firePostDeleteEvent(pe, entityAccess);
    }

    @Override
    protected void deleteEntities(PersistentEntity pe, @SuppressWarnings("rawtypes") Iterable objects) {
        List<EntityAccess> entityAccesses = new ArrayList<EntityAccess>();
        for (Object obj : objects) {
            EntityAccess entityAccess = createEntityAccess(pe, obj);
            if (cancelDelete(pe, entityAccess)) {
                return;
            }
            entityAccesses.add(entityAccess);
        }
        getCypherEngine().execute(
                String.format("MATCH (n:%s) OPTIONAL MATCH (n)-[r]-() DELETE r,n", pe.getDiscriminator()));
        for (EntityAccess entityAccess: entityAccesses) {
            firePostDeleteEvent(pe, entityAccess);
        }
    }

    @Override
    public Query createQuery() {
        return new Neo4jQuery(session, getPersistentEntity(), this);
    }

    @Override
    public Serializable refresh(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected EntityAccess createEntityAccess(PersistentEntity pe, Object obj) {
        return new EntityAccess(pe, obj);
    }

    public CypherEngine getCypherEngine() {
        return (CypherEngine) session.getNativeInterface();
    }
}
