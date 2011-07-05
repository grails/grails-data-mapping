package grails.gorm.tests

import org.apache.log4j.BasicConfigurator

class ManyToManySpec extends GormDatastoreSpec {

    def setupSpec() {
        new BasicConfigurator().configure()
    }

    def "check if manytomany relationsships are persistent correctly"() {
        setup:
            def user = new User(username: 'user1').addToRoles(new Role(role:'role1'))
            user.save(flush:true)
            session.clear()

        when:
            user = User.findByUsername('user1')

        then:
            user.roles
            user.roles.size()==1
            user.roles.every { it instanceof Role }

        when:
            def role = Role.findByRole('role1')

        then:
            role.people
            role.people.size()==1
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
            [
                    'user1': ['ROLE_USER'],
                    'user2': ['ROLE_ADMIN', 'ROLE_USER']
            ].each { username, roles ->
                user = new User(username: username)
                roles.each {
                    user.addToRoles(Role.findByRole(it))
                }
                user.save()
            }

            session.flush()
            session.clear()

        then:
            User.count()==3
            User.findByUsername('user1').roles.size() == 1
            User.findByUsername('user2').roles.size() == 2
            Role.findByRole('ROLE_USER').people.size() == 2
            Role.findByRole('ROLE_ADMIN').people.size() == 2

    }
}

class User {
    Long id
    Long version
    String username
    static belongsTo = Role
    Set roles = [] as Set

    static hasMany = [ roles: Role]
}

class Role {
    Long id
    Long version
    String role
    Set people = [] as Set
    static hasMany = [ people: User]
}

