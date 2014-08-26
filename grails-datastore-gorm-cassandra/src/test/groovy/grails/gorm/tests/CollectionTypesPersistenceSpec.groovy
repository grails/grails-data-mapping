package grails.gorm.tests

import grails.persistence.Entity


/**
 * @author graemerocher
 */
class CollectionTypesPersistenceSpec extends GormDatastoreSpec {

    @Override
    public List getDomainClasses() {
        [CollectionTypes, Increment]
    }
    
    void "Test basic collection persistence"() {
        given:
            def list = [1,2,3]            
            def set = ["one", "two", "three"] as Set
            def map = [a:1, b:2.5f, z:3l]
            def ct = new CollectionTypes(string: "1", i:2, list: list, map: map, set:set)
        when:
            ct.save(flush:true)
            ct.discard()
            ct = CollectionTypes.get(ct.id)
        then:
            ct   
            "1" == ct.string
            2 == ct.i 
            list == ct.list
            set == ct.set
            map == ct.map       
		
		when: 
			ct.list << 4			
			list << 4
			ct.set.remove("one")
			set.remove("one")
			ct.map << [e:3.1f] 
			map << [e: 3.1f]
			ct.save(flush:true)
			ct.discard()
			ct = CollectionTypes.get(ct.id)
		then:
			ct
			list == ct.list
			set == ct.set
			map == ct.map
			
    }

	void "Test beforeInsert() and beforeUpdate() methods for collections"() {
		when:"An entity is persisted"
    		def p = new Increment()
    		p.save(flush:true)
    		session.clear()
    		p = Increment.get(p.id)

		then:"The collection is updated"
    		p.counter == 1
    		p.history == [0]

		when:"The entity is updated"
    		p.counter = 10
    		p.save(flush:true)
    		session.clear()
    		p = Increment.get(p.id)

		then:"The collection is updated too"
    		p.counter == 11
    		p.history == [0, 10]
	}
}

@Entity
class CollectionTypes {

	String string
	int i
	List<Integer> list
	HashSet<String> set
	Map<String, Float> map
}


@Entity
class Increment {	
	Integer counter = 0
	List<Integer> history = []

	def beforeInsert() {
		inc()
	}

	def beforeUpdate() {
		inc()
	}

	def inc() {
		history << counter++
	}
}