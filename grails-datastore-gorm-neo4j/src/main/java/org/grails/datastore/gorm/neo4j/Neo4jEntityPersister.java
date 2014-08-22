package org.grails.datastore.gorm.neo4j;

import groovy.lang.GroovyObject;
import org.grails.datastore.gorm.neo4j.engine.CypherEngine;
import org.grails.datastore.gorm.neo4j.engine.CypherResult;
import org.grails.datastore.gorm.neo4j.parsers.PlingStemmer;
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
        return persistEntities(pe, objs, new HashSet());
    }

    protected List<Serializable> persistEntities(PersistentEntity pe, @SuppressWarnings("rawtypes") Iterable objs, Collection persistingColl) {
        List<Serializable> result = new ArrayList<Serializable>();
        for (Object obj: objs) {
            result.add(persistEntity(pe, obj, persistingColl));
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

        Object instance = getSession().getCachedInstance(persistentEntity.getJavaClass(), id);

        if (instance == null) {
            instance = unmarshall(persistentEntity, id, data);
            getSession().cacheInstance(persistentEntity.getJavaClass(), id, instance);
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
            PersistentEntity persistentEntity = findDerivedPersistentEntityWithLabel(pe, l);
            if (persistentEntity!=null) {
                int inheritenceChain = calcInheritenceChain(persistentEntity);
                if (inheritenceChain > longestInheritenceChain) {
                    longestInheritenceChain = inheritenceChain;
                    result = persistentEntity;
                }
            }
        }
        return result;
    }

    private PersistentEntity findDerivedPersistentEntityWithLabel(PersistentEntity parent, String label) {
        for (PersistentEntity pe: getMappingContext().getPersistentEntities()) {
            if (isInParentsChain(parent, pe)) {
                if (((GraphPersistentEntity)pe).getLabels().contains(label)) {
                    return pe;
                }
            }
        }
        return null;
    }

    private boolean isInParentsChain(PersistentEntity parent, PersistentEntity it) {
        if (it==null) {
            return false;
        } else if (it.equals(parent)) {
            return true;
        } else return isInParentsChain(parent, it.getParentEntity());
    }

    private int calcInheritenceChain(PersistentEntity current) {
        if (current == null) {
            return 0;
        } else {
            return calcInheritenceChain(current.getParentEntity()) + 1;
        }
    }

    private Object unmarshall(PersistentEntity persistentEntity, Long id, Map<String, Object> data) {

        log.debug( "unmarshalling entity {}, props {}, {}", id, data);
        EntityAccess entityAccess = new EntityAccess(persistentEntity, persistentEntity.newInstance());
        entityAccess.setConversionService(persistentEntity.getMappingContext().getConversionService());
        entityAccess.setIdentifier(id);
        data.remove("__id__");

        Map<TypeDirectionPair, Map<String, Collection>> relationshipsMap = new HashMap<TypeDirectionPair, Map<String, Collection>>();
        CypherResult relationships = getSession().getNativeInterface().execute(String.format("MATCH (m%s {__id__:{1}})-[r]-(o) RETURN type(r) as relType, startNode(r)=m as out, {ids: collect(o.__id__), labels: collect(labels(o))} as values", ((GraphPersistentEntity)persistentEntity).getLabelsAsString()), Collections.singletonList(id));
        for (Map<String, Object> row : relationships) {
            String relType = (String) row.get("relType");
            Boolean outGoing = (Boolean) row.get("out");
            Map<String, Collection> values = (Map<String, Collection>) row.get("values");
            TypeDirectionPair key = new TypeDirectionPair(relType, outGoing);
            relationshipsMap.put(key, values);
        }

        for (PersistentProperty property: entityAccess.getPersistentEntity().getPersistentProperties()) {

            String propertyName = property.getName();
            if (property instanceof Simple) {  // implicitly sets version property as well
                entityAccess.setProperty(propertyName, data.remove(propertyName));
            } else if (property instanceof Association) {

                Association association = (Association) property;
                TypeDirectionPair typeDirectionPair = new TypeDirectionPair(
                        RelationshipUtils.relationshipTypeUsedFor(association),
                        !RelationshipUtils.useReversedMappingFor(association)
                );

                Map<String, Collection> idsAndLabels = relationshipsMap.remove(typeDirectionPair);
                Collection<Long> targetIds = idsAndLabels == null ? null : idsAndLabels.get("ids");

                if (association instanceof ToOne) {
                    ToOne toOne = (ToOne) association;
                    if (targetIds!=null) {
                        Long targetId = IteratorUtil.single(targetIds);
//                        if (targetId!=null) {
                            entityAccess.setProperty(propertyName,
                                    getMappingContext().getProxyFactory().createProxy(
                                            session,
                                            toOne.getAssociatedEntity().getJavaClass(),
                                            targetId
                                    )
                            );
//                        }
                    }
                } else if ((association instanceof OneToMany) || (association instanceof ManyToMany)) {
                    Collection values = (Collection) entityAccess.getProperty(propertyName);
                    values = createDirtyCheckableAwareCollection(entityAccess, association, values);
                    entityAccess.setProperty(propertyName, values);

                    if (targetIds!=null) {
                        for (Long targetId : targetIds) {
                            values.add(getMappingContext().getProxyFactory().createProxy(
                                            session,
                                            association.getAssociatedEntity().getJavaClass(),
                                            targetId
                                    )
                            );

                        }
                    }
                } else {
                    throw new IllegalArgumentException("association " + association.getName() + " is of type " + association.getClass().getSuperclass().getName());
                }

            } else {
                throw new IllegalArgumentException("property " + property.getName() + " is of type " + property.getClass().getSuperclass().getName());

            }
        }

        if (!relationshipsMap.isEmpty()) {
            for (Map.Entry<TypeDirectionPair, Map<String,Collection>> entry: relationshipsMap.entrySet()) {

                if (entry.getKey().isOutgoing()) {
                    Iterator<Long> idIter = entry.getValue().get("ids").iterator();
                    Iterator<Collection<String>> labelIter = entry.getValue().get("labels").iterator();

                    Collection values = new ArrayList();
                    while (idIter.hasNext() && labelIter.hasNext()) {
                        Long targetId = idIter.next();
                        Collection<String> labels = labelIter.next();
                        Object proxy = getMappingContext().getProxyFactory().createProxy(
                                session,
                                ((Neo4jMappingContext) getMappingContext()).findPersistentEntityForLabels(labels).getJavaClass(),
                                targetId
                        );
                        values.add(proxy);
                    }

                    // for single instances and singular property name do not use an array
                    Object value = (values.size()==1) && isSingular(entry.getKey().getType()) ? IteratorUtil.single(values): values;
                    data.put(entry.getKey().getType(), value);
                }
            }
        }

        if (!data.isEmpty()) {
            GroovyObject go = (GroovyObject)(entityAccess.getEntity());
            go.setProperty(Neo4jGormEnhancer.UNDECLARED_PROPERTIES, data);
        }


        firePostLoadEvent(entityAccess.getPersistentEntity(), entityAccess);
        return entityAccess.getEntity();
    }

    private Collection createCollection(Association association) {
        return association.isList() ? new ArrayList() : new HashSet();
    }

    private Collection createDirtyCheckableAwareCollection(EntityAccess entityAccess, Association association, Collection delegate) {
        if (delegate==null) {
            delegate = createCollection(association);
        }
        if (!(delegate instanceof DirtyCheckableAwareCollection)) {
            delegate = association.isList() ?
                    new DirtyCheckableAwareList(entityAccess, association, (List)delegate, getSession()) :
                    new DirtyCheckableAwareSet(entityAccess, association, (Set) delegate, getSession());
        }
        return delegate;
    }

    private boolean isSingular(String key) {
        return PlingStemmer.isSingular(key);
    }

    /*private Class<Object> findJavaClassForLabels(Object labels) {
        return null;
    }*/

    @Override
    protected Serializable persistEntity(PersistentEntity pe, Object obj) {
        if (obj == null) {
            log.error("obj is null");
            throw new IllegalStateException("obj is null");
//            return null;
        }
        return persistEntity(pe, obj, new HashSet());
    }

    protected Serializable persistEntity(PersistentEntity pe, Object obj, Collection persistingColl ) {

        if (persistingColl.contains(obj)) {
            return null;
        } else {
            persistingColl.add(obj);
        }

        boolean isDirty = obj instanceof DirtyCheckable ? ((DirtyCheckable)obj).hasChanged() : true;

        if (getSession().containsPersistingInstance(obj) && (!isDirty)) {
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
            persistAssociationsOfEntity(pe, entityAccess, true, persistingColl);
            firePostUpdateEvent(pe, entityAccess);

        } else {
            if (cancelInsert(pe, entityAccess)) {
                return null;
            }
            getSession().addPendingInsert(new NodePendingInsert(getSession().getDatastore().nextIdForType(pe), entityAccess, getCypherEngine(), getMappingContext()));
            persistAssociationsOfEntity(pe, entityAccess, false, persistingColl);
            firePostInsertEvent(pe, entityAccess);
        }

        return (Serializable) entityAccess.getIdentifier();
    }

    private void persistAssociationsOfEntity(PersistentEntity pe, EntityAccess entityAccess, boolean isUpdate, Collection persistingColl) {

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

                        Collection targets = (Collection) propertyValue;
                        persistEntities(association.getAssociatedEntity(), targets, persistingColl);

                        boolean reversed = RelationshipUtils.useReversedMappingFor(association);

                        if (!reversed) {
                            Collection dcc = createDirtyCheckableAwareCollection(entityAccess, association, targets);
                            entityAccess.setProperty(association.getName(), dcc);
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

                        persistEntity(to.getAssociatedEntity(), propertyValue, persistingColl);

                        boolean reversed = RelationshipUtils.useReversedMappingFor(to);
                        String relType = RelationshipUtils.relationshipTypeUsedFor(to);

                        if (!reversed) {
                            if (isUpdate) {
                                getSession().addPendingInsert(new RelationshipPendingDelete(entityAccess, relType, null , getCypherEngine()));
                            }
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
                String.format("MATCH (n%s) WHERE n.__id__={1} OPTIONAL MATCH (n)-[r]-() DELETE r,n",
                        ((GraphPersistentEntity)pe).getLabelsAsString()),
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

                // TODO: check if property is an empty array -> exclude
                if (association.isOwningSide() && association.doesCascade(CascadeType.REMOVE)) {

                    if (propertyNotEmpty(property)) {

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
        }

        for (Map.Entry<PersistentEntity, Collection<Object>> entry: cascades.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                deleteEntities(entry.getKey(), entry.getValue());
            }
        }

        getCypherEngine().execute(
                String.format("MATCH (n%s) WHERE n.__id__ in {1} OPTIONAL MATCH (n)-[r]-() DELETE r,n",
                        ((GraphPersistentEntity)pe).getLabelsAsString()), Collections.singletonList(ids));

        for (EntityAccess entityAccess: entityAccesses) {
            firePostDeleteEvent(pe, entityAccess);
        }
    }

    private boolean propertyNotEmpty(Object property) {
        if (property==null) {
            return false;
        }
        if ((property instanceof Collection) && ( ((Collection) property).isEmpty() )) {
            return false;
        }
        return true;
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
