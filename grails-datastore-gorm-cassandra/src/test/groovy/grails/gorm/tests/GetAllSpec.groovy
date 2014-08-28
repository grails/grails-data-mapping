package grails.gorm.tests


/**
 * @author Graeme Rocher
 */
class GetAllSpec extends GormDatastoreSpec {

    void "Test the getAll method works with no arguments"() {
        given:"some sample data"
            new Person(firstName:"Bob", lastName:"Builder").save()
            new Person(firstName:"Fred", lastName:"Flintstone").save()
            new Person(firstName:"Joe", lastName:"Doe").save(flush: true)
			new PersonLastNamePartitionKey(firstName:"Bob", lastName:"Builder").save()
			new PersonLastNamePartitionKey(firstName:"Fred", lastName:"Flintstone").save()
			new PersonLastNamePartitionKey(firstName:"Joe", lastName:"Doe").save(flush: true)

        when:"The getAll method is used to retrieve all people"
            def results = Person.getAll()
			def results2 = PersonLastNamePartitionKey.getAll()

        then:"The correct number of results is returned"
            results.size() == 3
            results.every { it instanceof Person }
			results2.size() == 3
			results2.every { it instanceof PersonLastNamePartitionKey }
    }

    void "Test the getAll method works with arguments"() {
        given:"some sample data"
        	def bob = new Person(firstName:"Bob", lastName:"Builder").save()
            def fred = new Person(firstName:"Fred", lastName:"Flintstone").save()
            def joe = new Person(firstName:"Joe", lastName:"Doe").save(flush: true)

        when:"The getAll method is used to retrieve all people"
        	def results = Person.getAll(bob.id,fred.id)

        then:"The correct number of results is returned"
            results.size() == 2
            results.any {  it.id == bob.id }
            results.any {  it.id == fred.id }
    }

    void "Test the getAll method works with a list argument"() {
        given:"some sample data"
    		def bob = new Person(firstName:"Bob", lastName:"Builder").save()
            def fred = new Person(firstName:"Fred", lastName:"Flintstone").save()
            def joe = new Person(firstName:"Joe", lastName:"Doe").save(flush: true)

        when:"The getAll method is used to retrieve all pets"
        	def results = Person.getAll([bob.id,fred.id])

        then:"The correct number of results is returned"
            results.size() == 2
            results.any {  it.id == bob.id }
            results.any {  it.id == fred.id }
    }    
}
