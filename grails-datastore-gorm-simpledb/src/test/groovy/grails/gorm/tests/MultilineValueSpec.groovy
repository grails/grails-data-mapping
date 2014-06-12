package grails.gorm.tests

/**
 * Tests whether values with line breaks are stored and retrieved correctly.
 */
class MultilineValueSpec extends GormDatastoreSpec {
    void "Test multivalue with slash n"() {
        given:
            def multiline = "Bob\nThe coder\nBuilt decoder"
            def entity = new Book(title: multiline, author: "Mark", published: true).save(flush:true)

        when:
            def result = Book.get(entity.id)

        then:
            result != null
            result.title == multiline
    }
    void "Test multivalue with slash r slash n"() {
        given:
            def multiline = "Bob\r\nThe coder\r\nBuilt decoder"
            def entity = new Book(title: multiline, author: "Mark", published: true).save(flush:true)

        when:
            def result = Book.get(entity.id)

        then:
            result != null
            result.title == multiline
    }
}