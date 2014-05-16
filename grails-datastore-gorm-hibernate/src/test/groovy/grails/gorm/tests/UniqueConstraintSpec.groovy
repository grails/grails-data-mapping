package grails.gorm.tests

import grails.persistence.Entity
import org.junit.Ignore

/**
 * Tests the unique constraint
 */

/**
 *
 *  NOTE: This test is disabled because in order for the test suite to run quickly we need to run each test in a transaction.
 *  This makes it not possible to test the scenario outlined here, however tests for this use case exist in the hibernate plugin itself
 *  so we are covered.
 *
 */
@Ignore
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

    def "Test unique constraint with a hasOne association"() {
        when:"Two domain classes with the same license are saved"
            def license = new License()
            def one = new Driver(license: license).save(flush: true)
            def two = new Driver(license: license)
            two.save(flush: true)

        then:"The second has errors"
            one != null
            two.hasErrors()
            Driver.count() == 1
            License.count() == 1

        when:"The first is saved again"
            one = one.save(flush:true)

        then:"The are no errors"
            one != null
    }

    @Override
    List getDomainClasses() {
        [UniqueGroup, GroupWithin, Driver, License]
    }
}

@Entity
class Driver implements Serializable {
    Long id
    Long version
    License license
    static hasOne = [license: License]
    static constraints = {
        license unique: true
    }
}

@Entity
class License implements Serializable {
    Long id
    Long version
    Driver driver
}
