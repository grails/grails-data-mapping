package grails.gorm.tests

class InListSpec extends GormDatastoreSpec {

    void "test in list returns the correct results for empty argument"() {
        when:
        new TestEntity(name:"Fred").save()
        new TestEntity(name:"Bob").save()
        new TestEntity(name:"Jack").save(flush:true)

        then:
        TestEntity.countByNameInList(['Fred', "Bob"]) == 2
        TestEntity.countByNameInList([]) == 0
        TestEntity.countByNameInList(null) == 0

    }
}
