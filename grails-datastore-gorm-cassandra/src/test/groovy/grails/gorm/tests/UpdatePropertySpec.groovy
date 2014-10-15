package grails.gorm.tests


import org.grails.datastore.mapping.core.Session

class UpdatePropertySpec extends GormDatastoreSpec {
    
	void "Test update properties"() {
		given:
			def t = new TestEntity(name:"Bob", age: 25)
			def p = new PersonLastNamePartitionKey(firstName: "Bob", lastName: "Wilson", age: 25, location: "UK")
			t.save(failOnError: true, flush:true)
    		p.save(failOnError: true, flush:true)
			session.clear()
		when:			
			TestEntity.updateProperties(t.id, [name: "Jon", age: 20])
			PersonLastNamePartitionKey.updateProperties([firstName:p.firstName, lastName: p.lastName, age: p.age], [location: "London"], [flush:true])
			t = TestEntity.get(t.id)			
			p = PersonLastNamePartitionKey.get([firstName:p.firstName, lastName: p.lastName])

		then:
			t != null
			t.id != null
			"Jon" == t.name
			t.age == 20
			
			p != null
			"Bob" == p.firstName
			"Wilson" == p.lastName
			25 == p.age
			"London" == p.location		
		
		when: "update non existent property"
			TestEntity.updateProperties(t.id, [unknown: "Jon", age: 20], [flush:true])
		
		then:
			def e = thrown(MissingPropertyException)			
			e.message == "No such property: unknown for class: grails.gorm.tests.TestEntity"
		
		when: "update for non existent primary key"
			PersonLastNamePartitionKey.updateProperty([unknown:p.firstName, lastName: p.lastName, age: p.age], "location", "London", [flush:true])
		
		then:
    		e = thrown(MissingPropertyException)    		
    		e.message == "No such primary key property: unknown for entity class: grails.gorm.tests.PersonLastNamePartitionKey"
	}		
	
	void "Test update enum property"() {
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
			
			t.en == TestEnum.V1
			t.name == 'e1'
			t.enumValue == TestEnum.V3
						
			p.name =='e2'
			p.en == TestEnum.V3		
	}
	
}
