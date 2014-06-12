package grails.gorm.tests

import grails.gorm.tests.ChildEntity
import grails.gorm.tests.GormDatastoreSpec
import grails.gorm.tests.TestEntity
import spock.lang.Ignore

/**
 * Removed some projection tests from standard CriteriaBuilderSpec because DynamoDB allows only count(*) in the select.
 * The rest is identical to the main CriteriaBuilderSpec.
 */

class CriteriaBuilderSpec extends GormDatastoreSpec {
    void "Test idEq method"() {
        given:
            def entity = new TestEntity(name:"Bob", age: 44, child:new ChildEntity(name:"Child")).save(flush:true)

        when:
            def result = TestEntity.createCriteria().get { idEq entity.id }

        then:
            result != null
            result.name == 'Bob'
    }

    void "Test disjunction query"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save() }
            def criteria = TestEntity.createCriteria()

        when:
            def results = criteria.list {
                or {
                    like('name', 'B%')
                    eq('age', 41)
                }
            }

        then:
            3 == results.size()
    }

    void "Test conjunction query"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney", "Frank"].each { new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save() }

            def criteria = TestEntity.createCriteria()

        when:
            def results = criteria.list {
                and {
                    like('name', 'B%')
                    eq('age', 40)
                }
            }

        then:
            1 == results.size()
    }

    void "Test list() query"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney", "Frank"].each {
                new TestEntity(name:it, age: age++, child: new ChildEntity(name:"$it Child")).save()
            }

            def criteria = TestEntity.createCriteria()

        when:
            def results = criteria.list {
                 like('name', 'B%')
            }

        then:
            2 == results.size()

        when:
            criteria = TestEntity.createCriteria()
            results = criteria.list {
                like('name', 'B%')
                maxResults 1
            }

        then:
            1 == results.size()
    }

    void "Test count()"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney", "Frank"].each {
                new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save()
            }

            def criteria = TestEntity.createCriteria()

        when:
            def result = criteria.count {
                like('name', 'B%')
            }

        then:
            2 == result
    }

    void "Test obtain a single result"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney", "Frank"].each {
                new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save()
            }

            def criteria = TestEntity.createCriteria()

        when:
            def result = criteria.get {
                eq('name', 'Bob')
            }

        then:
            result != null
            "Bob" == result.name
    }

    void "Test order by a property name"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney", "Frank"].each {
                new TestEntity(name:it, age: age++, child:new ChildEntity(name:"$it Child")).save()
            }

            def criteria = TestEntity.createCriteria()

        when:
            def results = criteria.list {
                like('name', 'B%')
                order "age"
            }

        then:
            "Bob" == results[0].name
            "Barney" == results[1].name

        when:
        criteria = TestEntity.createCriteria()
            results = criteria.list {
                like('name', 'B%')
                order "age", "desc"
            }

        then:
            "Barney" == results[0].name
            "Bob" == results[1].name
    }
}