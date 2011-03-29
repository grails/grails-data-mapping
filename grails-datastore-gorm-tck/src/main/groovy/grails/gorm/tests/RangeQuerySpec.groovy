package grails.gorm.tests

/**
 * Abstract base test for querying ranges. Subclasses should do the necessary setup to configure GORM
 */
class RangeQuerySpec extends GormDatastoreSpec {

    void "Test between query"() {
        given:
            def age = 40
            ["Bob", "Fred", "Barney", "Frank", "Joe", "Ernie"].each { new TestEntity(name:it, age: age--, child:new ChildEntity(name:"$it Child")).save() }

        when:
            def results = TestEntity.findAllByAgeBetween(38, 40)

        then:
            3 == results.size()

        when:
            results = TestEntity.findAllByAgeBetween(38, 40)

        then:
            3 == results.size()

            results.find{ it.name == "Bob" } != null
            results.find{ it.name == "Fred" } != null
            results.find{ it.name == "Barney" } != null

        when:
            results = TestEntity.findAllByAgeBetweenOrName(38, 40, "Ernie")

        then:
            4 == results.size()
    }

    void "Test greater than or equal to and less than or equal to queries"() {
        given:

            def age = 40
            ["Bob", "Fred", "Barney", "Frank", "Joe", "Ernie"].each { new TestEntity(name:it, age: age--, child:new ChildEntity(name:"$it Child")).save() }

        when:
            def results = TestEntity.findAllByAgeGreaterThanEquals(38)

        then:
            3 == results.size()
            results.find { it.age == 38 } != null
            results.find { it.age == 39 } != null
            results.find { it.age == 40 } != null

        when:
            results = TestEntity.findAllByAgeLessThanEquals(38)

        then:
            assert 4 == results.size()
            assert results.find { it.age == 38 } != null
            assert results.find { it.age == 37 }!= null
            assert results.find { it.age == 36 }!= null
            assert results.find { it.age == 35 }!= null
    }
}
