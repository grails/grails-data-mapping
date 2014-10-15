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
    		1 == p.counter
    		[0] == p.history 

		when:"The entity is updated"
    		p.counter = 10
    		p.save(flush:true)
    		session.clear()
    		p = Increment.get(p.id)

		then:"The collection is updated too"
    		11 == p.counter 
    		[0, 10] == p.history 
	}
	
	void "Test update simple types"() {
		given:
            def list = [1,2,3]            
            def set = ["one", "two", "three"] as Set
            def map = [a:1, b:2.5f, z:3l]
            def ct = new CollectionTypes(string: "1", i:2, list: list, map: map, set:set)        
            ct.save(flush:true)
            ct.discard()
            ct = CollectionTypes.get(ct.id)
			
        when:
			ct.string = "2"
			ct.i = 3
            ct.list << 4						
			ct.set.remove("one")			
			ct.map << [e:3.1f] 			
			ct.update(flush:true)  
			ct.discard()
			ct = CollectionTypes.get(ct.id)			
		then:			
			"2" == ct.string 
			3 == ct.i 					
			list == ct.list
			set == ct.set
			map == ct.map
	}
	
	void "Test update collections"() {
		given:
			def list = [1,2,3]
			def set = ["one", "two", "three"] as Set
			def map = [a:1, b:2.5f, z:3l]
			def ct = new CollectionTypes(string: "1", i:2, list: list, map: map, set:set)
			ct.save(flush:true)
			ct.discard()
			ct = CollectionTypes.get(ct.id)
			
		when:
			def list2 = [4,5,6]			
			CollectionTypes.updateProperty(ct.id, "list", list2, [flush:true])
			ct.discard()
			ct = CollectionTypes.get(ct.id)
			
		then:
			"1" == ct.string 
			2 == ct.i
			list2 == ct.list
			set == ct.set
			map == ct.map
		
		when:
    		def set2 = ["four", "five", "six"] as Set
    		def map2 = [e:2, f:5.5f, g:5l]
			CollectionTypes.updateProperty(ct.id, "set", set2)
			CollectionTypes.updateProperty(ct.id, "map", map2, [flush:true])
			ct.discard()
			ct = CollectionTypes.get(ct.id)
		
		then:
			set2 == ct.set
			map2 == ct.map
	}
	
	void "Test update properties"() {
		given:
    		def list = [1,2,3]
    		def set = ["one", "two", "three"] as Set
    		def map = [a:1, b:2.5f, z:3l]
    		def ct = new CollectionTypes(string: "1", i:2, list: list, map: map, set:set)
    		ct.save(flush:true)
    		ct.discard()
    		ct = CollectionTypes.get(ct.id)
    		
    	when:
    		def list2 = [4,5,6]
			def map2 = [e:2, f:5.5f, g:5l]
    		CollectionTypes.updateProperties(ct.id, [string: "2", list: list2, map: map2], [flush:true])
    		ct.discard()
    		ct = CollectionTypes.get(ct.id)
    		
    	then:
    		"2" == ct.string
    		2 == ct.i
    		list2 == ct.list    		
    		map2 == ct.map
	}
	
	void "Test collection append methods"() {
		given:
    		def list = [1,2,3]
    		def set = ["one", "two", "three"] as Set
    		def map = [a:1, b:2.5f, z:3l]
    		def ct = new CollectionTypes(string: "1", i:2, list: list, map: map, set:set)
			ct.save(flush:true)
			ct.discard()
			
    	when:
    		CollectionTypes.appendToList(ct.id, 4)  
			CollectionTypes.appendToSet(ct.id, "four")
			CollectionTypes.appendToMap(ct.id, [e:5.5f], [flush:true])
    		ct = CollectionTypes.get(ct.id)
			
    	then:
    		ct
    		"1" == ct.string 
    		2 == ct.i  
    		[1,2,3,4] == ct.list 
    		["one", "two", "three", "four"] as Set == ct.set 
    		[a:1, b:2.5f, e:5.5f, z:3l] == ct.map 
		
		when:
			CollectionTypes.appendToList(ct.id, [5,6])
			CollectionTypes.appendToSet(ct.id, ["five", "six"] as Set)
			CollectionTypes.appendToMap(ct.id, [f:10l, g: 11], [flush:true])
			ct.discard()
			ct = CollectionTypes.get(ct.id)
			
		then:
			ct
			"1" == ct.string 
			2 == ct.i 
			[1,2,3,4,5,6] == ct.list 			
			["one", "two", "three", "four", "five", "six"] as Set == ct.set 
			[a:1, b:2.5f, e:5.5f, f:10l, g:11, z:3l] == ct.map 					
	}
	
	void "Test collection prepend methods"() {
		given:
			def list = [1,2,3]
			def set = ["one", "two", "three"] as Set
			def map = [a:1, b:2.5f, z:3l]
			def ct = new CollectionTypes(string: "1", i:2, list: list, map: map, set:set)
			ct.save(flush:true)
			ct.discard()
			
		when:
			CollectionTypes.prependToList(ct.id, 4, [flush:true])			
			ct = CollectionTypes.get(ct.id)
			
		then:
			ct
			"1" == ct.string 
			2 == ct.i 
			[4,1,2,3] == ct.list 
			set == ct.set 
			map == ct.map 
		
		when:
			CollectionTypes.prependToList(ct.id, [5,0], [flush:true])			
			ct.discard()
			ct = CollectionTypes.get(ct.id)
			
		then:
			ct
			"1" == ct.string 
			2 == ct.i  
			[0,5,4,1,2,3] == ct.list 
			set == ct.set 
			map == ct.map 
		
		when: 
			CollectionTypes.prependToSet(ct.id, "zero", [flush:true])
		
		then:
			def e = thrown(MissingMethodException)
			e.message.startsWith("No signature of method: grails.gorm.tests.CollectionTypes.prependToSet() is applicable for argument types: (java.util.UUID, java.lang.String, java.util.LinkedHashMap)")
			
		when:
			CollectionTypes.prependToMap(ct.id, [e:5.5f], [flush:true])
		
		then:
			e = thrown(MissingMethodException)
			e.message.startsWith("No signature of method: grails.gorm.tests.CollectionTypes.prependToMap() is applicable for argument types: (java.util.UUID, java.util.LinkedHashMap, java.util.LinkedHashMap)")
	}
	
	void "Test collection replaceAt method"() {
		given:
			def list = [1,2,3]
			def set = ["one", "two", "three"] as Set
			def map = [a:1, b:2.5f, z:3l]
			def ct = new CollectionTypes(string: "1", i:2, list: list, map: map, set:set)
			ct.save(flush:true)
			ct.discard()
			
		when:
			CollectionTypes.replaceAtInList(ct.id, 4, 1, [flush:true])
			ct = CollectionTypes.get(ct.id)
			
		then:
			ct
			"1" == ct.string
			2 == ct.i
			[1,4,3] == ct.list 
			set == ct.set 
			map == ct.map 
		
		when:
			CollectionTypes.replaceAtInSet(ct.id, "zero", 1, [flush:true])
		
		then:
			def e = thrown(MissingMethodException)
			e.message.startsWith("No signature of method: grails.gorm.tests.CollectionTypes.replaceAtInSet() is applicable for argument types: (java.util.UUID, java.lang.String, java.lang.Integer, java.util.LinkedHashMap)")
			
		when:
			CollectionTypes.replaceAtInMap(ct.id, [e:5.5f], 1, [flush:true])
		
		then:
			e = thrown(MissingMethodException)
			e.message.startsWith("No signature of method: grails.gorm.tests.CollectionTypes.replaceAtInMap() is applicable for argument types: (java.util.UUID, java.util.LinkedHashMap, java.lang.Integer, java.util.LinkedHashMap)")
	}
	
	void "Test collection deleteFrom methods"() {
		given:
			def list = [1,2,3]
			def set = ["one", "two", "three", "four"] as Set
			def map = [a:1, b:2.5f, z:3l]
			def ct = new CollectionTypes(string: "1", i:2, list: list, map: map, set:set)
			ct.save(flush:true)
			ct.discard()
			
		when:
			CollectionTypes.deleteFromList(ct.id, 1)
			CollectionTypes.deleteFromSet(ct.id, "two", [flush:true])			
			ct = CollectionTypes.get(ct.id)
			
		then:
			ct
			"1" == ct.string
			2 == ct.i
			[2,3] == ct.list 
			["one", "three", "four"] as Set == ct.set 
			map == ct.map 
		
		when:
			CollectionTypes.deleteFromList(ct.id, [2,3])
			CollectionTypes.deleteFromSet(ct.id, ["three", "four"] as Set, [flush:true])			
			ct.discard()
			ct = CollectionTypes.get(ct.id)
			
		then:
			ct
			"1" == ct.string
			2 == ct.i
			null == ct.list
			["one"] as Set == ct.set 
			map == ct.map 
		
		when:
			CollectionTypes.deleteFromMap(ct.id, [a:1], [flush:true])
		
		then:
			def e = thrown(MissingMethodException)
			e.message.startsWith("No signature of method: grails.gorm.tests.CollectionTypes.prependToMap() is applicable for argument types: (java.util.UUID, java.util.LinkedHashMap, java.util.LinkedHashMap)")
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