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
			ct.updateSingleTypes(flush:true)  
			ct.discard()
			def ct2 = CollectionTypes.get(ct.id)			
		then:		
			!ct.is(ct2)	
			"2" == ct2.string 
			3 == ct2.i 					
			list == ct2.list
			set == ct2.set
			map == ct2.map
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
		when: "test instance collections created"
			def ct = new CollectionTypes(string: "1", i:2)
			ct.save(flush:true)
			ct.discard()
			ct.appendToList(1)
			ct.appendToSet("one")
			ct.appendToMap([a:1], [flush:true])
			ct.discard()
			
		then:
			[1] == ct.list
			["one"] as Set == ct.set
			[a:1] == ct.map	
			
    	when: "test instance collections updated"
			ct = new CollectionTypes(string: "1", i:2, list: [1,2,3], map: [a:1, b:2.5f, z:3l], set:["one", "two", "three"] as Set)
			ct.save(flush:true)
			ct.discard()
    		ct.appendToList(4)  
			ct.appendToSet("four")
			ct.appendToMap([e:5.5f], [flush:true])    
			ct.discard()
			
    	then:    		
			def list = [1,2,3,4]		
			def set = ["one", "two", "three", "four"] as Set
			def map = [a:1, b:2.5f, e:5.5f, z:3l]
    		
    		list == ct.list 
			set == ct.set
    		map == ct.map 
    	
		when: "test datastore updated"
			ct = CollectionTypes.get(ct.id)
			
		then:
			ct
			"1" == ct.string
			2 == ct.i
			list == ct.list
			set == ct.set
			map == ct.map
			ct.is(CollectionTypes.get(ct.id))
			
		when: "test static methods"
			CollectionTypes.appendToList([id: ct.id], [5,6])
			ct.appendToList([7,8])
			list += [5,6,7,8]
			CollectionTypes.appendToSet([id: ct.id], ["five", "six"] as Set)
			set += ["five", "six"]
			CollectionTypes.appendToMap([id: ct.id], [f:10l, g: 11], [flush:true])
			map << [f:10l, g: 11]
			ct.discard()
			ct = CollectionTypes.get(ct.id)			
			
		then:						
			list == ct.list 			
			set == ct.set 
			map == ct.map 		
		
		when: "test queried instance updated"
			ct.discard()
			ct = CollectionTypes.findById(ct.id)
			ct.appendToList(9)
			list << 9
			ct.appendToSet("seven")
			set << "seven"
			ct.appendToMap([j:5], [flush:true])
			map << [j:5]
		
		then:
			list == ct.list
			set == ct.set
			map == ct.map
	}
	
	void "Test collection prepend methods"() {
		when: "test instance collections created"
			def ct = new CollectionTypes(string: "1", i:2)
			ct.save(flush:true)
			ct.discard()
			ct.prependToList(1, [flush:true])	
		
		then:
			[1] == ct.list		
		
		when: "test instance collections updated"
			def list = [1,2,3]
			def set = ["one", "two", "three"] as Set
			def map = [a:1, b:2.5f, z:3l]
			ct = new CollectionTypes(string: "1", i:2, list: list, map: map, set:set)
			ct.save(flush:true)
			ct.discard()				
			ct.prependToList(4, [flush:true])			
			ct.discard()
		
		then:
			def list2 = [4,1,2,3]		
			list2 == ct.list
		
		when: "test datastore updated"
			ct = CollectionTypes.get(ct.id)
			
		then:
			ct
			"1" == ct.string 
			2 == ct.i 
			list2 == ct.list 
			set == ct.set 
			map == ct.map 
		
		when: "test static methods"
			ct.prependToList(9, [flush:true])
			CollectionTypes.prependToList(ct.id, 6, [flush:true])
			CollectionTypes.prependToList([id: ct.id], [5,0], [flush:true])
			ct.discard()
			ct = CollectionTypes.get(ct.id)
			
		then:			
			[0,5,6,9,4,1,2,3] == ct.list 
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
			
		when: "test instance collections updated"
			ct.replaceAtInList(1, 4, [flush:true])
			ct.discard()
			
		then:
			def list2 = [1,4,3]	
			list2 == ct.list
		
		when: "test datastore updated"
			ct = CollectionTypes.get(ct.id)
			
		then:
			ct
			"1" == ct.string
			2 == ct.i
			list2 == ct.list 
			set == ct.set 
			map == ct.map 
		
		when: "test static methods"
			CollectionTypes.replaceAtInList(ct.id, 2, 5, [flush:true])
			CollectionTypes.replaceAtInList([id: ct.id], 0, 3, [flush:true])
			session.clear()
			ct = CollectionTypes.get(ct.id)
		
		then:    		
    		[3,4,5] == ct.list
    		set == ct.set
    		map == ct.map
			
		when:
			CollectionTypes.replaceAtInSet(ct.id, 1, "zero", [flush:true])
		
		then:
			def e = thrown(MissingMethodException)
			e.message.startsWith("No signature of method: grails.gorm.tests.CollectionTypes.replaceAtInSet() is applicable for argument types: (java.util.UUID, java.lang.Integer, java.lang.String, java.util.LinkedHashMap)")
			
		when:
			ct.replaceAtInMap(1, [e:5.5f], [flush:true])
		
		then:
			e = thrown(MissingMethodException)
			e.message.startsWith("No signature of method: grails.gorm.tests.CollectionTypes.replaceAtInMap() is applicable for argument types: (java.lang.Integer, java.util.LinkedHashMap, java.util.LinkedHashMap)")
	}
	
	void "Test collection deleteFrom methods"() {
		given:
			def list = [1,2,3]
			def set = ["one", "two", "three", "four"] as Set
			def map = [a:1, b:2.5f, z:3l]
			def ct = new CollectionTypes(string: "1", i:2, list: list, map: map, set:set)
			ct.save(flush:true)
			ct.discard()
			
		when: "test deleted from instace collections"
			ct.deleteFromList(1)
			ct.deleteFromSet("two")	
			ct.deleteFromMap("b", [flush:true])		
			ct.discard()
			
		then:
			def list2 = [2,3] 
			def set2 = ["one", "three", "four"] as Set
			def map2 = [a:1, z:3l]
			list2 == ct.list
			set2 == ct.set
			map2 == ct.map
			
		when: "test deleted from datastore"
			ct = CollectionTypes.get(ct.id)
		
		then:		
			ct
			"1" == ct.string
			2 == ct.i
			list2 == ct.list 
			set2 == ct.set 
			map2 == ct.map 
		
		when: "test static methods"
			CollectionTypes.deleteFromList(ct.id, [2,3])
			CollectionTypes.deleteFromSet([id: ct.id], ["three", "four"] as Set)
			CollectionTypes.deleteFromMap([id: ct.id], "z", [flush:true])			
			ct.discard()
			ct = CollectionTypes.get(ct.id)
			
		then:
			ct			
			null == ct.list
			["one"] as Set == ct.set 
			[a:1] == ct.map 			
	}
	
	void "Test collection deleteAtFrom methods"() {
		given:
			def list = [1,2,3]
			def set = ["one", "two", "three", "four"] as Set
			def map = [a:1, b:2.5f, z:3l]
			def ct = new CollectionTypes(string: "1", i:2, list: list, map: map, set:set)
			ct.save(flush:true)
			ct.discard()
			
		when: "test deleted from instace collection"
			ct.deleteAtFromList(1, [flush:true])
			ct.discard()
		
		then:
			def list2 = [1,3]
			list2 == ct.list
			set == ct.set
			map == ct.map
			
		when: "test deleted from datastore"
			ct = CollectionTypes.get(ct.id)
		
		then:
			ct
			"1" == ct.string
			2 == ct.i
			list2 == ct.list
			set == ct.set
			map == ct.map
		
		when: "test static methods"
			CollectionTypes.deleteAtFromList(ct.id, 1, [flush:true])			
			ct.discard()
			ct = CollectionTypes.get(ct.id)
			
		then:			
			[1] == ct.list
			set == ct.set
			map == ct.map
		
		when:
			CollectionTypes.deleteAtFromSet(ct.id, 1, [flush:true])
				
		then:
			def e = thrown(MissingMethodException)
			e.message.startsWith("No signature of method: grails.gorm.tests.CollectionTypes.deleteAtFromSet() is applicable for argument types: (java.util.UUID, java.lang.Integer, java.util.LinkedHashMap)")
		
		when:
			ct.deleteAtFromMap(1, [flush:true])
		
		then:
			e = thrown(MissingMethodException)
			e.message.startsWith("No signature of method: grails.gorm.tests.CollectionTypes.deleteAtFromMap() is applicable for argument types: (java.lang.Integer, java.util.LinkedHashMap)")
	}
			
}

@Entity
class CollectionTypes {

	String string
	int i
	List<Integer> list
	Set<String> set
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