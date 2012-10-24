package grails.gorm.tests

import grails.persistence.Entity
import groovyx.gpars.GParsPool
import org.grails.datastore.gorm.neo4j.GrailsRelationshipTypes
import org.neo4j.graphdb.Direction
import org.neo4j.graphdb.Node
import org.neo4j.helpers.collection.IteratorUtil
import spock.lang.Ignore
import spock.lang.Issue

/**
 * some more unrelated testcases, in more belong together logically, consider refactoring them into a seperate spec
 */
class MiscSpec extends GormDatastoreSpec {

    @Override
    List getDomainClasses() {
        [ Club, Team, Tournament, User, Role ]
    }

    def "test object identity, see if cache is being used"() {
        setup:
            new User(username: 'user1').save()
            new User(username: 'user2').save(flush:true)
            session.clear()

        when:  "retrieve the same object twice"
            def user = User.findByUsername('user1')
            def user2 = User.findByUsername('user1')

        then: "see if the same instance is returned"
            user.is(user2)
            2 == User.count()
            user in User.list()
    }

    def "test object identity in relationships"() {
        setup:
            def user = new User(username: 'user1')
            user.addToRoles new Role(role: 'role1')
            user.addToRoles new Role(role: 'role2')
            user.save(flush:true)
            session.clear()

        when:
            user = User.findByUsername('user1')
            def role = Role.findByRole('role1')

        then:
            role in user.roles
    }

    def "test unique constraint"() {
        setup:
            def role1 = new Role(role: 'role')
            role1.save(flush:true)
            def result = new Role(role:'role').save(flush:true)
            session.clear()

        expect:
            Role.findAllByRole('role').size() == 1

    }

    void "Test modification of a persistent instance with explicit save"() {
        given:
            def t = new TestEntity(name:"Bob")
            t.save(flush:true)
            session.clear()
        when:
            t = TestEntity.get(t.id)
            t.name = "Sam"
            t.save()  // explicit save necessary
            session.flush()
            session.clear()
        then:
            TestEntity.findByName("Bob") == null
            TestEntity.findByName("Sam") != null
    }

    void "test if addtoXXXX gets persisted correctly"() {
        given:
        new PlantCategory(name: 'category').save(flush:true)
        session.clear()

        when:
        def category = PlantCategory.findByName('category')
        session.clear()

        category = PlantCategory.get(category.id)
        def plant1 = new Plant(name:'plant1')
        category.addToPlants(plant1).save()
        category.save(flush:true)
        session.clear()
        category = PlantCategory.get(category.id)

        then:
        category
        category.name =='category'
        category.plants.size() == 1
        category.plants*.name == ['plant1']

    }

    // this test belongs semantically to grails.gorm.tests.CircularOneToManySpec but will fail in some existing
    // implementations, that's why it's located here
    void "test circular one-to-many using addToXX"() {
        setup:
            def user1 = new User(username: 'user1')
            def user2 = new User(username: 'user2')
            user1.addToFriends( user2)
            user2.addToFriends( user1)
            user1.save()
            user2.save()
            session.flush()
            session.clear()

        when:
            user1 = User.get(user1.id)
            user2 = User.get(user2.id)

        then:
            new ArrayList(user1.friends) == [ user2 ]
            new ArrayList(user2.friends) == [ user1 ]
    }

    void "test multiple relations with the same name"() {
        setup:
        def team = new Team(name: 'team')
        def club = new Club(name: 'club')
        club.addToTeams(team).save()
        def tournament = new Tournament(name:'tournament')
        tournament.addToTeams(team).save(flush:true)
        session.clear()

        when:
        tournament = Tournament.get(tournament.id)

        then:
        tournament.teams.size() == 1
        tournament.teams*.name == ['team']
        tournament.teams[0].club.name == 'club'
    }

    // TODO: more tests for indexing are required, add a IndexSearchSpec.groovy
    void "test indexing"() {
        setup:
        def task1 = new Task(name: 'task1')
        task1.save()
        new Task(name: 'task2').save(flush: true)
        session.clear()
        def index = session.datastore.indexManager.nodeAutoIndexer.autoIndex

        expect: "run native neo4j index query"
        index.get('name', 'task1').single == task1.node

        and: "a dynamic finder works"
        Task.findAllByName('task1')*.id == [task1.id]

    }

