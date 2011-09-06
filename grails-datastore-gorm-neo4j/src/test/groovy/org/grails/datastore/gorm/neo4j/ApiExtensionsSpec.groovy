package org.grails.datastore.gorm.neo4j

import grails.gorm.tests.GormDatastoreSpec
import grails.gorm.tests.Person
import grails.gorm.tests.Pet

import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.ReturnableEvaluator
import org.neo4j.graphdb.StopEvaluator
import org.neo4j.graphdb.TraversalPosition
import org.neo4j.graphdb.Traverser

/**
 * check the traverser extension
 */
class ApiExtensionsSpec extends GormDatastoreSpec {

    def "test static traversing"() {
        given:
        new Person(lastName: "person1").save()
        new Person(lastName: "person2").save()

        when:
        def traverserResult = Person.traverseStatic(Traverser.Order.BREADTH_FIRST, StopEvaluator.END_OF_GRAPH, ReturnableEvaluator.ALL_BUT_START_NODE,
                GrailsRelationshipTypes.INSTANCE, Direction.BOTH, GrailsRelationshipTypes.SUBREFERENCE, Direction.BOTH)
        def size = traverserResult.size()

        then:

        size == Person.traverseStatic(StopEvaluator.END_OF_GRAPH, ReturnableEvaluator.ALL_BUT_START_NODE,
            GrailsRelationshipTypes.INSTANCE, Direction.BOTH, GrailsRelationshipTypes.SUBREFERENCE, Direction.BOTH).size()

        size+1 == Person.traverseStatic(Traverser.Order.BREADTH_FIRST,
                { TraversalPosition p -> false },
                { TraversalPosition p -> true },
            GrailsRelationshipTypes.INSTANCE, Direction.BOTH, GrailsRelationshipTypes.SUBREFERENCE, Direction.BOTH).size()

        size+1 == Person.traverseStatic(
                { TraversalPosition p -> false },
                { TraversalPosition p -> true },
            GrailsRelationshipTypes.INSTANCE, Direction.BOTH, GrailsRelationshipTypes.SUBREFERENCE, Direction.BOTH).size()

        size+1 == Person.traverseStatic(
                { TraversalPosition p -> false },
                { TraversalPosition p -> true } ).size()

        Person.count() == Person.traverseStatic(
                { TraversalPosition p -> false },
                { TraversalPosition p -> p.currentNode().getProperty("__type__",null) == Person.name } ).size()

        Person.count()+2 == Person.traverseStatic( // +2: referenceNode + self (aka subreferenceNode)
                        { TraversalPosition p -> true },
                        { TraversalPosition p -> true } ).size()
    }

    def "test instance based traversing"() {
        given:
        def person = new Person(lastName: "person1")
        person.save()
        new Person(lastName: "person2").save()

        when:
        def traverserResult = person.traverse(Traverser.Order.BREADTH_FIRST, StopEvaluator.END_OF_GRAPH, ReturnableEvaluator.ALL_BUT_START_NODE,
                GrailsRelationshipTypes.INSTANCE, Direction.BOTH, GrailsRelationshipTypes.SUBREFERENCE, Direction.BOTH)
        def size = traverserResult.size()

        then:

        size == person.traverse(StopEvaluator.END_OF_GRAPH, ReturnableEvaluator.ALL_BUT_START_NODE,
            GrailsRelationshipTypes.INSTANCE, Direction.BOTH, GrailsRelationshipTypes.SUBREFERENCE, Direction.BOTH).size()

        size+1 == person.traverse(Traverser.Order.BREADTH_FIRST,
                { TraversalPosition p -> false },
                { TraversalPosition p -> true },
            GrailsRelationshipTypes.INSTANCE, Direction.BOTH, GrailsRelationshipTypes.SUBREFERENCE, Direction.BOTH).size()

        size+1 == person.traverse(
                { TraversalPosition p -> false },
                { TraversalPosition p -> true },
            GrailsRelationshipTypes.INSTANCE, Direction.BOTH, GrailsRelationshipTypes.SUBREFERENCE, Direction.BOTH).size()

        size+1 == person.traverse(
                { TraversalPosition p -> false },
                { TraversalPosition p -> true } ).size()

        Person.count() == person.traverse(
                { TraversalPosition p -> false },
                { TraversalPosition p -> p.currentNode().getProperty("__type__",null) == Person.name } ).size()

        2 == person.traverse(
                        { TraversalPosition p -> true },
                        { TraversalPosition p -> true } ).size()
    }

    def "test createInstanceForNode"() {
        given:
        def person = new Person(lastName: 'person1')
        person.save()
        def pet = new Pet(name: 'pet')
        person.save()

        when: "retrieve a instance only by id"
        def instance = Pet.createInstanceForNode(person.id)

        then:
        instance instanceof Person
        instance.lastName == 'person1'

        when: "look up non-existing id"
        instance = Pet.createInstanceForNode(999)

        then:
        instance == null
    }

    def "test handling of non-declared properties"() {
        when:
        def person = new Person(lastName:'person1').save()
        person['notDeclaredProperty'] = 'someValue'   // n.b. the 'dot' notation is not valid for undeclared properties
        person['emptyArray'] = []
        person['someIntArray'] = [1,2,3]
        person['someStringArray'] = ['a', 'b', 'c']
        person['someDoubleArray'] = [0.9, 1.0, 1.1]
        session.flush()
        session.clear()
        person = Person.get(person.id)

        then:
        person['notDeclaredProperty'] == 'someValue'
        person['lastName'] == 'person1'  // declared properties are also available via map semantics
        person['someIntArray'] == [1,2,3]
        person['someStringArray'] == ['a', 'b', 'c']
        person['someDoubleArray'] == [0.9, 1.0, 1.1]
    }

    def "test handling of non-declared properties on transient instance"() {
        when:
        def person = new Person(lastName:'person1')
        person['notDeclaredProperty'] = 'someValue'

        then:
        thrown(IllegalStateException)
    }

    def "test handling of non-declared properties that do not match valid types in neo4j"() {
        when:
        def person = new Person(lastName:'person1')
        person['notDeclaredProperty'] = new Date()

        then:
        thrown(IllegalStateException)
    }

}
