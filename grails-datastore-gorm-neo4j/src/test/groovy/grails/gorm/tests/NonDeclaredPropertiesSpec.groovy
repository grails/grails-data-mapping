package grails.gorm.tests

import org.grails.datastore.gorm.neo4j.engine.Neo4jQuery
import org.neo4j.helpers.collection.IteratorUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Issue

class NonDeclaredPropertiesSpec extends GormDatastoreSpec {

    private static Logger log = LoggerFactory.getLogger(NonDeclaredPropertiesSpec.class);

    @Override
    List getDomainClasses() {
        [Club]
    }

    def "non declared properties should not mark the object as dirty if the value is the same"() {
        when:'An object is initially saved'
        def club = new Club(name: 'Cosima')
        club.buddy = 'Lara'
        club.save(flush:true)
        session.clear()

        club = Club.get(club.id)
        then:"it is not diry"
        club.buddy == 'Lara'
        !club.hasChanged()

        when:"A dynamic property is modified with the same value"
        club.buddy = 'Lara'

        then:"The object has no changes"
        !club.hasChanged()
        !club.hasChanged('buddy')

        when:"A dynamic property is modified with a different value"
        club.buddy = 'Foo'

        then:"The object has changes"
        club.hasChanged()
        club.hasChanged('buddy')

    }


    def "should non declared properties work for transient instances"() {
        setup:
            def club = new Club(name: 'Cosima')

        when:
            club.buddy = 'Lara'

        then:
            club.dynamicAttributes() == [buddy: 'Lara']
            club.buddy == 'Lara'


        when: "setting null means deleting the property"
            club.buddy = null
            def x = club.buddy

        then:
            x == null
    }

    def "should non declared properties throw error if not set"() {
        setup:
            def club = new Club(name: 'Cosima')

        expect:
            club.buddy == null
    }

    def "should non declared properties get persisted"() {
        setup:
            def club = new Club(name: 'Cosima').save()
            club.buddy = 'Lara'

            def date = new Date()
            club.born = date
            club.save(flush:true)
            session.clear()

        when:
            club = Club.findByName('Cosima')

        then:
            club.buddy == 'Lara'

        and: "dates are converted to long"
            club.born == date.time

        and: "we have no additional properties"
        club.dynamicAttributes().size() == 2
    }

    def "test handling of non-declared properties"() {
        when:
        def club = new Club(name:'person1').save()
        club['notDeclaredProperty'] = 'someValue'   // n.b. the 'dot' notation is not valid for undeclared properties
        club['emptyArray'] = []
        club['someIntArray'] = [1,2,3]
        club['someStringArray'] = ['a', 'b', 'c']
//        person['someDoubleArray'] = [0.9, 1.0, 1.1]
        session.flush()
        session.clear()
        club = Club.get(club.id)

        then:
        club['notDeclaredProperty'] == 'someValue'
        club['name'] == 'person1'  // declared properties are also available via map semantics
        club['someIntArray'] == [1,2,3]
        club['someStringArray'] == ['a', 'b', 'c']
//        person['someDoubleArray'] == [0.9, 1.0, 1.1]
    }

    def "test handling of non-declared properties using dot notation"() {
        setup:
        def club = new Club(name:'club1').save(flush:true)
        session.clear()
        club = Club.load(club.id)

        when:
        club.notDeclaredProperty = 'someValue'   // n.b. the 'dot' notation is not valid for undeclared properties
        club.emptyArray = []
        club.someIntArray = [1,2,3]
        club.someStringArray = ['a', 'b', 'c']
//        person.someDoubleArray= [0.9, 1.0, 1.1]
        session.flush()
        session.clear()
        club = Club.get(club.id)

        then:
        club.notDeclaredProperty == 'someValue'
        club.name == 'club1'  // declared properties are also available via map semantics
        club.someIntArray == [1,2,3]
        club.someStringArray == ['a', 'b', 'c']
        club.emptyArray == []
//        person.someDoubleArray == [0.9, 1.0, 1.1]
    }

    def "test null values on dynamic properties"() {
        setup:
        def club = new Club(name: 'person1').save(flush: true)
        session.clear()
        club = Club.load(club.id)
        when:
        club.notDeclaredProperty = null
        session.flush()
        session.clear()
        club = Club.get(club.id)

        then:
        club.notDeclaredProperty == null

        when:
        club.notDeclaredProperty = 'abc'
        session.flush()
        session.clear()
        club = Club.get(club.id)

        then:
        club.notDeclaredProperty == 'abc'

        when:
        club.notDeclaredProperty = null
        session.flush()
        session.clear()
        club = Club.get(club.id)

        then:
        club.notDeclaredProperty == null
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
        Pet.findByName("Cosima").buddies*.name.sort() == ["Lara", "Samira"]
    }

    def "Test update dynamic single-ended relationships"() {
        setup:
        def cosima = new Pet(name: 'Cosima')
        def lara = new Pet(name: 'Lara')
        cosima.buddy = lara
        cosima.friends = lara  // NB plural version


        when:
        cosima.save()
        session.flush()
        session.clear()

        then:
        Pet.findByName("Cosima").buddy.name == "Lara"

        and: "using plural named properties returns an array"
        Pet.findByName("Cosima").friends*.name == ["Lara"]

        when:"The dynamic association is updated"
        cosima = Pet.findByName("Cosima")
        cosima.buddy = new Pet(name:"Fred")
        cosima.friends << new Pet(name: "Bob").save()
        cosima.save()
        session.flush()
        session.clear()

        then:
        Pet.findByName("Cosima").buddy.name == "Fred"

        and: "using plural named properties returns an array"
        Pet.findByName("Cosima").friends*.name == ["Lara", "Bob"]
    }

}







