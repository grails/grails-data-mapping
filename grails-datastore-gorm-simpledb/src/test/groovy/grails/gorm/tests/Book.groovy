package grails.gorm.tests

import grails.persistence.Entity

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
@Entity
class Book implements Serializable {
    String id
    String author
    String title
    Boolean published = false

    static mapping = {
        domain 'Book'
    }
}
