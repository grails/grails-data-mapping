package grails.gorm.tests

import grails.persistence.Entity

/**
 * Created by graemerocher on 12/02/14.
 */
class SortWithNestedPropertiesSpec extends GormDatastoreSpec{


    def setup() {
        ['C','A','b','a','c','B'].eachWithIndex { name, i ->
            def person = new SortPerson(version:1, name:name).save(flush:true)
            def author = new SortAuthor(version:1, name:name, person:person).save(flush:true)
            def address = new SortAddress(street:name, city:'Oslo')
            new SortBook(version:1, title:'foo', author:author, address:address).save(flush:true)
        }
    }

    void "Test the list method with nested property sort"() {
        expect:
            SortBook.list(sort:'author.name').author.name == ['A','a','b','B','C','c']
            SortBook.list(sort:'author.name', ignoreCase:false).author.name == ['A','B','C','a','b','c']
    }

    void "Test sort with named query builder"() {
        expect:
            SortBook.manningBooks().list(sort:'author.name').author.name == ['A','a','b','B','C','c']
    }

    void "Test sort with findWhere method"() {
        expect:
            SortBook.findAllWhere([publisher: 'Manning']).author.name.sort() == ['A','B','C', 'a','b','c']
    }

    void "Test sort with findBy dynamic method"() {
        expect:
            SortBook.findAllByPublisher('Manning', [sort:'author.name']).author.name == ['A','a','b','B','C','c']
    }

    void "Test sort with find dynamic method"() {
        expect:
            SortBook.findByPublisher('Manning', [sort:'author.name']).author.name == 'A'
    }

    void "Test deep sort"() {
        expect:
            SortBook.list(sort:'author.person.name').author.person.name == ['A','a','b','B','C','c']
    }

    void "Test other parameters are presevered with sort query"() {
        expect:
            equalsIgnoreCase(['b','B','C'], SortBook.list(max:3, offset:2, sort:'author.name').author.name)
            equalsIgnoreCase(['C','a','b'], SortBook.list(max:3, offset:2, sort:'author.name', ignoreCase:false).author.name)
            equalsIgnoreCase(['b','B','C'], SortBook.manningBooks().list(max:3, offset:2, sort:'author.name').author.name)
            equalsIgnoreCase(['b','B','C'], SortBook.findAllByPublisher('Manning', [max:3, offset:2, sort:'author.name']).author.name)
            equalsIgnoreCase(['b','B','C'], SortBook.list(max:3, offset:2, sort:'author.person.name').author.person.name)
    }

    boolean equalsIgnoreCase(Collection<String> a, Collection<String> b) {
        a.collect{it.toLowerCase()} ==  b.collect{it.toLowerCase()}
    }

    void "Test sort with embedded property"() {
        expect:
            equalsIgnoreCase(['A','a','b','B','C','c'], SortBook.list(sort:'address.street').address.street)
    }

    void "Test default sort"() {
        expect:
            equalsIgnoreCase(['A','a','b','B','C','c'], SortBook.list().address.street)
    }

    @Override
    List getDomainClasses() {
        [SortBook, SortAuthor, SortPerson]
    }
}

@Entity
class SortBook {
    Long id
    Long version
    String title
    SortAuthor author
    SortAddress address
    String publisher = 'Manning'
    static embedded = ["address"]
    static namedQueries = {
        manningBooks {
            eq('publisher', 'Manning')
        }
    }
    static mapping = {
        sort 'author.name'
    }
}

@Entity
class SortAuthor {
    Long id
    Long version

    String name
    SortPerson person
}

@Entity
class SortPerson {
    Long id
    Long version

    String name
}

class SortAddress {
    String street
    String city
}
