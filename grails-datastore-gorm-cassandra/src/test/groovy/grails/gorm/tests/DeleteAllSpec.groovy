package grails.gorm.tests

class DeleteAllSpec extends GormDatastoreSpec {

    def "Test that many objects can be deleted at once using multiple arguments"() {
        given:
            def bob = new Person(firstName:"Bob", lastName:"Builder").save(flush: true)
            def fred = new Person(firstName:"Fred", lastName:"Flintstone").save(flush: true)
            def joe = new Person(firstName:"Joe", lastName:"Doe").save(flush: true)
            Person.deleteAll(bob, fred, joe)
            session.flush()

        when:
            def total = Person.count()
        then:
            total == 0
    }
    
    def "Test that objects with assigned id can be deleted at once using multiple arguments"() {
        given:
            def bob = new PersonAssignedId(firstName:"Bob", lastName:"Builder").save(flush: true)
            def fred = new PersonAssignedId(firstName:"Fred", lastName:"Flintstone").save(flush: true)
            def joe = new PersonAssignedId(firstName:"Joe", lastName:"Doe").save(flush: true)
            PersonAssignedId.deleteAll(bob, fred, joe)
            session.flush()

        when:
            def total = PersonAssignedId.count()
        then:
            total == 0
    }

    
    def "Test that many objects can be deleted using an iterable"() {
        given:
            def bob = new Person(firstName:"Bob", lastName:"Builder").save(flush: true)
            def fred = new Person(firstName:"Fred", lastName:"Flintstone").save(flush: true)
            def joe = new Person(firstName:"Joe", lastName:"Doe").save(flush: true)

            Vector<Person> people = new Vector<Person>()
            people.add(bob)
            people.add(fred)
            people.add(joe)

            Person.deleteAll(people)
            session.flush()

        when:
            def total = Person.count()
        then:
            total == 0
    }
}
