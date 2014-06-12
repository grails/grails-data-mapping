package grails.gorm.tests

/**
 * Override from GORM TCK to test String based Id for FindByMethodSpec
 *
 * @author Erawat Chamanont
 * @since 1.0
 */
class FindByMethodSpec extends GormDatastoreSpec{

    def cleanup() {
        def nativeSession  = session.nativeInterface
        def wp = nativeSession.getWorkspace();
        def qm = wp.getQueryManager();

        def q = qm.createQuery("//Highway", javax.jcr.query.Query.XPATH);
        def qr = q.execute()
        def itr = qr.getNodes();
        itr.each { it.remove() }

        q = qm.createQuery("//Book", javax.jcr.query.Query.XPATH);
        qr = q.execute()
        itr = qr.getNodes();
        itr.each { it.remove() }
        nativeSession.save()
    }

    void testBooleanPropertyQuery() {
        given:
            new Highway(bypassed: true, name: 'Bypassed Highway').save()
            new Highway(bypassed: true, name: 'Bypassed Highway').save()
            new Highway(bypassed: false, name: 'Not Bypassed Highway').save()
            new Highway(bypassed: false, name: 'Not Bypassed Highway').save()

        when:
            def highways= Highway.findAllBypassedByName('Not Bypassed Highway')

        then:
            0 == highways.size()

        when:
            highways = Highway.findAllNotBypassedByName('Not Bypassed Highway')

        then:
            2 == highways?.size()
            'Not Bypassed Highway' == highways[0].name
            'Not Bypassed Highway'== highways[1].name

        when:
            highways = Highway.findAllBypassedByName('Bypassed Highway')

        then:
            2 == highways?.size()
            'Bypassed Highway'== highways[0].name
            'Bypassed Highway'== highways[1].name

        when:
            highways = Highway.findAllNotBypassedByName('Bypassed Highway')
        then:
            assert 0 == highways?.size()

        when:
            highways = Highway.findAllBypassed()
        then:
            2 ==highways?.size()
            'Bypassed Highway'== highways[0].name
            'Bypassed Highway'==highways[1].name

        when:
            highways = Highway.findAllNotBypassed()
        then:
            2 == highways?.size()
            'Not Bypassed Highway' == highways[0].name
            'Not Bypassed Highway'== highways[1].name

        when:
            def highway = Highway.findNotBypassed()
        then:
            'Not Bypassed Highway' == highway?.name

        when:
            highway = Highway.findBypassed()
        then:
            'Bypassed Highway' == highway?.name

        when:
            highway = Highway.findNotBypassedByName('Not Bypassed Highway')
        then:
            'Not Bypassed Highway' == highway?.name

        when:
            highway = Highway.findBypassedByName('Bypassed Highway')
        then:
            'Bypassed Highway' == highway?.name

        when:
            Book.newInstance(author: 'Jeff', title: 'Fly Fishing For Everyone', published: false).save()
            Book.newInstance(author: 'Jeff', title: 'DGGv2', published: true).save()
            Book.newInstance(author: 'Graeme', title: 'DGGv2', published: true).save()
            Book.newInstance(author: 'Dierk', title: 'GINA', published: true).save()

            def book = Book.findPublishedByAuthor('Jeff')
        then:
            'Jeff' == book.author
            'DGGv2'== book.title

        when:
            book = Book.findPublishedByAuthor('Graeme')
        then:
            'Graeme' == book.author
            'DGGv2'==  book.title

        when:
            book = Book.findPublishedByTitleAndAuthor('DGGv2', 'Jeff')
        then:
            'Jeff'== book.author
            'DGGv2'== book.title

        when:
            book = Book.findNotPublishedByAuthor('Jeff')
        then:
            'Fly Fishing For Everyone'== book.title

//        when:
//            book = Book.findPublishedByTitleOrAuthor('Fly Fishing For Everyone', 'Dierk')
//        then:
//            'GINA'== book.title
//            Book.findPublished() != null

        when:
            book = Book.findNotPublished()
        then:
            'Fly Fishing For Everyone' == book?.title

        when:
            def books = Book.findAllPublishedByTitle('DGGv2')
        then:
            2 == books?.size()

        when:
            books = Book.findAllPublished()
        then:
            3 == books?.size()

        when:
            books = Book.findAllNotPublished()
        then:
            1 == books?.size()

        when:
            books = Book.findAllPublishedByTitleAndAuthor('DGGv2', 'Graeme')
        then:
            1 == books?.size()

//        when:
//            books = Book.findAllPublishedByAuthorOrTitle('Graeme', 'GINA')
//        then:
//            2 == books?.size()

        when:
            books = Book.findAllNotPublishedByAuthor('Jeff')
        then:
            1 == books?.size()

        when:
            books = Book.findAllNotPublishedByAuthor('Graeme')
        then:
            assert 0 == books?.size()
    }
}

class Highway implements Serializable {
    //Long id
    String id
    Boolean bypassed
    String name

    static mapping = {
        bypassed index:true
        name index:true
    }
}

class Book implements Serializable {
    //Long id
    String id
    String author
    String title
    Boolean published

    static mapping = {
        published index:true
        title index:true
        author index:true
    }
}
