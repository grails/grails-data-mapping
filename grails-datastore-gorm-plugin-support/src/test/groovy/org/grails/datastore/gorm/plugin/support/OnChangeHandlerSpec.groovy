package org.grails.datastore.gorm.plugin.support

import spock.lang.Specification
import org.grails.datastore.mapping.core.Datastore
import org.springframework.transaction.PlatformTransactionManager
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import org.grails.datastore.mapping.transactions.DatastoreTransactionManager
import org.springframework.context.support.GenericApplicationContext

/**
 * Tests for onChange event handling
 */
class OnChangeHandlerSpec extends Specification{

    void setup() {
      datastore = new SimpleMapDatastore()
      ExpandoMetaClass.enableGlobally()
    }
    void cleanup() {
      datastore.mappingContext.persistentEntities.each {
          GroovySystem.metaClassRegistry.removeMetaClass(it.javaClass)
      }
    }

    Datastore datastore

    void "Test onChange events for domain classes"() {
        given:"A configured domain class"
            def cls = classUnderTest()
            final entity = datastore.mappingContext.addPersistentEntity(cls)
            methodsConfigurer().configure()

        when:"A dynamic method is called"
            def result = cls.count()

        then:"It works correctly"
           result == 0

        when:"A new version of the class is created on the onChange handler fired"
            def newCls = classUnderTest()
            final onChangeHandler = systemUnderTest()
            onChangeHandler.onChange(mockPlugin(), mockEvent(newCls))
            entity = datastore.mappingContext.getPersistentEntity(cls.name)

        then:"The new class is correctly configured"
            newCls.count() == 0
            entity.javaClass == newCls
    }

    Map mockEvent(Class aClass) {
        [   source: aClass,
            application: [
                isArtefactOfType: { String name, Class c -> true },
                addArtefact:{ String name, Class c -> true }
            ],
            ctx: new GenericApplicationContext()
        ]
    }

    Object mockPlugin() {
        [:]
    }

    protected Class classUnderTest() {
        def cls = new GroovyClassLoader().parseClass("""
import grails.persistence.*

@Entity
class Simple { Long id }
            """)
    }

    OnChangeHandler systemUnderTest() {
        return new SimpleOnChangeHandler(datastore, new DatastoreTransactionManager())
    }

    DynamicMethodsConfigurer methodsConfigurer() {
        return new SimpleDynamicMethodsConfigurer(datastore, new DatastoreTransactionManager())
    }
}
class SimpleOnChangeHandler extends OnChangeHandler {

    SimpleOnChangeHandler(Datastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager)
    }

    @Override
    String getDatastoreType() {
        return "Simple"
    }

}
