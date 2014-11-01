package grails.gorm.tests

class FindOrSaveWhereSpec extends GormDatastoreSpec {

    def "Test findOrSaveWhere returns a new instance if it doesn't exist in the database"() {
        when:
            def entity = TestEntity.findOrSaveWhere(name: 'Lake', age: 63, [allowFiltering:true, flush:true])
            def person = PersonLastNamePartitionKey.findOrSaveWhere(firstName: 'Jake', lastName: 'Brown', age: 35)
			session.flush()
			session.clear()
			entity = TestEntity.get(entity.id)
			person = PersonLastNamePartitionKey.get([firstName: person.firstName, lastName: person.lastName, age: person.age])
        then:
            'Lake' == entity.name
            63 == entity.age
            null != entity.id
            
            'Jake' == person.firstName
            'Brown' == person.lastName
            35 == person.age
    }

    def "Test findOrSaveWhere returns a persistent instance if it exists in the database"() {
        given:
            def entityId = new TestEntity(name: 'Levin', age: 64).save().id
			def person = new PersonLastNamePartitionKey(firstName: 'Jake', lastName: 'Brown', age: 35).save(flush:true)
			session.clear()
        when:
            def entity = TestEntity.findOrSaveWhere(name: 'Levin', age: 64, [allowFiltering:true, flush:true])
			def person2 = PersonLastNamePartitionKey.findOrSaveWhere(firstName: person.firstName, lastName: person.lastName, age: person.age)
        then:
			entity.isAttached()
            entity.id != null
            entityId == entity.id
            'Levin' == entity.name
            64 == entity.age
			
			person2.isAttached()
			'Jake' == person2.firstName
			'Brown' == person2.lastName
			35 == person2.age
    }
}
