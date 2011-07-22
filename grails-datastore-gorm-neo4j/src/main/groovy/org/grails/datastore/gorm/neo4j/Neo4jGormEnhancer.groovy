/* Copyright (C) 2010 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.datastore.gorm.neo4j

import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.finders.FinderMethod
import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.ReturnableEvaluator
import org.neo4j.graphdb.StopEvaluator
import org.neo4j.graphdb.Traverser
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.core.SessionCallback
import org.springframework.transaction.PlatformTransactionManager

/**
 * @author Stefan Armbruster <stefan@armbruster-it.de>
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

    def traverse(instance, Traverser.Order order, StopEvaluator stopEvaluator, ReturnableEvaluator returnableEvaluator, Object... args ) {

        execute new SessionCallback() {
            def doInSession(Session session) {

                Node referenceNode = ((Neo4jDatastore)datastore).graphDatabaseService.getNodeById(instance.id)

                // run neo4j traverser
                Traverser traverser = args ? referenceNode.traverse(order, stopEvaluator, returnableEvaluator, args) :
                    referenceNode.traverse(order, stopEvaluator, returnableEvaluator,
                    GrailsRelationshipTypes.INSTANCE, Direction.BOTH, GrailsRelationshipTypes.SUBREFERENCE, Direction.BOTH)

                // iterate result, unmarshall nodes to domain class instances if possible
                traverser.collect { Node node ->
                    Class clazz = node.getProperty("__type__", null)
                    if (clazz) {
                        session.retrieve(clazz, node.id)
                    } else {
                        node
                    }
                }
            }
        }
    }

    Node getSubreferenceNode(instance) {
        ((Neo4jDatastore)datastore).subReferenceNodes[instance.getClass().name]
    }

    Node getNode(instance) {
        ((Neo4jDatastore)datastore).graphDatabaseService.getNodeById(instance.id)
    }

    def traverse(instance, StopEvaluator stopEvaluator, ReturnableEvaluator returnableEvaluator, Object... args) {
        traverse(instance, Traverser.Order.BREADTH_FIRST, stopEvaluator, returnableEvaluator, args)
    }

    def traverse(instance, Closure stopEvaluator, Closure returnableEvaluator, Object... args) {
        traverse(instance, Traverser.Order.BREADTH_FIRST, stopEvaluator, returnableEvaluator, args)
    }

    def traverse(instance, Traverser.Order order, Closure stopEvaluator, Closure returnableEvaluator, Object... args) {
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

                Node subReferenceNode = ((Neo4jDatastore)datastore).subReferenceNodes[persistentEntity.name]

                // run neo4j traverser
                Traverser traverser = args ? subReferenceNode.traverse(order, stopEvaluator, returnableEvaluator, args) :
                    subReferenceNode.traverse(order, stopEvaluator, returnableEvaluator,
                    GrailsRelationshipTypes.INSTANCE, Direction.BOTH, GrailsRelationshipTypes.SUBREFERENCE, Direction.BOTH)

                // iterate result, unmarshall nodes to domain class instances if possible
                traverser.collect { Node node ->
                    Class clazz = node.getProperty("__type__", null)
                    if (clazz) {
                        session.retrieve(clazz, node.id)
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

    def createInstanceForNode(nodeOrId) {
        execute new SessionCallback() {
            def doInSession(Session session) {
                session.createInstanceForNode(nodeOrId)
            }
        }
    }
}
