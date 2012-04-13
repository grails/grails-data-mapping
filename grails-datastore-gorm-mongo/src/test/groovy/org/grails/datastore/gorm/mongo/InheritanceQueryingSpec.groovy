package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity

/**
 */
class InheritanceQueryingSpec extends GormDatastoreSpec{
    def cleanup() {
        A.get("id")?.delete(flush:true)
        B.get("id")?.delete(flush:true)
    }

    def setup() {
        B b = new B()
        b.id = "id"
        b.prop = "value"
        b.save(failOnError:true, flush:true)
    }

    def "Test collection and count sizes"() {
        expect: "Both collections have 1"
            B.collection.count()==0
            A.collection.count()==1

        and: "Both counts have 1"
            B.count()==1
            A.count()==1
    }

    def "Test listing"() {
        when: "B has them in the list"
            def bList = B.list()

        then:
            bList.size()==1
            bList[0].id=="id"

        when:
            def aList = A.list()

        then:
            aList.size()==1
            aList[0].id=="id"
    }

    def "Test getting"() {
        when:
            B b = B.get("id")

        then:
            b?.id=="id"
            b.prop=="value"

        when:
            def a = A.get("id")

        then:
            a?.id=="id"
            a instanceof B
            a.hasProperty("prop")
    }

    def "Access prop through mongo"() {
        when:
            def jsonString = (A.collection.findOne(_id:"id")).toString()

        then:
            jsonString.contains("id")
            jsonString.contains("prop")

    }

    @Override
    List getDomainClasses() {
        [A,B]
    }


}

@Entity
class A {
    static mapWith = "mongo"

    static mapping = {
        id generator:'assigned', name:'id', type:'string'
    }
    String id
}

@Entity
class B extends A {
    static mapWith = "mongo"

    static constraints = {
    }
    String prop
}
