package grails.gorm.tests

class ManyToManySpec extends GormDatastoreSpec {

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

