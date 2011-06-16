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
import org.grails.datastore.gorm.finders.FinderMethod

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
        return new Neo4jGormStaticApi<D>(cls, datastore, finders)
    }

    protected <D> GormInstanceApi<D> getInstanceApi(Class<D> cls) {
        return new Neo4jGormInstanceApi<D>(cls, datastore)
    }

}

class Neo4jGormInstanceApi<D> extends GormInstanceApi<D> {

    Neo4jGormInstanceApi(Class<D> persistentClass, Datastore datastore) {
        super(persistentClass, datastore)
    }

    def traverse(def instance, Traverser.Order order, StopEvaluator stopEvaluator, ReturnableEvaluator returnableEvaluator, Object... args ) {

        execute new SessionCallback() {
            def doInSession(Session session) {

                def referenceNode = datastore.graphDatabaseService.getNodeById(instance.id)

                // run neo4j traverser
                def traverser = args ? referenceNode.traverse(order, stopEvaluator, returnableEvaluator, args) :
                    referenceNode.traverse(order, stopEvaluator, returnableEvaluator,
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

    def traverse(def instance, StopEvaluator stopEvaluator, ReturnableEvaluator returnableEvaluator, Object... args) {
        traverse(instance, Traverser.Order.BREADTH_FIRST, stopEvaluator, returnableEvaluator, args)
    }

    def traverse(def instance, Closure stopEvaluator, Closure returnableEvaluator, Object... args) {
        traverse(instance, Traverser.Order.BREADTH_FIRST, stopEvaluator, returnableEvaluator, args)
    }

    def traverse(def instance, Traverser.Order order, Closure stopEvaluator, Closure returnableEvaluator, Object... args) {
        traverse(instance, order, stopEvaluator as StopEvaluator, returnableEvaluator as ReturnableEvaluator, args)
    }


}

class Neo4jGormStaticApi<D> extends GormStaticApi<D> {

    Neo4jGormStaticApi(Class<D> persistentClass, Datastore datastore, List<FinderMethod> finders) {
        super(persistentClass, datastore, finders)
    }


    def traverseStatic(Traverser.Order order, StopEvaluator stopEvaluator, ReturnableEvaluator returnableEvaluator, Object... args ) {

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

    def traverseStatic(StopEvaluator stopEvaluator, ReturnableEvaluator returnableEvaluator, Object... args) {
        traverseStatic(Traverser.Order.BREADTH_FIRST, stopEvaluator, returnableEvaluator, args)
    }

    def traverseStatic(Closure stopEvaluator, Closure returnableEvaluator, Object... args) {
        traverseStatic(Traverser.Order.BREADTH_FIRST, stopEvaluator, returnableEvaluator, args)
    }

    def traverseStatic(Traverser.Order order, Closure stopEvaluator, Closure returnableEvaluator, Object... args) {
        traverseStatic(order, stopEvaluator as StopEvaluator, returnableEvaluator as ReturnableEvaluator, args)
    }

    def createInstanceForNode(def nodeOrId) {
        execute new SessionCallback() {
            def doInSession(Session session) {
                session.createInstanceForNode(nodeOrId)
            }
        }
    }

}

