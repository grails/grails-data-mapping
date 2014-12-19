package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.codehaus.groovy.grails.orm.hibernate.ExtendAbstractJavaClassTests.JavaClass
import org.junit.Test

/**
 * @author graemerocher
 */
class ExtendAbstractJavaClassTests extends AbstractGrailsHibernateTests{

    @Test
    void testPersistClassThatExtendsJavaClass() {
        def e = ExtendAbstractJava.newInstance()

        e.save()

        assert e.id
    }

    static abstract class JavaClass {}

    @Override
    protected getDomainClasses() {
        [ExtendAbstractJava]
    }
}

@Entity
class ExtendAbstractJava extends JavaClass {
    Long id
    Long version

}

