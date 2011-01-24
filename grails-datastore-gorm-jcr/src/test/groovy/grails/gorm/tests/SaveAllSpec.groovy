package grails.gorm.tests

class SaveAllSpec extends GormDatastoreSpec{

    def cleanup() {
      println 'cleanup'
      def nativeSession = session.nativeInterface
      def wp = nativeSession.getWorkspace();
      def qm = wp.getQueryManager();

      def q = qm.createQuery("//Person", javax.jcr.query.Query.XPATH);
      def qr = q.execute()
      def itr = qr.getNodes();
      itr.each { it.remove() }

    } 

	def "Test that many objects can be saved at once using multiple arguments"() {
		given:
			def bob = new Person(firstName:"Bob", lastName:"Builder")
			def fred = new Person(firstName:"Fred", lastName:"Flintstone")
			def joe = new Person(firstName:"Joe", lastName:"Doe")
			
			Person.saveAll(bob, fred, joe)
			
		when:
			def total = Person.count()
			def results = Person.list()
		then:
			total == 3
			results.every { it.id != null } == true
	}
	
	def "Test that many objects can be saved at once using a list"() {
		given:
			def bob = new Person(firstName:"Bob", lastName:"Builder")
			def fred = new Person(firstName:"Fred", lastName:"Flintstone")
			def joe = new Person(firstName:"Joe", lastName:"Doe")
			
			Person.saveAll(*[bob, fred, joe])
			
		when:
			def total = Person.count()
			def results = Person.list()
		then:
			total == 3
			results.every { it.id != null } == true
	}
}
