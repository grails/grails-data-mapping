package grails.gorm.tests

/**
 * @author graemerocher
 */
class FindOrCreateWhereSpec extends GormDatastoreSpec {

    def "Test findOrCreateWhere returns a new instance if it doesn't exist in the database"() {

        when:
            def entity = TestEntity.findOrCreateWhere(name: 'Fripp', age: 64)

        then:
            'Fripp' == entity.name
            64 == entity.age
            null == entity.id
    }
}
