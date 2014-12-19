package org.grails.orm.hibernate

import grails.persistence.Entity

/**
 */
class CascadeValidationForEmbeddedSpec extends GormSpec{

    void "Test that validation cascades to embedded entities"() {
        when:"An entity with an invalid embedded entity is created"
            def company = new CascadeValidationForEmbeddedCompany()
            company.address = new CascadeValidationForEmbeddedCompanyAddress()

        then:"The entity is invalid"
            company.validate() == false

        when:"The embedded entity is made valid"
            company.address.country = "Spain"

        then:"The root entity validates"
            company.validate() == true
    }
    @Override
    List getDomainClasses() {
        [CascadeValidationForEmbeddedCompany, CascadeValidationForEmbeddedCompanyAddress]
    }
}

@Entity
class CascadeValidationForEmbeddedCompany {
    Long id
    Long version

    CascadeValidationForEmbeddedCompanyAddress address

    static embedded = ['address']

    static constraints = {
        address(nullable:false)
    }
}

@Entity
class CascadeValidationForEmbeddedCompanyAddress {
    Long id
    Long version

    String country

    static constraints = {
        country(blank:false)
    }
}