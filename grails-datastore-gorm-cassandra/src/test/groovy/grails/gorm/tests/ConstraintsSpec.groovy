package grails.gorm.tests


class ConstraintsSpec extends GormDatastoreSpec {

    void "Test constraints with static default values"() {
         given: "A Test class with static constraint values"
            def ce = new ConstrainedEntity(num:1000, str:"ABC")

         when: "saved is called"
            ce.save()

         then:
            ce.hasErrors() == false
    }

    @Override
    List getDomainClasses() {
        [ConstrainedEntity]
    }
}
