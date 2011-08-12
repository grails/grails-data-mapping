package grails.gorm.tests

class ManyToManySpec extends GormDatastoreSpec {

    /*def setupSpec() {
        new BasicConfigurator().configure()
    }*/

    def "check if manytomany relationships are persistent correctly"() {
        setup:
            def user = new User(username: 'user1').addToRoles(new Role(role:'role1'))
            user.save(flush:true)
            session.clear()

        when:
            user = User.findByUsername('user1')

        then:
            user.roles
            1 == user.roles.size()
            user.roles.every { it instanceof Role }

        when:
            def role = Role.findByRole('role1')

        then:
            role.people
            1 == role.people.size()
            role.people.every { it instanceof User }
    }

    def "insert multiple instances with many-to-many"() {

        setup:
            ['ROLE_USER', 'ROLE_ADMIN'].each {
                new Role(role:it).save()
            }

            def user = new User(username: 'initial')
            user.addToRoles(Role.findByRole('ROLE_ADMIN'))
            user.save(flush:true)
            session.clear()

        when:
            ['user1': ['ROLE_USER'],
             'user2': ['ROLE_ADMIN', 'ROLE_USER']].each { username, roles ->
                user = new User(username: username)
                roles.each {
                    user.addToRoles(Role.findByRole(it))
                }
                user.save()
            }

            session.flush()
            session.clear()

        then:
            3 == User.count()
            1 == User.findByUsername('user1').roles.size()
            2 == User.findByUsername('user2').roles.size()
            2 == Role.findByRole('ROLE_USER').people.size()
            2 == Role.findByRole('ROLE_ADMIN').people.size()
    }

    // TODO: this testcase belongs semantically into OneToManySpec
    def "check multiple one-to-many relationships of the same type"() {
        setup:
        def user = new User(username: 'person1')
        user.save(flush:true)
        session.clear()

        when: "creating a lonely user"
        user = User.findByUsername('person1')

        then: "user has no friends and foes"
        user
        user.friends.size() == 0
        user.foes.size() == 0
        user.bestBuddy == null

        when: "adding some friend and foe"
        user.addToFriends(username:'friend1')
        user.addToFoes(username:'foe1')
        user.save()
        session.flush()
        session.clear()
        user = User.findByUsername('person1')

        then: "friends and foes are found"
        user
        user.friends.size() == 1
        user.friends.every { it.username =~ /friend\d/ }
        user.foes.size() == 1
        user.foes.every { it.username =~ /foe\d/ }
        user.bestBuddy == null

        when: "setting bestbuddy"
        user.bestBuddy = new User(username:'bestBuddy')
        user.save()
        session.flush()
        session.clear()
        user = User.findByUsername('person1')

        then: "bestBuddy is there"
        user.bestBuddy.username == 'bestBuddy'

        and: 'friends and foes are not modified'
        user.friends.size() == 1
        user.friends.every { it.username =~ /friend\d/ }
        user.foes.size() == 1
        user.foes.every { it.username =~ /foe\d/ }

    }

}

class User {
    Long id
    Long version
    String username
    User bestBuddy
    Set roles = []
    Set friends = []
    Set foes = []

    boolean equals(other) {
        other?.username == username
    }

    int hashCode() {
        username ? username.hashCode() : 0
    }

    static hasMany = [ roles: Role, friends: User, foes: User ]
    static forceUnidirectional = [ 'friends', 'foes', 'bestBuddy' ]
    static belongsTo = Role

    static constraints = {
        bestBuddy nullable:true
    }
}

class Role {
    Long id
    Long version
    String role
    Set people = []

    boolean equals(other) {
        other?.role == role
    }

    int hashCode() {
        role ? role.hashCode() : 0
    }

    static hasMany = [people: User]

    static constraints = {
        role(blank: false, unique: true)
    }
}
