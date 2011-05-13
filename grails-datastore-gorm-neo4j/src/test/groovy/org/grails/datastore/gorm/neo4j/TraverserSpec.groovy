package org.grails.datastore.gorm.neo4j

import grails.gorm.tests.GormDatastoreSpec
import org.neo4j.graphdb.Traverser
import org.neo4j.graphdb.StopEvaluator
import org.neo4j.graphdb.ReturnableEvaluator
import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.TraversalPosition
import grails.gorm.tests.Person

/**
 * check the traverser extension
 */
class TraverserSpec extends GormDatastoreSpec {


    def "test static traversing"() {
        given:
        new Person(lastName: "person1").save()
        new Person(lastName: "person2").save()

        when:
        def traverserResult = Person.traverse(Traverser.Order.BREADTH_FIRST, StopEvaluator.END_OF_GRAPH, ReturnableEvaluator.ALL_BUT_START_NODE,
                GrailsRelationshipTypes.INSTANCE, Direction.BOTH, GrailsRelationshipTypes.SUBREFERENCE, Direction.BOTH)
        def size = traverserResult.size()
        println traverserResult //.collect { "$it.id ${it.getProperty('__type__', 'unknown')}" }

        then:

        size == Person.traverse(StopEvaluator.END_OF_GRAPH, ReturnableEvaluator.ALL_BUT_START_NODE,
            GrailsRelationshipTypes.INSTANCE, Direction.BOTH, GrailsRelationshipTypes.SUBREFERENCE, Direction.BOTH).size()

        size+1 == Person.traverse(Traverser.Order.BREADTH_FIRST,
                { TraversalPosition p -> false },
                { TraversalPosition p -> true },
            GrailsRelationshipTypes.INSTANCE, Direction.BOTH, GrailsRelationshipTypes.SUBREFERENCE, Direction.BOTH).size()

        size+1 == Person.traverse(
                { TraversalPosition p -> false },
                { TraversalPosition p -> true },
            GrailsRelationshipTypes.INSTANCE, Direction.BOTH, GrailsRelationshipTypes.SUBREFERENCE, Direction.BOTH).size()

        size+1 == Person.traverse(
                { TraversalPosition p -> false },
                { TraversalPosition p -> true } ).size()

        Person.count() == Person.traverse(
                { TraversalPosition p -> false },
                { TraversalPosition p -> p.currentNode().getProperty("__type__",null)==Person.class.name } ).size()

    }
}
