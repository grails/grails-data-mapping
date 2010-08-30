package org.grails.datastore.test

import org.junit.runner.RunWith
import grails.gorm.tests.AllTests
import org.junit.runners.model.RunnerBuilder
import org.grails.datastore.gorm.GormEnhancer
import grails.gorm.tests.ChildEntity
import grails.gorm.tests.TestEntity
import org.springframework.datastore.mock.SimpleMapDatastore
import org.springframework.validation.Errors
import org.springframework.util.StringUtils
import org.springframework.validation.Validator
import grails.gorm.tests.CommonTypes

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 30, 2010
 * Time: 11:35:53 AM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(SimpleMapRunner)
class AllSimpleMapTests extends AllTests{

  static class SimpleMapRunner extends AllTests.TestSuite {

    SimpleMapRunner(Class klass, RunnerBuilder builder) {
      super(klass, builder);
    }

    def session
    protected void tearDown() {
      session?.disconnect()
    }

    protected void setUp() {
      def datastore = new SimpleMapDatastore()
      def entity = datastore.mappingContext.addPersistentEntity(TestEntity)
      datastore.mappingContext.addPersistentEntity(ChildEntity)
      datastore.mappingContext.addPersistentEntity(CommonTypes)

      datastore.mappingContext.addEntityValidator(entity, [
              supports: { Class c -> true },
              validate: { Object o, Errors errors ->
                  if(!StringUtils.hasText(o.name)) {
                    errors.rejectValue("name", "name.is.blank")
                  }
              }
      ] as Validator)
      
      new GormEnhancer(datastore).enhance()
      session = datastore.connect()

    }

  }
}
