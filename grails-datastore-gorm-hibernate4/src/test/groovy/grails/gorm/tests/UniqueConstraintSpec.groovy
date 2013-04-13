package grails.gorm.tests

/**
 * Tests the unique constraint
 */
class UniqueConstraintSpec extends GormDatastoreSpec {

    void "Test simple unique constraint"() {
        when:"Two domain classes with the same name are saved"
            def one = new UniqueGroup(name:"foo").save(flush:true)
            def two = new UniqueGroup(name:"foo")
            two.save(flush:true)

        then:"The second has errors"
            one != null
            two.hasErrors()
            UniqueGroup.count() == 1

        when:"The first is saved again"
            one = one.save(flush:true)

        then:"The are no errors"
            one != null

        when:"Three domain classes are saved within different uniqueness groups"
            one = new GroupWithin(name:"foo", org:"mycompany").save(flush:true)
            two = new GroupWithin(name:"foo", org:"othercompany").save(flush:true)
            def three = new GroupWithin(name:"foo", org:"mycompany")
            three.save(flush:true)

        then:"Only the third has errors"
            one != null
            two != null
            three.hasErrors()
            GroupWithin.count() == 2
    }

    @Override
    List getDomainClasses() {
        [UniqueGroup, GroupWithin]
    }
}
