package grails.gorm.rx.mongodb

import org.codehaus.groovy.ast.ClassNode
import spock.lang.Specification

/**
 * Created by graemerocher on 25/05/16.
 */
class ParseSpec extends Specification {

    void "test parse an entity"() {

        when:"An entity is parsed as a class"
        def cls = new GroovyClassLoader().parseClass('''
import grails.gorm.rx.mongodb.*

class MyObject implements RxMongoEntity<MyObject> {
    String name
}
''')

        then:"It is a valid ClassNode"
        new ClassNode(cls).methods
    }

    void "test parse an entity with @Entity as well"() {

        when:"An entity is parsed as a class"
        def cls = new GroovyClassLoader().parseClass('''
import grails.gorm.rx.mongodb.*
import grails.gorm.annotation.*
@Entity
class MyObject implements RxMongoEntity<MyObject> {
    String name
}
''')

        then:"It is a valid ClassNode"
        new ClassNode(cls).methods
    }
}
