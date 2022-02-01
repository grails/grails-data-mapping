package org.grails.datastore.gorm.async.transform

import grails.async.Promise
import groovy.transform.Generated
import org.codehaus.groovy.ast.ClassNode
import spock.lang.Specification

import java.lang.reflect.Method

/**
 * Created by graemerocher on 01/07/16.
 */
class DelegateAsyncSpec extends Specification {

    void "Test DelegateAsync with parameterized return types"() {
        given:"a class with parameterized return types"
        GroovyClassLoader gcl = new GroovyClassLoader()
        def cls = gcl.parseClass('''
import org.grails.datastore.gorm.async.transform.*
class Bar<D>{
    @DelegateAsync Foo<D> foo
}
interface Foo<D> {
    public <T> T  withTransaction(Closure<T> callable)
}


''')
        expect:"The method is retrieved"
        new ClassNode(cls).methods
        
        Method method = cls.getMethod("withTransaction", Closure)
        Promise.isAssignableFrom( method.returnType )

        and: 'marked as Generated'
        method.isAnnotationPresent(Generated)
    }

}
