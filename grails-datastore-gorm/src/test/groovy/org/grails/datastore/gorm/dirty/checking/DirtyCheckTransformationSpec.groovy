package org.grails.datastore.gorm.dirty.checking

import grails.gorm.dirty.checking.DirtyCheck
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Issue

/**
 * @author Graeme Rocher
 */
class DirtyCheckTransformationSpec extends Specification {

    void "Test dirty check with abstract inheritance"() {
        when:"A Dirty checkable class with generic types is parsed"
        def gcl = new GroovyClassLoader()
        Class cls = gcl.parseClass('''
package org.grails.datastore.gorm.dirty.checking

import grails.gorm.dirty.checking.DirtyCheck



@DirtyCheck
class ChildAuthor extends Author {
    int age
}

@DirtyCheck
abstract class Author {
    String name
}
''')

        def child = cls.newInstance()
        child.trackChanges()
        child.name = "Stephen King"
        then:"The generic types are retained"
        child.hasChanged()
        child.name == "Stephen King"
    }


    void "Test dirty check with generic types"() {
        when:"A Dirty checkable class with generic types is parsed"
            def gcl = new GroovyClassLoader()
            Class cls = gcl.parseClass('''
package org.grails.datastore.gorm.dirty.checking

import grails.gorm.dirty.checking.DirtyCheck

@DirtyCheck
class Author {
    Set<Book> books
}

@DirtyCheck
class ChildAuthor extends Author {
    int age
}

''')

        then:"The generic types are retained"
            cls.getMethod("getBooks").getReturnType().getGenericInterfaces()
    }

    @Issue('GRAILS-10433')
    void "Test that properties with single character getters generate the correct getter and setter combo"() {
        when:"A Dirty checkable class with generic types is parsed"
            def gcl = new GroovyClassLoader()
            Class cls = gcl.parseClass('''
class FundProduct {

   String gSeriesOptionCode

   static mapping = {
      gSeriesOptionCode column: 'XXX'
   }
}

    ''')

        then:"The generic types are retained"
            cls.getMethod("getgSeriesOptionCode")

        when:"An invalid getter is used"
            cls.getMethod('getGSeriesOptionCode')

        then:"an error is thrown"
            thrown(NoSuchMethodException)
    }

    void "Test that the dirty checking transformations allows you to track changes to a class"() {
        when:"A new dirty checking instance is created"
            def b = new Book()

        then:"It implements the DirtyCheckable interface"
            b instanceof DirtyCheckable
            b.hasChanged()
            b.hasChanged("title")

        when:"The title is changed"
            b.title = "My Title"

        then:"No tracking started yet so return true by default"
            b.hasChanged()
            b.hasChanged("title")

        when:"We start tracking and change the title"
            b.trackChanges()

        then:"If no changes a present then hasChanges returns false"
            !b.hasChanged()
            !b.hasChanged("title")

        when:"The a property is changed"
            b.title = "Changed"

        then:"The changes are tracked"
            b.hasChanged()
            b.hasChanged("title")
            b.getOriginalValue('title') == "My Title"
            b.listDirtyPropertyNames() == ['title']
            !b.hasChanged("author")

        when:"A property with a getter and setter is changed"
            b.title = "Some other value"
            b.author = "Some Bloke"

        then:"We track that change too"
            b.hasChanged("title")
            b.getOriginalValue('title') == "My Title"
            b.listDirtyPropertyNames() == ['title', 'author']
            b.hasChanged("author")

    }

    void "Test that dirty checking transformation doesn't allow for NPE for new objects"(){
			given: "A new book is created"
				def book = new Book()

			when: "Title is set"
				book.title = "Title"

			then: "getOrginal Value returns null"
				book.getOriginalValue('title') == null
    }

    @Issue('https://github.com/grails/grails-data-mapping/issues/629')
    void "Test that dirty checking does not mark the object of dirty if a setter is called with the same value"() {
        when: "A new book is created"
        def book = new Book(title: "The Stand")
        book.trackChanges()

        then:"The object has no changes"
        !book.hasChanged()

        when:"A property is set to the same value"
        book.title = "The Stand"

        then:"The object has no changes"
        !book.hasChanged()

        when:"A property is set to a different value"
        book.title = "The Shining"

        then:"The object has changes"
        book.hasChanged()
        book.hasChanged('title')
        book.getOriginalValue('title') == 'The Stand'

        when:"A property is set to null"
        book.title = null

        then:"The object has changes"
        book.hasChanged()
        book.hasChanged('title')
        book.getOriginalValue('title') == 'The Stand'

        when:"A trackChanges is reset and property is set to null"
        book.title = "The Stand"
        book.trackChanges()
        book.title = null

        then:"The object has changes"
        book.hasChanged()
        book.hasChanged('title')
        book.getOriginalValue('title') == 'The Stand'

    }
    @Ignore // currently fails, TODO: add support for dirty checking collection/map changes
    void "Test that you can dirty check changes to simple collections"() {
        given: "A new book is created"
        def book = new Book()

        when: "a simple collection is modified"
        book.title = "Title"
        book.trackChanges()
        book.simple << 1

        then: "getOrginal Value returns null"
        book.hasChanged('simple')
    }

    void "Test dirty check with inheritance"() {
        when:"An inherited property is updated"
            def b = new KidsBook()
            b.age = 10
            b.trackChanges()
            b.age = 12
        then:"It is dirty"
            b.hasChanged()
            b.hasChanged("age")
    }
}

@DirtyCheck
class Book {
    String title
    Date releaseDate
    List<Integer> simple = []

    private String author

    void setAuthor(String author) {
        this.author = author
    }
    String getAuthor() {
        return this.author
    }
}

@DirtyCheck
class KidsBook extends Book{
    int age
}

