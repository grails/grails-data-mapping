package grails.gorm.tests

/**
 * @author graemerocher
 */
class AttachMethodSpec extends GormDatastoreSpec {

    void "Test attach method"() {
        given:
            def test = new Person(firstName:"Bob", lastName:"Builder").save()
            def test2 = new PersonAssignedId(firstName:"Bob", lastName:"Builder").save()

        when:
            session.flush()

        then:
            session.contains(test) == true            
            test.isAttached()            
            test.attached
            
            session.contains(test2) == true
            test2.isAttached()
            test2.attached

        when:
            test.discard()
            test2.discard()

        then:
            !session.contains(test)
            !test.isAttached()
            !test.attached
            
            !session.contains(test2)
            !test2.isAttached()
            !test2.attached

        when:
            test.attach()
            test2.attach()

        then:
            session.contains(test)
            test.isAttached()
            test.attached
            
            session.contains(test2)
            test2.isAttached()
            test2.attached

        when:
            test.discard()
            test2.discard()

        then:
            test == test.attach()
            test2 == test2.attach()
    }
}
