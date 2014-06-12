package org.codehaus.groovy.grails.compiler.gorm

import grails.persistence.Entity

import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import org.codehaus.groovy.grails.compiler.injection.ClassInjector
import org.codehaus.groovy.grails.compiler.injection.DefaultGrailsDomainClassInjector
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareClassLoader
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.GormValidationApi
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import org.springframework.validation.Errors

import spock.lang.Specification
import grails.spring.BeanBuilder
import org.springframework.context.ConfigurableApplicationContext

class GormTransformerSpec extends Specification {

    private alwaysInjectGormTransformer = new GormTransformer() {
        boolean shouldInject(URL url) { true }
    }

    private GrailsAwareClassLoader gcl = new GrailsAwareClassLoader()

    void "Test missing method thrown for uninitialized entity"() {
        given:
            gcl.classInjectors = [alwaysInjectGormTransformer] as ClassInjector[]

        when:
            def cls = gcl.parseClass('''
@grails.persistence.Entity
class TestEntity {
    Long id
}
  ''')
            cls.load(1)
        then:
            def e = thrown(IllegalStateException)
            e.message.contains '''Method on class [TestEntity] was used outside of a Grails application.'''
    }

    void 'Test that the compiler will not allow an entity to be marked with @Canonical'() {
        given:
            gcl.classInjectors = [alwaysInjectGormTransformer] as ClassInjector[]

        when:
            def cls = gcl.parseClass('''
package com.demo

@grails.persistence.Entity
@groovy.transform.Canonical
class TestEntity {
    Long id
}
  ''')

        then:
            def e = thrown(MultipleCompilationErrorsException)
            e.message.contains 'Class [com.demo.TestEntity] is marked with @groovy.transform.Canonical which is not supported for GORM entities.'

    }

    void "Test that generic information is added to hasMany collections"() {
        given:
            def domainTransformer = new DefaultGrailsDomainClassInjector() {
                boolean shouldInject(URL url) { true }
            }
            gcl.classInjectors = [alwaysInjectGormTransformer, domainTransformer] as ClassInjector[]

        when:
            def cls = gcl.parseClass('''
@grails.persistence.Entity
class TestEntity {
    Long id

    static hasMany = [associated:Associated]
}

@grails.persistence.Entity
class Associated {
    Long id
}
  ''')

        then:
            cls.getAnnotation(Entity) != null
            cls.getDeclaredField("associated") != null
            cls.getDeclaredField("associated").genericType != null
            cls.getDeclaredField("associated").genericType.getActualTypeArguments()[0] == gcl.loadClass("Associated")
    }

    void "Test that only one annotation is added on already annotated entity"() {
        given:
            gcl.classInjectors = [alwaysInjectGormTransformer] as ClassInjector[]

        when:
            def cls = gcl.parseClass('''
@grails.persistence.Entity
class TestEntity {
    Long id
}
  ''')

        then:
            cls.getAnnotation(Entity) != null
    }

    void "Test transforming a @grails.persistence.Entity marked class doesn't generate duplication methods"() {
        given:
              gcl.classInjectors = [alwaysInjectGormTransformer] as ClassInjector[]

        when:
            def cls = gcl.parseClass('''
@grails.persistence.Entity
class TestEntity {
    Long id
}
  ''')

        then:
            cls
    }

    void "Test that GORM static methods are available on transformation"() {
        given:
              gcl.classInjectors = [alwaysInjectGormTransformer] as ClassInjector[]

        when:
            def cls = gcl.parseClass('''
class TestEntity {
    Long id
}
  ''')
            cls.count()

        then:
            Exception e = thrown()
            assert e instanceof IllegalStateException || e instanceof MissingMethodException

        when:
            registerApiInstance(cls, GormStaticApi, null, true)
            cls.count()

        then:
            Exception e2 = thrown()
            assert e2 instanceof IllegalStateException || e2 instanceof MissingMethodException

        when:
            def ds = new SimpleMapDatastore()
            ds.mappingContext.addPersistentEntity(cls)

            registerApiInstance(cls, GormStaticApi, new GormStaticApi(cls, ds, []), true)

        then:
            cls.count() == 0
    }

    void "Test that the new Errors property is valid"() {
        given:
            def transformer = new GormValidationTransformer() {
                boolean shouldInject(URL url) { true }
            }
            gcl.classInjectors = [transformer] as ClassInjector[]

        when:
            def cls = gcl.parseClass('''
class TestEntity {
    Long id
    Long version
    String name
}
  ''')
            def dc = new DefaultGrailsDomainClass(cls)

        then:
            dc.persistentProperties.size() == 1

        when:
            def obj = dc.newInstance()

        then:
            obj != null
            obj.errors instanceof Errors

        when:
            def ds = new SimpleMapDatastore(new BeanBuilder().createApplicationContext() as ConfigurableApplicationContext)

            registerApiInstance(cls, GormValidationApi, new GormValidationApi(cls, ds), false)
            obj.clearErrors()

        then:
            obj.errors.hasErrors() == false
            obj.hasErrors() == false

        when:
            Errors errors = obj.errors
            errors.reject("bad")

        then:
            obj.hasErrors() == true
    }

    private void registerApiInstance(cls, apiInstanceType, apiInstance, staticApi) {
        new GormEnhancer(null).registerApiInstance(cls, apiInstanceType, apiInstance, staticApi)
    }
}
