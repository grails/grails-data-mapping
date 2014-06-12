package grails.gorm.tests

import spock.lang.Issue

class DeleteSpec extends GormDatastoreSpec {

    @Issue("GRAILS-9922")
    def "Test deleting an entity that has a validation error"() {
        when:
            def entity = new TestEntity(name: "Bob", age: 44).save(flush: true)

        then:
            !entity.hasErrors()
            TestEntity.count() == 1

        when:
            entity.name = ""
            entity.save(flush: true)

        then:
            entity.hasErrors()

        when:
            entity.delete(flush: true)

        then:
            TestEntity.count() == 0
    }

}
