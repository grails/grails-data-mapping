package grails.gorm.tests

import grails.persistence.Entity

class DefaultSortSpec extends GormDatastoreSpec {

    void "Test default sort"() {
        given:
            domainClasses.each { domainClass ->
                def age = 40
                ["Bob", "Fred", "Ernie", "Bob", "Joe", "Ernie"].each {
                    domainClass.newInstance(name: it, age: age++).save()
                }
            }

        when:
            def results = TestEntityWithSortProperty.list()

        then:
            results[0].name == "Bob"
            results[1].name == "Bob"
            results[2].name == "Ernie"
            results[3].name == "Ernie"

        when:
            results = TestEntityWithSortOrderProperty.list()

        then:
            results[0].name == "Joe"
            results[1].name == "Fred"
            results[2].name == "Ernie"
            results[3].name == "Ernie"

        when:
            results = TestEntityWithSingleEntrySortOrderMap.list()

        then:
            results[0].name == "Joe"
            results[1].name == "Fred"
            results[2].name == "Ernie"
            results[3].name == "Ernie"

        when:
            results = TestEntityWithMultiEntrySortOrderMap.list()

        then:
            results[0].name == "Joe" && results[0].age == 44
            results[1].name == "Fred" && results[1].age == 41
            results[2].name == "Ernie" && results[2].age == 45
            results[3].name == "Ernie" && results[3].age == 42
    }

    @Override
    List getDomainClasses() {
        [
            TestEntityWithSortProperty,
            TestEntityWithSortOrderProperty,
            TestEntityWithSingleEntrySortOrderMap,
            TestEntityWithMultiEntrySortOrderMap
        ]
    }
}

@Entity
class TestEntityWithSortProperty {
    Long id
    Long version
    String name
    Integer age
    static mapping = {
        sort "name"
    }
}

@Entity
class TestEntityWithSortOrderProperty {
    Long id
    Long version
    String name
    Integer age
    static mapping = {
        sort "name"
        order "desc"
    }
}

@Entity
class TestEntityWithSingleEntrySortOrderMap {
    Long id
    Long version
    String name
    Integer age
    static mapping = {
        sort name: "desc"
    }
}

@Entity
class TestEntityWithMultiEntrySortOrderMap {
    Long id
    Long version
    String name
    Integer age
    static mapping = {
        sort name: "desc", age: "desc"
    }
}
