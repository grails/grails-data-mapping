package org.grails.datastore.gorm.neo4j

import grails.gorm.tests.GormDatastoreSpec
import grails.gorm.tests.Person
import grails.gorm.tests.Pet

import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.ReturnableEvaluator
import org.neo4j.graphdb.StopEvaluator
import org.neo4j.graphdb.TraversalPosition
import org.neo4j.graphdb.Traverser
import org.neo4j.graphdb.Node
import spock.lang.IgnoreRest
import grails.gorm.tests.PetType
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.tooling.GlobalGraphOperations
import org.neo4j.visualization.asciidoc.AsciidocHelper
import org.neo4j.graphdb.traversal.TraversalDescription
import org.neo4j.kernel.Traversal
import org.neo4j.graphdb.Path

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
                GrailsRelationshipTypes.INSTANCE, Direction.BOTH, GrailsRelationshipTypes.SUBREFERENCE, Direction.BOTH, GrailsRelationshipTypes.SUBSUBREFERENCE, Direction.BOTH)
        def size = traverserResult.size()

        then:

        size == Person.traverseStatic(StopEvaluator.END_OF_GRAPH, ReturnableEvaluator.ALL_BUT_START_NODE,
            GrailsRelationshipTypes.INSTANCE, Direction.BOTH, GrailsRelationshipTypes.SUBREFERENCE, Direction.BOTH, GrailsRelationshipTypes.SUBSUBREFERENCE, Direction.BOTH).size()

        size+1 == Person.traverseStatic(Traverser.Order.BREADTH_FIRST,
                { TraversalPosition p -> false },
                { TraversalPosition p -> true },
            GrailsRelationshipTypes.INSTANCE, Direction.BOTH, GrailsRelationshipTypes.SUBREFERENCE, Direction.BOTH, GrailsRelationshipTypes.SUBSUBREFERENCE, Direction.BOTH).size()

        size+1 == Person.traverseStatic(
                { TraversalPosition p -> false },
                { TraversalPosition p -> true },
            GrailsRelationshipTypes.INSTANCE, Direction.BOTH, GrailsRelationshipTypes.SUBREFERENCE, Direction.BOTH, GrailsRelationshipTypes.SUBSUBREFERENCE, Direction.BOTH).size()

        size+1 == Person.traverseStatic(
                { TraversalPosition p -> false },
                { TraversalPosition p -> true } ).size()

        //println AsciidocHelper.createGraphViz("title", session.nativeInterface, "abc");

        /*Node subReferenceNode = ((Neo4jDatastore)session.datastore).subReferenceNodes[Person.class.name]
        Traversal.description().depthFirst()
                .relationships(GrailsRelationshipTypes.SUBSUBREFERENCE, Direction.OUTGOING)
                .relationships(GrailsRelationshipTypes.INSTANCE, Direction.OUTGOING)
                .traverse(subReferenceNode).each { Path p ->
            println p
        }*/

        Person.count() == Person.traverseStatic(
                { return false },
                { TraversalPosition p -> return p.currentNode().getProperty("__type__", null) == Person.name },
                GrailsRelationshipTypes.SUBSUBREFERENCE, Direction.OUTGOING,
                GrailsRelationshipTypes.INSTANCE, Direction.OUTGOING,
        ).size()

        // +2: referenceNode + self (aka subreferenceNode)
        Person.count()+2 == Person.traverseStatic(
                        { TraversalPosition p -> false },
                        { TraversalPosition p -> true },
                GrailsRelationshipTypes.SUBREFERENCE, Direction.OUTGOING,
                GrailsRelationshipTypes.SUBSUBREFERENCE, Direction.OUTGOING,
                GrailsRelationshipTypes.INSTANCE, Direction.OUTGOING,
        ).size()
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

        1 == person.traverse(
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

    def "test handling of non-declared properties using dot notation"() {
        when:
        def person = new Person(lastName:'person1').save(flush:true)
        session.clear()
        person = Person.load(person.id)
        person.notDeclaredProperty = 'someValue'   // n.b. the 'dot' notation is not valid for undeclared properties
        person.emptyArray = []
        person.someIntArray = [1,2,3]
        person.someStringArray = ['a', 'b', 'c']
        person.someDoubleArray= [0.9, 1.0, 1.1]
        session.flush()
        session.clear()
        person = Person.get(person.id)

        then:
        person.notDeclaredProperty == 'someValue'
        person.lastName == 'person1'  // declared properties are also available via map semantics
        person.someIntArray == [1,2,3]
        person.someStringArray == ['a', 'b', 'c']
        person.emptyArray == []
        person.someDoubleArray == [0.9, 1.0, 1.1]
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

    def "test cypher queries"() {
        setup:
        new Person(lastName:'person1').save()
        new Person(lastName:'person2').save()
        session.flush()
        session.clear()

        when:
        def result = Person.cypherStatic("start n=node({this}) match n-[:SUBSUBREFERENCE]->subRef-[:INSTANCE]->m where m.lastName='person1' return m")

        then:
        result.iterator().size()==1

    }

    def "test instance based cypher query"() {
        setup:
        def person = new Person(firstName: "Bob", lastName: "Builder")
        def petType = new PetType(name: "snake")
        def pet = new Pet(name: "Fred", type: petType, owner: person)
        person.addToPets(pet)
        person.save(flush: true)
        session.clear()

        when:
        def result = person.cypher("start n=node({this}) match n-[:pets]->m return m")

        then:
        result.iterator().size() == 1
    }

}
