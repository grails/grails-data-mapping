package grails.gorm.tests

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Sep 6, 2010
 * Time: 3:33:52 PM
 * To change this template use File | Settings | File Templates.
 */
class AttachMethodSpec extends GormDatastoreSpec{

  void "Test attach method"() {
    given:
      def test = new Person(firstName:"Bob", lastName:"Builder").save()

    when:
      session.flush()
    then:
      session.contains(test) == true
      test.isAttached()
      test.attached

    when:
      test.discard()

    then:
      !session.contains(test)
      !test.isAttached()
      !test.attached

    when:
      test.attach()

    then:
      session.contains(test)
      test.isAttached()
      test.attached

    when:
      test.discard()

    then:
      test == test.attach()

  }
}