    void "verify correct behaviour of version incrementing"() {
        setup:
        def club = new Club(name: 'club')
        club.save(flush: true)
        session.clear()

        expect:
        club.version == 0

        when:
        club = Club.get(club.id)

        then:
        club.version == 0

        when:
        session.flush()

        then:
        club.version == 0
    }

    def "verify concurrent adding does not cause LockingExceptions"() {
        when:
        GParsPool.withPool(numberOfThreads) {
            (1..numberOfTeams).eachParallel { counter ->
                Team.withNewTransaction {
                    new Team(name: "Team $counter").save(failOnError: true)
                }
            }
        }
        Node subReferenceNode = session.datastore.subReferenceNodes[Team.class.name]

        then: "correct number of teams has been created"
        Team.count() == numberOfTeams

        and: "the number of subsubreferenceNodes is correct"
        subReferenceNode.getRelationships(GrailsRelationshipTypes.SUBSUBREFERENCE, Direction.OUTGOING).iterator().size() == numberOfThreads

        where:
        numberOfThreads | numberOfTeams | numberOfSubSubReferenceNodes
        1               | 20            | 1
        2               | 20            | 2
        4               | 20            | 4
        8               | 20            | 8

    }

    @Ignore
    def "do peformance tests"() {
        when:
        def start = System.currentTimeMillis()
        Team.withNewTransaction {
            for (i in 1..10000) {
                new Team(name: "Team $i").save()
            }
        }
        def delta = System.currentTimeMillis() - start
        println "create 10000 in $delta msec"

        then:
        delta > 0

        when:
        start = System.currentTimeMillis()
        def count = Team.count()
        delta = System.currentTimeMillis() - start
        println "count is $count, delta $delta"

        then:
        delta > 0

    }

    @Ignore
    def "manual perf test"() {
        when:

        Node subRef
        Team.withNewTransaction {
            subRef = session.nativeInterface.createNode()
        }
        def start = System.currentTimeMillis()
        Team.withNewTransaction {
            for (i in 1..10000) {
                Node node = session.nativeInterface.createNode()
                node.setProperty("name", "Team $i".toString())
                subRef.createRelationshipTo(node, GrailsRelationshipTypes.INSTANCE)
            }
        }
        def delta = System.currentTimeMillis() - start
        println "create 10000 in $delta msec"

        then:
        delta > 0

        when:
        start = System.currentTimeMillis()
        def count = IteratorUtil.count((Iterator)subRef.getRelationships(Direction.OUTGOING, GrailsRelationshipTypes.INSTANCE))
        delta = System.currentTimeMillis() - start
        println "count is $count, delta $delta"

        then:
        delta > 0

    }

    @Issue("https://github.com/SpringSource/grails-data-mapping/issues/52")
    def "check that date properties are stored natively as longs"() {
        when:
            def pet = new Pet(birthDate: new Date()).save(flush: true)
        then:
            pet.node.getProperty("birthDate") instanceof Long
    }

    @Issue("https://github.com/SpringSource/grails-data-mapping/issues/52")
    def "verify backward compatibility, check that date properties stored as string can be read"() {
        when: "create a instance with a date property and manually assign a string to it"
            def date = new Date()
            def pet = new Pet(birthDate: date).save(flush: true)
        then:
        pet.node.getProperty("birthDate") instanceof Long

        when:
            pet.node.setProperty("birthDate", date.time.toString())
            pet = Pet.get(pet.id)
        then: "the string stored date gets parsed correctly"
            pet.birthDate == date
    }

    def "byte arrays work as domain class properties"() {
        when:
        def team = new Team(name: 'name', binaryData: 'abc'.bytes)
        team.save(flush: true)
        def value = team.node.getProperty('binaryData')
        then:
        value.class == byte[].class
        value == 'abc'.bytes
    }

}

@Entity
class Tournament {
    Long id
    Long version
    String name
    List teams
    static hasMany = [teams: Team ]
}

@Entity
class Team {
    Long id
    Long version
    String name
    Club club
    byte[] binaryData
}

@Entity
class Club {
    Long id
    Long version
    String name
    List teams
    static hasMany = [teams: Team ]
}

