package grails.gorm.tests

class FindOrCreateWhereSpec extends GormDatastoreSpec {
    
    def "Test findOrCreateWhere returns a new instance if it doesn't exist in the database"() {
        when:
            def entity = TestEntity.findOrCreateWhere(name: 'Fripp', age: 64, [allowFiltering:true])
			def person = PersonLastNamePartitionKey.findOrCreateWhere(firstName: 'Jake', lastName: 'Brown', age: 35)
        then:
			null == TestEntity.get(entity.id)
            'Fripp' == entity.name
            64 == entity.age
            null == entity.id			
			
			null == PersonLastNamePartitionKey.get([firstName: person.firstName, lastName: person.lastName, age: person.age])
			'Jake' == person.firstName
			'Brown' == person.lastName
			35 == person.age			
    }

    def "Test findOrCreateWhere returns a persistent instance if it exists in the database"() {
        given:
            def entityId = new TestEntity(name: 'Belew', age: 61).save().id
			def person = new PersonLastNamePartitionKey(firstName: 'Jake', lastName: 'Brown', age: 35).save(flush:true)
			session.clear()			
        when:
            def entity = TestEntity.findOrCreateWhere(name: 'Belew', age: 61, [allowFiltering:true])
			def person2 = PersonLastNamePartitionKey.findOrCreateWhere(firstName: person.firstName, lastName: person.lastName, age: person.age)
        then:
            entity.id != null
            entityId == entity.id
            'Belew' == entity.name
            61 == entity.age
			
			'Jake' == person2.firstName
			'Brown' == person2.lastName
			35 == person2.age
    }
}
