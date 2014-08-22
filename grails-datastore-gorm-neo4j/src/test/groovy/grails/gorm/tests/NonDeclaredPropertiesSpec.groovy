package grails.gorm.tests

import org.grails.datastore.gorm.neo4j.Neo4jGormEnhancer
import org.neo4j.helpers.collection.IteratorUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Issue

class NonDeclaredPropertiesSpec extends GormDatastoreSpec {

    private static Logger log = LoggerFactory.getLogger(NonDeclaredPropertiesSpec.class);

    @Override
    List getDomainClasses() {
        [Pet]
    }

    def "should non declared properties work for transient instances"() {
        setup:
            def pet = new Pet(name: 'Cosima')

        when:
            pet.buddy = 'Lara'

        then:
            pet.buddy == 'Lara'


        when: "setting null means deleting the property"
            pet.buddy = null
            def x = pet.buddy

        then:
            x == null
    }

    def "should non declared properties throw error if not set"() {
        setup:
            def pet = new Pet(name: 'Cosima')

        expect:
            pet.buddy == null
    }

    def "should non declared properties get persisted"() {
        setup:
            def pet = new Pet(name: 'Cosima').save()
            pet.buddy = 'Lara'

            def date = new Date()
            pet.born = date
            pet.save(flush:true)
            session.clear()

        when:
            pet = Pet.findByName('Cosima')

        then:
            pet.buddy == 'Lara'

        and: "dates are converted to long"
            pet.born == date.time

        and: "we have no additional properties"
            pet."${Neo4jGormEnhancer.UNDECLARED_PROPERTIES}".size() == 2
    }

    def "test handling of non-declared properties"() {
        when:
        def person = new Person(lastName:'person1').save()
        person['notDeclaredProperty'] = 'someValue'   // n.b. the 'dot' notation is not valid for undeclared properties
        person['emptyArray'] = []
        person['someIntArray'] = [1,2,3]
        person['someStringArray'] = ['a', 'b', 'c']
//        person['someDoubleArray'] = [0.9, 1.0, 1.1]
        session.flush()
        session.clear()
        person = Person.get(person.id)

        then:
        person['notDeclaredProperty'] == 'someValue'
        person['lastName'] == 'person1'  // declared properties are also available via map semantics
        person['someIntArray'] == [1,2,3]
        person['someStringArray'] == ['a', 'b', 'c']
//        person['someDoubleArray'] == [0.9, 1.0, 1.1]
    }

    def "test handling of non-declared properties using dot notation"() {
        setup:
        def person = new Person(lastName:'person1').save(flush:true)
        session.clear()
        person = Person.load(person.id)

        when:
        person.notDeclaredProperty = 'someValue'   // n.b. the 'dot' notation is not valid for undeclared properties
        person.emptyArray = []
        person.someIntArray = [1,2,3]
        person.someStringArray = ['a', 'b', 'c']
//        person.someDoubleArray= [0.9, 1.0, 1.1]
        session.flush()
        session.clear()
        person = Person.get(person.id)

        then:
        person.notDeclaredProperty == 'someValue'
        person.lastName == 'person1'  // declared properties are also available via map semantics
        person.someIntArray == [1,2,3]
        person.someStringArray == ['a', 'b', 'c']
        person.emptyArray == []
//        person.someDoubleArray == [0.9, 1.0, 1.1]
    }

    def "test null values on dynamic properties"() {
        setup:
        def person = new Person(lastName: 'person1').save(flush: true)
        session.clear()
        person = Person.load(person.id)
        when:
        person.notDeclaredProperty = null
        session.flush()
        session.clear()
        person = Person.get(person.id)

        then:
        person.notDeclaredProperty == null

        when:
        person.notDeclaredProperty = 'abc'
        session.flush()
        session.clear()
        person = Person.get(person.id)

        then:
        person.notDeclaredProperty == 'abc'

        when:
        person.notDeclaredProperty = null
        session.flush()
        session.clear()
        person = Person.get(person.id)

        then:
        person.notDeclaredProperty == null
    }

    @Issue("GPNEO4J-25")
    def "dynamic properties point to domain classes instance should be relationships"() {
        setup:
        def cosima = new Pet(name: 'Cosima')
        def lara = new Pet(name: 'Lara')
        cosima.buddy = lara
        cosima.buddies = lara  // NB plural version

        cosima.save()
        session.flush()
        session.clear()

        when:
        def result = session.nativeInterface.execute("MATCH (n:Pet {__id__:{1}})-[:buddy]->(l) return l", [cosima.id])

        then:
        IteratorUtil.count(result) == 1

        and: "reading dynamic rels works"
        Pet.findByName("Cosima").buddy.name == "Lara"

        and: "using plural named properties returns an array"
        Pet.findByName("Cosima").buddies*.name == ["Lara"]
    }

    def "dynamic properties pointing to arrays of domain classes should be a relationship"() {
        setup:
        def cosima = new Pet(name: 'Cosima')
        def lara = new Pet(name: 'Lara')
        def samira = new Pet(name: 'Samira')
        cosima.buddies = [lara, samira]

        cosima.save()
        session.flush()
        session.clear()

        when:
        def result = session.nativeInterface.execute("MATCH (n:Pet {__id__:{1}})-[:buddies]->(l) return l", [cosima.id])

        then:
        IteratorUtil.count(result) == 2

        and: "reading dynamic rels works"
        Pet.findByName("Cosima").buddies*.name == ["Lara", "Samira"]
    }

}







