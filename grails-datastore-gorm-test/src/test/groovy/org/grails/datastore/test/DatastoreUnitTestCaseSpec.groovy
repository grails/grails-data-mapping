package org.grails.datastore.test

import grails.datastore.test.DatastoreUnitTestMixin
import spock.lang.Specification
import grails.test.GrailsUnitTestCase

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 31, 2010
 * Time: 3:18:26 PM
 * To change this template use File | Settings | File Templates.
 */
class DatastoreUnitTestCaseSpec extends Specification {

  void "Test mock domain"() {
    given:
     TestTests tt = new TestTests()

    tt.metaClass.mixin DatastoreUnitTestMixin 

    when:
      tt.setUp()
      tt.testCRUD()
      tt.tearDown()

    then:
      true == true 
  }
}
@Mixin(DatastoreUnitTestMixin)
class TestTests extends GrailsUnitTestCase{

  protected void setUp() {
    super.setUp();
    connect()
  }

  protected void tearDown() {
    super.tearDown();
    disconnect()
  }


  void testCRUD() {
    mockDomain TestDomain

    def t = new TestDomain(name:"Bob")
    t.save()

    assert t.id != null

    t = TestDomain.get(t.id)

    assert t != null


  }
}
class TestDomain {
  Long id
  Long version
  String name
}
