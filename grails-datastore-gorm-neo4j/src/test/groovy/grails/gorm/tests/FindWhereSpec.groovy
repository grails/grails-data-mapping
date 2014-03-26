package grails.gorm.tests

/**
 * @author stefan armbruster
 */
class FindWhereSpec extends GormDatastoreSpec {

    def setup() {
        new Person(firstName: 'Jake', lastName: 'Brown', age: 11).save()
        new Person(firstName: 'Zack', lastName: 'Brown', age: 14).save()
        new Person(firstName: 'Jeff', lastName: 'Brown', age: 41).save()
        new Person(firstName: 'Zack', lastName: 'Galifianakis', age: 41).save(flush: true)
        session.clear()
    }

    void 'findWhere with simple property'() {
        expect:
        Person.findWhere(lastName: 'Brown').lastName == 'Brown'
    }

    void 'findWhere with multiple properties'() {
        expect:
        Person.findWhere(lastName: 'Brown', age: 41).firstName == 'Jeff'
    }

    void 'findWhere with non matching property'() {
        expect:
        Person.findWhere(lastName: 'Meyer') == null
    }

    void 'findWhere with non-declared property'() {
        expect:
        Person.findWhere(invalidProperty: 'Brown') == null
    }

    void 'findWhere with dynamic property'() {
        when:
        def property = 'lastName'
        then:
        Person.findWhere((property): 'Brown').lastName == 'Brown'
    }

    void 'findWhere with GString property'() {
        when:
        def property='lastName'
        then:
        Person.findWhere("${property}":'Brown').lastName == 'Brown'
    }

}


