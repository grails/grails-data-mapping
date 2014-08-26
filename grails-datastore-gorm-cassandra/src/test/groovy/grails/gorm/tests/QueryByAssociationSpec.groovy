package grails.gorm.tests

import org.junit.Ignore

/**
 * Abstract base test for query associations. Subclasses should do the necessary setup to configure GORM
 */
@Ignore("Cassandra GORM does not support associations at present")
class QueryByAssociationSpec extends GormDatastoreSpec {

    void "Test query entity by single-ended association"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save() }

        when:
            def child = ChildEntity.findByName("Barney Child")

        then:
            child != null
            child.id != null

        when:
            def t = TestEntity.findByChild(child)

        then:
            t != null
            "Barney" == t.name
    }
}
