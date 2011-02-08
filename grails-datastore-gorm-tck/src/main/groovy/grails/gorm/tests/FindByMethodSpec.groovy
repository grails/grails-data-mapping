package grails.gorm.tests

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Sep 2, 2010
 * Time: 11:50:39 AM
 * To change this template use File | Settings | File Templates.
 */
class FindByMethodSpec extends GormDatastoreSpec{
  
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

//      when:
//        book = Book.findPublishedByTitleOrAuthor('Fly Fishing For Everyone', 'Dierk')
//      then:
//        'GINA'== book.title
//          Book.findPublished() != null

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

//      when:
//        books = Book.findAllPublishedByAuthorOrTitle('Graeme', 'GINA')
//      then:
//
//        2 == books?.size()

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
class Highway implements Serializable{
    Long id
	Long version
    Boolean bypassed
    String name

    static mapping = {
      bypassed index:true
      name index:true
    }
}
class Book implements Serializable{
    Long id
	Long version
    String author
    String title
    Boolean published = false

    static mapping = {
      published index:true
      title index:true
      author index:true
    }
}