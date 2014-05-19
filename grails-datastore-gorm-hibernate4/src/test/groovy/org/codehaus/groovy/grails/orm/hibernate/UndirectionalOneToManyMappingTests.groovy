package org.codehaus.groovy.grails.orm.hibernate

import static junit.framework.Assert.*
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Oct 29, 2008
 */
class UndirectionalOneToManyMappingTests extends AbstractGrailsHibernateTests {

    @Override
    protected getDomainClasses() {
        [MappedU2mAuthor, MappedU2mBook]
    }

    @Test
    void testUnidirectionalOneToManyMapping() {
        def a = MappedU2mAuthor.newInstance(name:"Stephen King")

        a.addToBooks(MappedU2mBook.newInstance(title:"The Shining"))
         .addToBooks(MappedU2mBook.newInstance(title:"The Stand"))
         .save(true)
        assertEquals 2, MappedU2mBook.list().size()

        // now let's test the database state
        def c = session.connection()

        def ps = c.prepareStatement("select * from um_author_books")
        def rs = ps.executeQuery()
        assert rs.next()

        assertEquals 1,rs.getInt("um_author_id")
        assertEquals 1,rs.getInt("um_book_id")

        assert rs.next()
        assertEquals 1,rs.getInt("um_author_id")
        assertEquals 2,rs.getInt("um_book_id")
    }

}

class MappedU2mBook {
    Long id
    Long version

    String title
}

class MappedU2mAuthor {
    Long id
    Long version
    String name
    Set books
    static hasMany = [books:MappedU2mBook]

    static mapping = {
        books joinTable:[name:"um_author_books", key:'um_author_id', column:'um_book_id']
    }
}

