package grails.gorm.tests

/**
 * some more unreleated testcases, in more belong together logically, consider refactoring them into a seperate spec
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
}