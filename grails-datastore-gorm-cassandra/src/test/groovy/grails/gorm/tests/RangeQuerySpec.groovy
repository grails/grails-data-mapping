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
            ["Bob", "Fred", "Barney", "Frank", "Joe", "Ernie"].each { new PersonAssignedId(firstName:it, lastName:"Robinson", age: age--).save() }

        when:
            def results = PersonAssignedId.findAllByLastNameAndAgeBetween('Robinson', 38, 40, [allowFiltering:true])

        then:
            3 == results.size()

        when:
            results = PersonAssignedId.findAllByLastNameAndAgeBetween('Robinson',38, 40, [allowFiltering:true])

        then:
            3 == results.size()

            results.find { it.firstName == "Bob" } != null
            results.find { it.firstName == "Fred" } != null
            results.find { it.firstName == "Barney" } != null

        when:
            results = PersonAssignedId.findAllByAgeBetweenOrName(38, 40, "Ernie")

        then:
            thrown UnsupportedOperationException
    }

    void "Test greater than or equal to and less than or equal to queries"() {
        given:

            int age = 40
            ["Bob", "Fred", "Barney", "Frank", "Joe", "Ernie"].each { new PersonAssignedId(firstName:it, lastName:"Robinson", age: age--).save() }

        when:
            def results = PersonAssignedId.findAllByLastNameAndAgeGreaterThanEquals('Robinson', 38, [allowFiltering:true])

        then:
            3 == results.size()
            results.find { it.age == 38 } != null
            results.find { it.age == 39 } != null
            results.find { it.age == 40 } != null

        when:
            results = PersonAssignedId.findAllByLastNameAndAgeLessThanEquals('Robinson', 38, [allowFiltering:true])

        then:
            4 == results.size()
            results.find { it.age == 38 } != null
            results.find { it.age == 37 } != null
            results.find { it.age == 36 } != null
            results.find { it.age == 35 } != null
    }

    void 'Test InRange Dynamic Finder'() {
        given:
            new PersonAssignedId(firstName: 'Jake', lastName: 'Brown', age: 11).save()
            new PersonAssignedId(firstName: 'Zack', lastName: 'Brown', age: 14).save()
            new PersonAssignedId(firstName: 'Jeff', lastName: 'Brown', age: 41).save()
            new PersonAssignedId(firstName: 'Jack', lastName: 'Brown', age: 41).save()

        when:
            int count = PersonAssignedId.countByLastNameAndAgeInRange('Brown', 14..41, [allowFiltering:true])

        then:
            3 == count

        when:
            count = PersonAssignedId.countByLastNameAndAgeInRange('Brown', 41..14, [allowFiltering:true])

        then:
            3 == count

        when:
            count = PersonAssignedId.countByLastNameAndAgeInRange('Brown', 14..<30, [allowFiltering:true])

        then:
            1 == count

        when:
            count = PersonAssignedId.countByLastNameAndAgeInRange('Brown', 14..<42, [allowFiltering:true])

        then:
            3 == count

        when:
            count = PersonAssignedId.countByLastNameAndAgeInRange('Brown', 15..40, [allowFiltering:true])

        then:
            0 == count
    }
}
