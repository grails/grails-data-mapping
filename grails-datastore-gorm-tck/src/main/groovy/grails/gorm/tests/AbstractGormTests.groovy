package grails.gorm.tests

import org.junit.After
import org.junit.Before

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 26, 2010
 * Time: 12:19:06 PM
 * To change this template use File | Settings | File Templates.
 */
abstract class AbstractGormTests {

  @Before
  void startup() {
    cleanRegistry()
  }

  @After
  void cleanup() {
    cleanRegistry()
  }

  private def cleanRegistry() {
    GroovySystem.metaClassRegistry.removeMetaClass TestEntity
    GroovySystem.metaClassRegistry.removeMetaClass ChildEntity
  }

}
