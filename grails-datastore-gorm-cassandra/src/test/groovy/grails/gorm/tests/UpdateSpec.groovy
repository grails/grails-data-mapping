package grails.gorm.tests


import org.grails.datastore.mapping.core.Session

class UpdateSpec extends GormDatastoreSpec {
    
	void "Test update()"() {
		given:
			def t = new TestEntity(name:"Bob")
			def p = new PersonLastNamePartitionKey(firstName: "Bob", lastName: "Wilson", age: 25)				
			t.save()
			p.save(flush:true)			
			t = TestEntity.get(t.id)			
			p = PersonLastNamePartitionKey.get([firstName:p.firstName, lastName: p.lastName, age: 25])		
		
		when: 
			t.name = "Jim"
			t.update()
			p.location = "UK"
			p.update(flush:true)						
			def t2 = TestEntity.get(t.id)			
			def p2 = PersonLastNamePartitionKey.get([firstName:p.firstName, lastName: p.lastName, age: 25])
		
		then:
			t == t2
			p == p2
	}
	
	void "Test update() entity not in session"() {
		given:
			def t = new TestEntity(name:"Bob")
			def p = new PersonLastNamePartitionKey(firstName: "Bob", lastName: "Wilson", age: 25)
			t.save()
			p.save(flush:true)
			session.clear()
		
		when:
			t.name = "Jim"
			t.age = 31
			t.update()
			p.location = "UK"
			p.update(flush:true)
			def t2 = TestEntity.get(t.id)
			def p2 = PersonLastNamePartitionKey.get([firstName:p.firstName, lastName: p.lastName, age: 25])
		
		then:			
			t2 != null
			t.is(t2)	
			p2 != null
			p.is(p2)
			TestEntity.get(UUID.randomUUID()) == null
			PersonLastNamePartitionKey.get([firstName:p.firstName, lastName: p.lastName, age: 26]) == null
			
		when:
			session.clear()
			def t3 = TestEntity.get(t.id)
			def p3 = PersonLastNamePartitionKey.get([firstName:p.firstName, lastName: p.lastName, age: 25])
		
		then:
			t3
			!t.is(t3)
            t.id == t3.id
            "Jim" == t3.name  
			31 == t3.age 
			1 == t3.version                     
			
            p3
			!p.is(p3)
            "Bob" == p3.firstName
            "Wilson" == p3.lastName
            25 == p3.age
			"UK" == p3.location            
	}
	
	void "Test updateProperties"() {
		given:
			def t = new TestEntity(name:"Bob", age: 25)			
			def p = new PersonLastNamePartitionKey(firstName: "Bob", lastName: "Wilson", age: 25, location: "UK")
			def p2 = new PersonLastNamePartitionKey(firstName: "Jon", lastName: "Ryan", age: 30)	
			t.save(failOnError: true)
			p.save(failOnError: true)
    		p2.save(failOnError: true, flush:true)
			session.clear()
		when:			
			TestEntity.updateProperties(t.id, [name: "Jon", age: 20])
			PersonLastNamePartitionKey.updateProperties([firstName:p.firstName, lastName: p.lastName, age: p.age], [location: "London"], [flush:true])
			session.clear()
			t = TestEntity.get(t.id)			
			p = PersonLastNamePartitionKey.get([firstName:p.firstName, lastName: p.lastName, age: p.age])
			p2 = PersonLastNamePartitionKey.get([firstName:p2.firstName, lastName: p2.lastName, age: p2.age])
		then:
			t != null
			t.id != null
			"Jon" == t.name
			20 == t.age
			
			p != null
			"Bob" == p.firstName
			"Wilson" == p.lastName
			25 == p.age
			"London" == p.location		
			
			p2 != null
			"Jon" == p2.firstName
			"Ryan" == p2.lastName
			30 == p2.age
			null == p2.location
			
		
		when: "update non existent property"
			TestEntity.updateProperties(t.id, [unknown: "Jon", age: 20], [flush:true])
		
		then:
			def e = thrown(MissingPropertyException)			
			e.message == "No such property: unknown for class: grails.gorm.tests.TestEntity"
		
		when: "update for non existent primary key"
			PersonLastNamePartitionKey.updateProperties([unknown:p.firstName, lastName: p.lastName, age: p.age], [location: "London"], [flush:true])
		
		then:
    		e = thrown(MissingPropertyException)    		
    		e.message == "No such primary key property: unknown for entity class: grails.gorm.tests.PersonLastNamePartitionKey"
	}		
	
	void "Test update property"() {
		given:
    		EnumThingEnumPartitionKey t = new EnumThingEnumPartitionKey(en: TestEnum.V1, name: 'e1', enumValue: TestEnum.V2 )
    		EnumThing p = new EnumThing(name: 'e2', en: TestEnum.V2)        
    		t.save(failOnError: true, flush:true)
    		p.save(failOnError: true, flush:true)
    		session.clear()
		
		when:
			EnumThingEnumPartitionKey.updateProperty([en: t.en, name: t.name], "enumValue", TestEnum.V3)
			EnumThing.updateProperty(p.id, "en", TestEnum.V3, [flush:true])
			session.clear()
			t = t.get([en:t.en])
			p = p.get(p.id)
			
		then:
    		t != null
    		!t.hasErrors()
    		
    		p != null
    		!p.hasErrors()
			
			TestEnum.V1 == t.en 
			'e1' == t.name 
			TestEnum.V3 == t.enumValue 
						
			'e2' == p.name
			TestEnum.V3 == p.en 		
	}
	
}
