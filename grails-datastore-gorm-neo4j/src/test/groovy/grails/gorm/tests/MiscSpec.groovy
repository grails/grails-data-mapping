package grails.gorm.tests

import grails.persistence.Entity

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
}

@Entity
class Club {
    Long id
    Long version
    String name
    List teams
    static hasMany = [teams: Team ]
}
