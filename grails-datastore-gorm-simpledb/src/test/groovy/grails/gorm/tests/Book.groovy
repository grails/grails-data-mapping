package grails.gorm.tests


/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
class Book {
    String id
    String author
    String title
    Boolean published = false

    static mapping = {
    }
}
