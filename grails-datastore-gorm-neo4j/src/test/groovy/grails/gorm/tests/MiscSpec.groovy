package grails.gorm.tests

/**
 * some more unrelated testcases, in more belong together logically, consider refactoring them into a seperate spec
 */
class MiscSpec extends GormDatastoreSpec {

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
            User.count()==2
            user in User.list()
    }

    def "test object identity in relationships"() {
        setup:
            def role1 = new Role(role: 'role1')
            def role2 = new Role(role: 'role2')

            new User(username: 'user1', roles: [role1, role2]).save(flush:true)
            session.clear()

        when:
            def user = User.findByUsername('user1')
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
}
