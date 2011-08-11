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
}
