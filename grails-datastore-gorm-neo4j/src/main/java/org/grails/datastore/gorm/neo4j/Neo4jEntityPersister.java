package org.grails.datastore.gorm.neo4j;

import groovy.lang.GroovyObject;
import org.grails.datastore.gorm.neo4j.engine.CypherEngine;
import org.grails.datastore.gorm.neo4j.engine.CypherResult;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.engine.EntityPersister;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.*;
import org.grails.datastore.mapping.query.Query;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import javax.persistence.CascadeType;
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

    public Object unmarshallOrFromCache(PersistentEntity defaultPersistentEntity,
                                        Long id, Collection<String> labels,
                                        Map<String, Object> data) {

        PersistentEntity persistentEntity = mostSpecificPersistentEntity(defaultPersistentEntity, labels);
        Object instance = getSession().getCachedEntry(persistentEntity, id);

        if (instance == null) {
            instance = unmarshall(persistentEntity, id, data);
            getSession().cacheEntry(persistentEntity, id, instance);
        }
        return instance;
    }

    private PersistentEntity mostSpecificPersistentEntity(PersistentEntity pe, Collection<String> labels) {
        if (labels.size() == 1) {
            return pe;
        }
        PersistentEntity result = null;
        int longestInheritenceChain = -1;

        for (String l: labels) {
            PersistentEntity persistentEntity = findPersistentEntityWithLabel(l);

            int inheritenceChain = calcInheritenceChain(persistentEntity);
            if (inheritenceChain > longestInheritenceChain) {
                longestInheritenceChain = inheritenceChain;
                result = persistentEntity;
            }
        }
        return result;
    }

    private PersistentEntity findPersistentEntityWithLabel(String label) {
        for (PersistentEntity pe: getMappingContext().getPersistentEntities()) {
            if (((GraphPersistentEntity)pe).getLabel().equals(label)) {
                return pe;
            }
        }
        throw new IllegalStateException("no persistententity with discriminator " + label);
    }

    int calcInheritenceChain(PersistentEntity pe) {
        if (pe == null) {
            return 0;
        } else {
            return calcInheritenceChain(pe.getParentEntity()) + 1;
        }
    }

    private Object unmarshall(PersistentEntity persistentEntity, Long id, Map<String, Object> data) {

        log.debug( "unmarshalling entity {}, props {}, {}", id, data);
        EntityAccess entityAccess = new EntityAccess(persistentEntity, persistentEntity.newInstance());
        entityAccess.setConversionService(persistentEntity.getMappingContext().getConversionService());
        entityAccess.setIdentifier(id);
        data.remove("__id__");

        for (PersistentProperty property: entityAccess.getPersistentEntity().getPersistentProperties()) {

            String propertyName = property.getName();
            if (property instanceof Simple) {  // implicitly sets version property as well
                entityAccess.setProperty(propertyName, data.remove(propertyName));
//            } else if (property instanceof OneToOne) {
//                log.error("property " + property.getName() + " is of type " + property.getClass().getSuperclass());
            } else if (property instanceof ToOne) {
                ToOne to = (ToOne) property;

                CypherResult cypherResult = getSession().getNativeInterface().execute(CypherBuilder.findRelationshipEndpointIdsFor(to), Collections.singletonList(id));

                Map<String,Object> row = IteratorUtil.singleOrNull(cypherResult);
                if (row != null) {
                    Long endpointId = (Long) row.get("id");
                    entityAccess.setProperty(propertyName,
                            getMappingContext().getProxyFactory().createProxy(
                                    session,
                                    to.getAssociatedEntity().getJavaClass(),
                                    endpointId
                            )
                    );
                }
            } else if ((property instanceof OneToMany) || (property instanceof ManyToMany)) {

                LazyEnititySet lazyEnititySet = new LazyEnititySet(
                        entityAccess,
                        (Association) property,
                        getMappingContext().getProxyFactory(),
                        getSession()
                );
                entityAccess.setProperty(propertyName, lazyEnititySet);

            } else {
                    throw new IllegalArgumentException("property $property.name is of type ${property.class.superclass}");
            }

        }

        if (!data.isEmpty()) {
            GroovyObject go = (GroovyObject)(entityAccess.getEntity());
            go.setProperty(Neo4jGormEnhancer.UNDECLARED_PROPERTIES, data);
        }

        firePostLoadEvent(entityAccess.getPersistentEntity(), entityAccess);
        return entityAccess.getEntity();
    }

    @Override
    protected Serializable persistEntity(PersistentEntity pe, Object obj) {
        if ((obj == null) || (getSession().containsPersistingInstance(obj))) {
            return null;
        }

        EntityAccess entityAccess = createEntityAccess(pe, obj);
        if (getMappingContext().getProxyFactory().isProxy(obj)) {
            return (Serializable) entityAccess.getIdentifier();
        }

        getSession().addPersistingInstance(obj);

        // cancel operation if vetoed
        boolean isUpdate = entityAccess.getIdentifier() != null;
        if (isUpdate) {
            if (cancelUpdate(pe, entityAccess)) {
                return null;
            }
            getSession().addPendingUpdate(new NodePendingUpdate(entityAccess, getCypherEngine(), getMappingContext()));
            persistAssociationsOfEntity(pe, entityAccess, true);
            firePostUpdateEvent(pe, entityAccess);

        } else {
            if (cancelInsert(pe, entityAccess)) {
                return null;
            }
            getSession().addPendingInsert(new NodePendingInsert(getSession().getDatastore().nextIdForType(pe), entityAccess, getCypherEngine(), getMappingContext()));
            persistAssociationsOfEntity(pe, entityAccess, false);
            firePostInsertEvent(pe, entityAccess);
        }

        return (Serializable) entityAccess.getIdentifier();
    }

    private void persistAssociationsOfEntity(PersistentEntity pe, EntityAccess entityAccess, boolean isUpdate) {

        Object obj = entityAccess.getEntity();
        DirtyCheckable dirtyCheckable = null;
        if (obj instanceof DirtyCheckable) {
            dirtyCheckable = (DirtyCheckable)obj;
        }

        for (PersistentProperty pp: pe.getAssociations()) {
            if ((!isUpdate) || ((dirtyCheckable!=null) && dirtyCheckable.hasChanged(pp.getName()))) {

                Object propertyValue = entityAccess.getProperty(pp.getName());

                if ((pp instanceof OneToMany) || (pp instanceof ManyToMany)) {
                    Association association = (Association) pp;

                    if (propertyValue!= null) {

                        if (association.isBidirectional()) {  // Populate other side of bidi
                            for (Object associatedObject: (Iterable)propertyValue) {
                                EntityAccess assocEntityAccess = createEntityAccess(association.getAssociatedEntity(), associatedObject);
                                assocEntityAccess.setProperty(association.getReferencedPropertyName(), obj);
                            }
                        }

                        Iterable targets = (Iterable) propertyValue;
                        persistEntities(association.getAssociatedEntity(), targets);

                        boolean reversed = RelationshipUtils.useReversedMappingFor(association);

                        if (!reversed) {
                            if (!(propertyValue instanceof LazyEnititySet)) {
                                LazyEnititySet les = new LazyEnititySet(entityAccess, association, getMappingContext().getProxyFactory(), getSession());
                                les.addAll(targets);
                                entityAccess.setProperty(association.getName(), les);
                            }
                        }
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
                                if (collection == null ) {
                                    collection = new ArrayList();
                                    assocEntityAccess.setProperty(to.getReferencedPropertyName(), collection);
                                }
                                if (!collection.contains(obj)) {
                                    collection.add(obj);
                                }
                            }
                        }

                        persistEntity(to.getAssociatedEntity(), propertyValue);

                        boolean reversed = RelationshipUtils.useReversedMappingFor(to);
                        String relType = RelationshipUtils.relationshipTypeUsedFor(to);

                        if (!reversed) {
                            getSession().addPendingInsert(new RelationshipPendingInsert(entityAccess, relType,
                                    new EntityAccess(to.getAssociatedEntity(), propertyValue),
                                    getCypherEngine()));
                        }


                    }
                } else {
                    throw new IllegalArgumentException("wtf don't know how to handle " + pp + "(" + pp.getClass() +")" );

                }
            }


        }
    }

    @Override
    protected void deleteEntity(PersistentEntity pe, Object obj) {
        EntityAccess entityAccess = createEntityAccess(pe, obj);
        if (cancelDelete(pe, entityAccess)) {
            return;
        }

        for (Association association: pe.getAssociations()) {
            if (association.isOwningSide() && association.doesCascade(CascadeType.REMOVE)) {
                log.debug("cascading delete for property " + association.getName());

                GraphPersistentEntity otherPersistentEntity = (GraphPersistentEntity) association.getAssociatedEntity();
                Object otherSideValue = entityAccess.getProperty(association.getName());
                if (association instanceof ToOne) {
                    deleteEntity(otherPersistentEntity, otherSideValue);
                } else {
                    deleteEntities(otherPersistentEntity, (Iterable) otherSideValue);
                }
            }
        }

        getCypherEngine().execute(
                String.format("MATCH (n:%s) WHERE n.__id__={1} OPTIONAL MATCH (n)-[r]-() DELETE r,n",
                        ((GraphPersistentEntity)pe).getLabel()),
                Collections.singletonList(entityAccess.getIdentifier()));

        firePostDeleteEvent(pe, entityAccess);
    }

    @Override
    protected void deleteEntities(PersistentEntity pe, @SuppressWarnings("rawtypes") Iterable objects) {
        List<EntityAccess> entityAccesses = new ArrayList<EntityAccess>();
        List<Object> ids = new ArrayList<Object>();
        Map<PersistentEntity, Collection<Object>> cascades = new HashMap<PersistentEntity, Collection<Object>>();

        for (Object obj : objects) {
            EntityAccess entityAccess = createEntityAccess(pe, obj);
            if (cancelDelete(pe, entityAccess)) {
                return;
            }
            entityAccesses.add(entityAccess);
            ids.add(entityAccess.getIdentifier());

            // populate cascades
            for (Association association: pe.getAssociations()) {
                Object property = entityAccess.getProperty(association.getName());
                if (association.isOwningSide() && association.doesCascade(CascadeType.REMOVE) && (property!=null)) {

                    PersistentEntity associatedEntity = association.getAssociatedEntity();

                    Collection<Object> cascadesForPersistentEntity = cascades.get(associatedEntity);
                    if (cascadesForPersistentEntity==null) {
                        cascadesForPersistentEntity = new ArrayList<Object>();
                        cascades.put(associatedEntity, cascadesForPersistentEntity);
                    }

                    if (association instanceof ToOne) {
                        cascadesForPersistentEntity.add(property);
                    } else {
                        cascadesForPersistentEntity.addAll((Collection<?>) property);
                    }

                }
            }

        }

        for (Map.Entry<PersistentEntity, Collection<Object>> entry: cascades.entrySet()) {
            deleteEntities(entry.getKey(), entry.getValue());

        }

        getCypherEngine().execute(
                String.format("MATCH (n:%s) WHERE n.__id__ in {1} OPTIONAL MATCH (n)-[r]-() DELETE r,n",
                        ((GraphPersistentEntity)pe).getLabel()), Collections.singletonList(ids));

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
