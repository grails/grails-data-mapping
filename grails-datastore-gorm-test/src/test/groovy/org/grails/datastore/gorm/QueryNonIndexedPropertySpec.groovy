package org.grails.datastore.gorm

import spock.lang.Specification
import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 7/26/11
 * Time: 9:48 AM
 * To change this template use File | Settings | File Templates.
 */
class QueryNonIndexedPropertySpec extends GormDatastoreSpec{

    static {
        GormDatastoreSpec.TEST_CLASSES << Company << CompanyAddress
    }


    def "Test that we can query a property that has no indices specified"() {
        given:"A valid set of persisted domain instances"
            def address = new CompanyAddress(postCode:"30483")
            def person = new Company(name:"Bob", address: address)
            person.save(flush:true)

        when: "An indexed property is queried"
            def found = Company.findByName("Bob")

        then: "A result is returned"
            found != null
            found.name == "Bob"

        when: "A non-indexed property is queried"
            found = Company.findByAddress(address)

        then: "A result is returned"
            found != null
            found.name == "Bob"
    }
}
@Entity
class Company {
    String name
    CompanyAddress address
}
@Entity
class CompanyAddress {
    String postCode
}


