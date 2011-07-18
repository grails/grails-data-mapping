package grails.gorm.tests

//import org.apache.log4j.BasicConfigurator

class ManyToManySpec extends GormDatastoreSpec {

    def setupSpec() {
        //new BasicConfigurator().configure()
    }

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
}

class User {
    Long id
    Long version
    String username
    Set roles = []

    boolean equals(other) {
        other?.username == username
    }

    int hashCode() {
        username ? username.hashCode() : 0
    }

    static hasMany = [roles: Role]
    static belongsTo = Role
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
