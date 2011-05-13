package org.grails.datastore.gorm.neo4j

import org.grails.datastore.gorm.GormEnhancer
import org.springframework.datastore.mapping.core.Datastore
import org.springframework.transaction.PlatformTransactionManager
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.GormInstanceApi
import org.neo4j.graphdb.ReturnableEvaluator
import org.neo4j.graphdb.Traverser
import org.neo4j.graphdb.StopEvaluator
import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.Node
import org.springframework.datastore.mapping.core.Session
import org.springframework.datastore.mapping.core.SessionCallback

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 25.04.11
 * Time: 13:10
 * To change this template use File | Settings | File Templates.
 */
class Neo4jGormEnhancer extends GormEnhancer {

    Neo4jGormEnhancer(Datastore datastore) {
        super(datastore, null)
    }

    Neo4jGormEnhancer(Datastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager)
    }

    protected <D> GormStaticApi<D> getStaticApi(Class<D> cls) {
        return new Neo4jGormStaticApi<D>(cls, datastore)
    }

    protected <D> GormInstanceApi<D> getInstanceApi(Class<D> cls) {
        return new Neo4jGormInstanceApi<D>(cls, datastore)
    }

}

class Neo4jGormInstanceApi<D> extends GormInstanceApi<D> {

    Neo4jGormInstanceApi(Class<D> persistentClass, Datastore datastore) {
        super(persistentClass, datastore)
    }

//    /**
//     * Allows subscript access to schemaless attributes.
//     *
//     * @param instance The instance
//     * @param name The name of the field
//     */
//    void putAt(D instance, String name, value) {
//        if (instance.hasProperty(name)) {
//            instance.setProperty(name, value)
//        }
//        else {
//            getDbo(instance)?.put name, value
//        }
//    }
//
//    /**
//     * Allows subscript access to schemaless attributes.
//     *
//     * @param instance The instance
//     * @param name The name of the field
//     * @return the value
//     */
//    def getAt(D instance, String name) {
//        if (instance.hasProperty(name)) {
//            return instance.getProperty(name)
//        }
//
//        def dbo = getDbo(instance)
//        if (dbo != null && dbo.containsField(name)) {
//            return dbo.get(name)
//        }
//        return null
//    }
//
//    /**
//     * Return the DBObject instance for the entity
//     *
//     * @param instance The instance
//     * @return The DBObject instance
//     */
//    DBObject getDbo(D instance) {
//        execute new SessionCallback<DBObject>() {
//            DBObject doInSession(Session session) {
//
//                if (!session.contains(instance) && !instance.save()) {
//                    throw new IllegalStateException(
//                        "Cannot obtain DBObject for transient instance, save a valid instance first")
//                }
//
//                Neo4jEntityPersister persister = session.getPersister(instance)
//                def id = persister.getObjectIdentifier(instance)
//                return session.getCachedEntry(persister.getPersistentEntity(), id)
//            }
//        }
//    }
}

class Neo4jGormStaticApi<D> extends GormStaticApi<D> {

    Neo4jGormStaticApi(Class<D> persistentClass, Datastore datastore) {
        super(persistentClass, datastore)
    }


    def traverse(Traverser.Order order, StopEvaluator stopEvaluator, ReturnableEvaluator returnableEvaluator, Object... args ) {

        execute new SessionCallback() {
            def doInSession(Session session) {

                def subReferenceNode = datastore.subReferenceNodes[persistentEntity.name]

                // run neo4j traverser
                def traverser = args ? subReferenceNode.traverse(order, stopEvaluator, returnableEvaluator, args) :
                    subReferenceNode.traverse(order, stopEvaluator, returnableEvaluator,
                    GrailsRelationshipTypes.INSTANCE, Direction.BOTH, GrailsRelationshipTypes.SUBREFERENCE, Direction.BOTH)

                // iterate result, unmarshall nodes to domain class instances if possible
                traverser.collect { Node node ->
                    def clazz = node.getProperty("__type__", null)
                    if (clazz) {
                        def entityPersister = session.getPersister(clazz as Class)
                        assert entityPersister
                        entityPersister.createObjectFromNativeEntry(entityPersister.persistentEntity, node.id, node)
                    } else {
                        node
                    }
                }
            }
        }
    }

    def traverse(StopEvaluator stopEvaluator, ReturnableEvaluator returnableEvaluator, Object... args) {
        traverse(Traverser.Order.BREADTH_FIRST, stopEvaluator, returnableEvaluator, args)
    }

    def traverse (Closure stopEvaluator, Closure returnableEvaluator, Object... args) {
        traverse(Traverser.Order.BREADTH_FIRST, stopEvaluator, returnableEvaluator, args)
    }

    def traverse(Traverser.Order order, Closure stopEvaluator, Closure returnableEvaluator, Object... args) {
        traverse(order, stopEvaluator as StopEvaluator, returnableEvaluator as ReturnableEvaluator, args)
    }

//    @Override
//    Neo4jCriteriaBuilder createCriteria() {
//        return new Neo4jCriteriaBuilder(persistentClass, datastore.currentSession)
//    }
//
//    /**
//     * @return The name of the Neo4j collection that entity maps to
//     */
//    String getCollectionName() {
//        Neo4jDatastore ms = datastore
//        ms.getNeo4jTemplate(persistentEntity).getDefaultCollectionName()
//    }
//
//    /**
//     * The actual collection that this entity maps to.
//     *
//     * @return The actual collection
//     */
//    DBCollection getCollection() {
//        Neo4jDatastore ms = datastore
//        def template = ms.getNeo4jTemplate(persistentEntity)
//
//        def coll = template.getCollection(template.getDefaultCollectionName())
//        DBCollectionPatcher.patch(coll)
//        return coll
//    }
}

