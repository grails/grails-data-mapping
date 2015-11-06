package org.grails.compiler.gorm

import grails.compiler.ast.ClassInjector
import grails.persistence.Entity

import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.grails.compiler.gorm.GormTransformer;
import org.grails.compiler.injection.DefaultGrailsDomainClassInjector
import org.grails.compiler.injection.GrailsAwareClassLoader
import org.grails.core.DefaultGrailsDomainClass
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
            e.message.contains '''Either class [TestEntity] is not a domain class or GORM has not been initialized correctly or has already been shutdown. If you are unit testing your entities using the mocking APIs'''
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
}
