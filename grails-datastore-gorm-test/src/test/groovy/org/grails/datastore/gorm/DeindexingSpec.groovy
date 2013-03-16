package org.grails.datastore.gorm

import grails.gorm.tests.GormDatastoreSpec

/**
 * @author Daniel Wiell
 */
class DeindexingSpec extends GormDatastoreSpec {

    static {
        TEST_CLASSES << AuthorWithPseudonym
    }

    def 'Null is de-indexed'() {
        def author = new AuthorWithPseudonym(name: 'Samuel Clemens').save(failOnError: true)
        author.pseudonym = 'Mark Twain'
        author.save(failOnError: true)

        expect:
        !AuthorWithPseudonym.findByPseudonymIsNull()
    }
}

class AuthorWithPseudonym {
    Long id
    Integer version
    String name
    String pseudonym

    static constraints = {
        pseudonym nullable: true
    }
}