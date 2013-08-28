package grails.gorm.tests

class MultipleOrderBySpec extends GormDatastoreSpec {

    void "Test multiple order by with list() method"() {
        given:
            def age = 40

            ["Bob", "Fred", "Ernie", "Bob", "Joe", "Ernie"].each {
                new TestEntity(name: it, age: age++).save()
            }

        when:
            def results = TestEntity.list(sort: [name: "asc", age: "asc"])

        then:
            results[0].name == "Bob" && results[0].age == 40
            results[1].name == "Bob" && results[1].age == 43
            results[2].name == "Ernie" && results[2].age == 42
            results[3].name == "Ernie" && results[3].age == 45

        when:
            results = TestEntity.list(sort: [name: "desc", age: "desc"])

        then:
            results[0].name == "Joe" && results[0].age == 44
            results[1].name == "Fred" && results[1].age == 41
            results[2].name == "Ernie" && results[2].age == 45
            results[3].name == "Ernie" && results[3].age == 42

        when:
            results = TestEntity.list(sort: [name: "asc", age: "desc"])

        then:
            results[0].name == "Bob" && results[0].age == 43
            results[1].name == "Bob" && results[1].age == 40
            results[2].name == "Ernie" && results[2].age == 45
            results[3].name == "Ernie" && results[3].age == 42

        when:
            results = TestEntity.list(sort: [age: "asc", name: "asc"])

        then:
            results[0].name == "Bob" && results[0].age == 40
            results[1].name == "Fred" && results[1].age == 41
            results[2].name == "Ernie" && results[2].age == 42
            results[3].name == "Bob" && results[3].age == 43
    }

    void "Test multiple order by property name with dynamic finder"() {
        given:
            def age = 40

            ["Bob", "Fred", "Ernie", "Bob", "Joe", "Ernie"].each {
                new TestEntity(name: it, age: age++).save()
            }

        when:
            def results = TestEntity.findAllByAgeGreaterThanEquals(40, [sort: [name: "asc", age: "asc"]])

        then:
            results[0].name == "Bob" && results[0].age == 40
            results[1].name == "Bob" && results[1].age == 43
            results[2].name == "Ernie" && results[2].age == 42
            results[3].name == "Ernie" && results[3].age == 45

        when:
            results = TestEntity.findAllByAgeGreaterThanEquals(40, [sort: [name: "desc", age: "desc"]])

        then:
            results[0].name == "Joe" && results[0].age == 44
            results[1].name == "Fred" && results[1].age == 41
            results[2].name == "Ernie" && results[2].age == 45
            results[3].name == "Ernie" && results[3].age == 42

        when:
            results = TestEntity.findAllByAgeGreaterThanEquals(40, [sort: [name: "asc", age: "desc"]])

        then:
            results[0].name == "Bob" && results[0].age == 43
            results[1].name == "Bob" && results[1].age == 40
            results[2].name == "Ernie" && results[2].age == 45
            results[3].name == "Ernie" && results[3].age == 42

        when:
            results = TestEntity.findAllByAgeGreaterThanEquals(40, [sort: [age: "asc", name: "asc"]])

        then:
            results[0].name == "Bob" && results[0].age == 40
            results[1].name == "Fred" && results[1].age == 41
            results[2].name == "Ernie" && results[2].age == 42
            results[3].name == "Bob" && results[3].age == 43
    }
}
