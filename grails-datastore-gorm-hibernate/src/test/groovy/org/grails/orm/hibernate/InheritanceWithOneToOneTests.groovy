package org.grails.orm.hibernate


import static junit.framework.Assert.*
import grails.persistence.Entity
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Nov 15, 2007
 */
class InheritanceWithOneToOneTests extends AbstractGrailsHibernateTests {

    @Test
    void testOneToOneInSuperClass() {

        def book = InheritanceWithOneToOneBook.newInstance(name: "King Series", title: "The Stand")

        book.detailPicture = InheritanceWithOneToOneAttachment.newInstance(filepath: "/path/to/det")
        book.previewPicture = InheritanceWithOneToOneAttachment.newInstance(filepath: "/path/to/prev")
        book.detailPicture.attachment2 = InheritanceWithOneToOneAttachment2.newInstance(filepath: "/path/to/det")
        book.previewPicture.attachment2 = InheritanceWithOneToOneAttachment2.newInstance(filepath: "/path/to/det")
        assertNotNull book.save()
        session.flush()
        session.clear()

        book = InheritanceWithOneToOneBook.get(1)

        assertNotNull book
        assertNotNull book.detailPicture
        assertNotNull book.previewPicture

        assertEquals "King Series", book.name
        assertEquals "The Stand", book.title

        assertEquals "/path/to/det", book.detailPicture.filepath
        assertEquals "/path/to/prev", book.previewPicture.filepath
    }

    @Override
    protected getDomainClasses() {
        [InheritanceWithOneToOneProduct, InheritanceWithOneToOneAttachment, InheritanceWithOneToOneBook, InheritanceWithOneToOneAttachment2]
    }
}
@Entity
class InheritanceWithOneToOneProduct{
    Long id
    Long version
    String name
    InheritanceWithOneToOneAttachment detailPicture
    InheritanceWithOneToOneAttachment previewPicture
}

@Entity
class InheritanceWithOneToOneBook extends InheritanceWithOneToOneProduct {
    Long id
    Long version
    String title
}

@Entity
class InheritanceWithOneToOneAttachment {
    Long id
    Long version
    static belongsTo = [InheritanceWithOneToOneProduct]
    String filepath
    InheritanceWithOneToOneAttachment2 attachment2
}

@Entity
class InheritanceWithOneToOneAttachment2 {
    Long id
    Long version
    String filepath

    static belongsTo = InheritanceWithOneToOneAttachment
}
