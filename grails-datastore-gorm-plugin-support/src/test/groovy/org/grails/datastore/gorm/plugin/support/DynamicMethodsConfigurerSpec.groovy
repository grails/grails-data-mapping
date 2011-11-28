package org.grails.datastore.gorm.plugin.support

import spock.lang.Specification
import org.grails.datastore.mapping.core.Datastore
import org.springframework.transaction.PlatformTransactionManager
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import grails.persistence.Entity

/**
 *
 */
class DynamicMethodsConfigurerSpec extends Specification{

      void setup() {
          ExpandoMetaClass.enableGlobally()
      }
      void cleanup() {
          GroovySystem.metaClassRegistry.removeMetaClass(Simple)
      }

      void "Test dynamic methods are configured correctly with no existing datastore"() {
          given:"A dynamic methods configurer with a single domain class"
            final configurer = systemUnderTest()
            configurer.datastore.mappingContext.addPersistentEntity(Simple)

          when:"Dynamic methods are configured"
            configurer.configure()

          then:"Dynamic methods can be called on the domain"
            Simple.count() == 0
      }

      void "Test dynamic methods are configured correctly with an existing datastore"() {
          given:"A dynamic methods configurer with a single domain class"
            final configurer = systemUnderTest()
            configurer.hasExistingDatastore = true
            configurer.datastore.mappingContext.addPersistentEntity(Simple)

          when:"Dynamic methods are configured"
            configurer.configure()

          then:"Dynamic methods are scoped to the datastore"
            Simple.simple.count() == 0

          when:"And are not included at the top level"
            Simple.count()

          then:"A missing method exception is thrown"
             thrown(MissingMethodException)
      }

      DynamicMethodsConfigurer systemUnderTest() {
          return new SimpleDynamicMethodsConfigurer(new SimpleMapDatastore(), new DatastoreTransactionManager())
      }
}
class SimpleDynamicMethodsConfigurer extends DynamicMethodsConfigurer {

    SimpleDynamicMethodsConfigurer(Datastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager)
    }

    @Override
    String getDatastoreType() {
        return "Simple"
    }

}

@Entity
class Simple {
    Long id
}