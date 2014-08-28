package grails.gorm.tests

class FindOrSaveWhereSpec extends GormDatastoreSpec {

    def "Test findOrSaveWhere returns a new instance if it doesn't exist in the database"() {
        when:
            def entity = TestEntity.findOrSaveWhere(name: 'Lake', age: 63, [allowFiltering:true])
            def person = Person.findOrSaveWhere(firstName: 'Jake', lastName: 'Brown', age: 11, [allowFiltering:true])
        then:
            'Lake' == entity.name
            63 == entity.age
            null != entity.id
            
            'Jake' == person.firstName
            'Brown' == person.lastName
            11 == person.age
    }

    def "Test findOrSaveWhere returns a persistent instance if it exists in the database"() {
        given:
            def entityId = new TestEntity(name: 'Levin', age: 64).save().id

        when:
            def entity = TestEntity.findOrSaveWhere(name: 'Levin', age: 64, [allowFiltering:true])

        then:
            entity.id != null
            entityId == entity.id
            'Levin' == entity.name
            64 == entity.age
    }
}
