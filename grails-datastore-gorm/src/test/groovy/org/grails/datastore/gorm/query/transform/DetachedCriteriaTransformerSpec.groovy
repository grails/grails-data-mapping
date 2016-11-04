package org.grails.datastore.gorm.query.transform

import grails.gorm.DetachedCriteria
import grails.gorm.annotation.Entity
import spock.lang.Specification

/**
 * Created by graemerocher on 04/11/16.
 */
class DetachedCriteriaTransformerSpec extends Specification {

    void "test transform query with embedded entity"() {
        when:"A query is parsed that queries the embedded entity"
        def gcl = new GroovyClassLoader()
        DetachedCriteria criteria = gcl.parseClass('''
import org.grails.datastore.gorm.query.transform.*

Vendor.where {
    address.zip =~ '%44%\'
}
''').newInstance().run()

        then:"The criteria contains the correct criterion"
        criteria.criteria[0].property == 'address.zip'
    }
}


@Entity
class Company {
    Address address

    static embedded = ['address']
    static constraints = {
        address nullable: true
    }
    static mapping = {
        tablePerSubclass  true
    }
}
@Entity
class Vendor extends Company {

    static constraints = {
    }
}
class Address {
    String address
    String city
    String state
    String zip
}
