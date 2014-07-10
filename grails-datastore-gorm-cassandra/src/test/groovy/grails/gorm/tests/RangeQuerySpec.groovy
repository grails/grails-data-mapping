package grails.gorm.tests

import groovy.time.TimeCategory

/**
 * Abstract base test for querying ranges. Subclasses should do the necessary setup to configure GORM
 */
class RangeQuerySpec extends GormDatastoreSpec {   
    
    void "Test between query with dates"() {
        given:
            def now = new Date()
            use(TimeCategory) {
                new Publication(title:"The Guardian", datePublished: now - 5.minutes).save()
                new Publication(title:"The Guardian", datePublished: now - 5.days).save()
                new Publication(title:"The Guardian", datePublished: now - 10.days).save()
            }

        when:
            def results = use(TimeCategory) {
                Publication.findAllByTitleAndDatePublishedBetween("The Guardian", now-6.days, now)
            }

        then:
            results != null
            results.size() == 2
    }

    void "Test between query"() {
        given:
            int age = 40
            ["Bob", "Fred", "Barney", "Frank", "Joe", "Ernie"].each { new PersonLastNamePartitionKey(firstName:"A", lastName:it, age: age--).save() }

        when:
            def results = PersonLastNamePartitionKey.findAllByFirstNameAndAgeBetween('A', 38, 40, [allowFiltering:true])

        then:
            3 == results.size()

        when:
            results = PersonLastNamePartitionKey.findAllByFirstNameAndAgeBetween('A',38, 40, [allowFiltering:true])

        then:
            3 == results.size()

            results.find { it.lastName == "Bob" } != null
            results.find { it.lastName == "Fred" } != null
            results.find { it.lastName == "Barney" } != null

        when:
            results = PersonLastNamePartitionKey.findAllByAgeBetweenOrName(38, 40, "Ernie")

        then:
            thrown UnsupportedOperationException
    }

    void "Test greater than or equal to and less than or equal to queries"() {
        given:

            int age = 40
            ["Bob", "Fred", "Barney", "Frank", "Joe", "Ernie"].each { new PersonLastNamePartitionKey(firstName:"A", lastName:it, age: age--).save() }

        when:
            def results = PersonLastNamePartitionKey.findAllByFirstNameAndAgeGreaterThanEquals('A', 38, [allowFiltering:true])

        then:
            3 == results.size()
            results.find { it.age == 38 } != null
            results.find { it.age == 39 } != null
            results.find { it.age == 40 } != null

        when:
            results = PersonLastNamePartitionKey.findAllByFirstNameAndAgeLessThanEquals('A', 38, [allowFiltering:true])

        then:
            4 == results.size()
            results.find { it.age == 38 } != null
            results.find { it.age == 37 } != null
            results.find { it.age == 36 } != null
            results.find { it.age == 35 } != null
    }

    void 'Test InRange Dynamic Finder'() {
        given:
            new PersonLastNamePartitionKey(firstName: 'A', lastName: 'Brown', age: 11).save()
            new PersonLastNamePartitionKey(firstName: 'A', lastName: 'Johnson', age: 14).save()
            new PersonLastNamePartitionKey(firstName: 'A', lastName: 'Patel', age: 41).save()
            new PersonLastNamePartitionKey(firstName: 'A', lastName: 'Lee', age: 41).save()

        when:
            int count = PersonLastNamePartitionKey.countByFirstNameAndAgeInRange('A', 14..41, [allowFiltering:true])

        then:
            3 == count

        when:
            count = PersonLastNamePartitionKey.countByFirstNameAndAgeInRange('A', 41..14, [allowFiltering:true])

        then:
            3 == count

        when:
            count = PersonLastNamePartitionKey.countByFirstNameAndAgeInRange('A', 14..<30, [allowFiltering:true])

        then:
            1 == count

        when:
            count = PersonLastNamePartitionKey.countByFirstNameAndAgeInRange('A', 14..<42, [allowFiltering:true])

        then:
            3 == count

        when:
            count = PersonLastNamePartitionKey.countByFirstNameAndAgeInRange('A', 15..40, [allowFiltering:true])

        then:
            0 == count
    }
}
