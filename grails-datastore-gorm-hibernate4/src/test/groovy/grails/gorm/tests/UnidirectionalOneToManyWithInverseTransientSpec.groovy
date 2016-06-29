package grails.gorm.tests

import grails.persistence.Entity
import org.grails.orm.hibernate.GormSpec
import spock.lang.Issue

/**
 * Created by graemerocher on 13/05/16.
 */
class UnidirectionalOneToManyWithInverseTransientSpec extends GormSpec{

    @Issue('https://github.com/grails/grails-data-mapping/issues/708')
    void "Test that a bidirectional association is not created when the inverse property is transient"() {
        when:"An author is saved with a book"
        new UO2MAuthor(name: "Stephen King")
            .addToBooks(new UO2MBook(name:"The Stand"))
            .save(flush:true)

        UO2MAuthor author = UO2MAuthor.first()

        then:"The books are correct"
        author.books.size() == 1
    }
    @Override
    List getDomainClasses() {
        [UO2MAuthor, UO2MBook]
    }
}

@Entity
class UO2MAuthor {

    static hasMany = [books: UO2MBook]
    String name
    static constraints = {
    }

    static mapping = {
        books joinTable:[name:"book_authors", key:'author_id' ]
    }
}

@Entity
class UO2MBook {
    static transients = ['primaryAuthor']
    static belongsTo = UO2MAuthor
    static hasMany = [authors: UO2MAuthor]
    String name

    static constraints = {
    }

    static mapping = {
        authors joinTable:[name:"book_authors", key:'book_id' ]
    }

    public UO2MAuthor getPrimaryAuthor() {
        def rtn
        if(rtn == null && this.authors.size() > 0) {
            rtn = this.authors.first()
        }
        return rtn
    }

    public void setPrimaryAuthor(UO2MAuthor author) {
        if(!author.id) {
            return
        }
        if(!this.authors || !this.authors.contains(author)) {
            this.addToAuthors(author)
        }
    }

}
