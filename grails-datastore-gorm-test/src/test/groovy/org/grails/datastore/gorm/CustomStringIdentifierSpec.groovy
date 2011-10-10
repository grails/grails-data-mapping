package org.grails.datastore.gorm

import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 10/10/11
 * Time: 10:07 AM
 * To change this template use File | Settings | File Templates.
 */
class CustomStringIdentifierSpec extends GormDatastoreSpec {

    static {
        TEST_CLASSES << Product
    }
    void "test basic crud operations with string id"() {
        when: "A product is saved with an assigned id"
            createProducts()
            def p = Product.get("MacBook")

        then:"The product is not null"
            p != null

        when:"A product is retrieved by id"
            session.clear()
            p = Product.get("MacBook")

        then:"The product is not null"
            p != null
    }

    void "Test dynamic finders with string id"() {
        when: "A product with a string id is query via a dynamic finder"
            createProducts()
            def p = Product.findByName("MacBook")

        then:"The product is not null"
            p != null

    }

    protected def createProducts() {
        new Product(name: "MacBook").save()
        new Product(name: "iPhone").save()
        new Product(name: "iMac").save(flush: true)

    }
}

@Entity
class Product {
    String name
    Date dateCreated

    static mapping = {
        id generator:'assigned', name:"name"
    }
}
