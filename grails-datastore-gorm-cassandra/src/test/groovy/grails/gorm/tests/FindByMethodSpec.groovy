package grails.gorm.tests

import com.datastax.driver.core.exceptions.InvalidQueryException


/**
 * @author graemerocher
 */
class FindByMethodSpec extends GormDatastoreSpec {
    
    void 'Test Using AND Multiple Times In A Dynamic Finder'() {
        given:
            new Person(firstName: 'Jake', lastName: 'Brown', age: 11).insert()
            new Person(firstName: 'Zack', lastName: 'Brown', age: 14).insert()
            new Person(firstName: 'Jeff', lastName: 'Brown', age: 41).insert()
            new Person(firstName: 'Zack', lastName: 'Galifianakis', age: 41).insert()

        when:
            def people = Person.findAllByFirstNameAndLastNameAndAge('Jeff', 'Brown', 1)

        then:
            0 == people?.size()

        when:
            people = Person.findAllByFirstNameAndLastNameAndAgeGreaterThan('Zack', 'Brown', 20)

        then:
            0 == people?.size()

        when:
            people = Person.findAllByFirstNameAndLastNameAndAgeGreaterThan('Zack', 'Brown', 8)

        then:
            1 == people?.size()
            14 == people[0].age

        when:
            people = Person.findAllByFirstNameAndLastNameAndAgeLessThan('Jake', 'Brown', 14)

        then:
            1 == people?.size()
            11 == people[0].age
        
        when:
            people = Person.findAllByLastNameAndAgeLessThanEquals('Brown', 14, [allowFiltering:true])

        then:
            2 == people?.size()
            11 == people[0].age
            14 == people[1].age
        
        when:
            people = Person.findAllByLastNameAndAgeGreaterThanEquals('Brown', 14, [allowFiltering:true])

        then:
            2 == people?.size()
            14 == people[0].age
            41 == people[1].age
        
        when:
            people = Person.findAllByLastNameAndAgeBetween('Brown', 11, 42, [allowFiltering:true])

        then:
            3 == people?.size()
            11 == people[0].age
            14 == people[1].age
            41 == people[2].age
            
        when:
            def cnt = Person.countByFirstNameAndLastNameAndAge('Jake', 'Brown', 11)

        then:
            1 == cnt

        when:
            cnt = Person.countByFirstNameAndLastNameAndAgeInList('Zack', 'Brown', [12, 13, 14, 15])

        then:
            1 == cnt
    }

    void 'Test Using OR exception In A Dynamic Finder'() {
        given:
            new Person(firstName: 'Jake', lastName: 'Brown', age: 11).save()
            new Person(firstName: 'Zack', lastName: 'Brown', age: 14).save()
            new Person(firstName: 'Jeff', lastName: 'Brown', age: 41).save()
            new Person(firstName: 'Zack', lastName: 'Galifianakis', age: 41).save()

        when:
            def people = Person.findAllByFirstNameOrLastNameOrAge('Zack', 'Tyler', 125)

        then:
           thrown UnsupportedOperationException      
    }

    void testBooleanPropertyQuery() {
        given:
            new Highway(bypassed: true, name: 'Bypassed Highway').save()
            new Highway(bypassed: true, name: 'Bypassed Highway').save()
            new Highway(bypassed: false, name: 'Not Bypassed Highway').save()
            new Highway(bypassed: false, name: 'Not Bypassed Highway').save()

        when:
            def highways= Highway.findAllBypassedByName('Not Bypassed Highway', [allowFiltering:true])

        then:
            0 == highways.size()

        when:
            highways = Highway.findAllNotBypassedByName('Not Bypassed Highway', [allowFiltering:true])

        then:
            2 == highways?.size()
            'Not Bypassed Highway' == highways[0].name
            'Not Bypassed Highway'== highways[1].name

        when:
            highways = Highway.findAllBypassedByName('Bypassed Highway', [allowFiltering:true])

        then:
            2 == highways?.size()
            'Bypassed Highway'== highways[0].name
            'Bypassed Highway'== highways[1].name

        when:
            highways = Highway.findAllNotBypassedByName('Bypassed Highway', [allowFiltering:true])
        then:
            0 == highways?.size()

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
            highway = Highway.findNotBypassedByName('Not Bypassed Highway', [allowFiltering:true])
        then:
            'Not Bypassed Highway' == highway?.name

        when:
            highway = Highway.findBypassedByName('Bypassed Highway', [allowFiltering:true])
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

        when:
            book = Book.findPublishedByTitleOrAuthor('Fly Fishing For Everyone', 'Dierk')
        then:
            thrown UnsupportedOperationException 
            
        when:
            book = Book.findByTitle('DGGv2')
        then:
            'Jeff'== book.author
            'DGGv2'== book.title
            true == book.published
        
        when:
            book = Book.findByTitle('None')
        then:
            book == null
        
        when: 
            book = Book.findByAuthorAndTitle('Jeff', 'DGGv2')
        then:
            'Jeff'== book.author
            'DGGv2'== book.title
            true == book.published
        
        when:
            book = Book.findByAuthorAndTitle('Jeff', 'None')
        then:
            book == null
            
        when:
            book = Book.findNotPublished()
        then:
            'Fly Fishing For Everyone' == book?.title
        /* throws ReadTimeoutException, kind of expected
        when:
            def books = Book.findAllByTitlePublished('DGGv2', [allowFiltering:true])
        then:
            2 == books?.size()
        */
        when:
            def books = Book.findAllPublished()
        then:
            3 == books?.size()

        when:
            books = Book.findAllNotPublished()
        then:
            1 == books?.size()

        when:
            books = Book.findAllByAuthor('Jeff')
        then:
            2 == books?.size()
        
        when:
            books = Book.findAllByAuthorAndTitle('Jeff', 'DGGv2')
        then:
            1 == books?.size()
        
        when:
            books = Book.findAllByTitle('Fly Fishing For Everyone')
        then:
            1 == books?.size()
            
        when:
            books = Book.findAllPublishedByTitleAndAuthor('DGGv2', 'Graeme')
        then:
            1 == books?.size()

        when:
            books = Book.findAllPublishedByAuthorOrTitle('Graeme', 'GINA')
        then:
            thrown UnsupportedOperationException 

        when:
            books = Book.findAllNotPublishedByAuthor('Jeff')
        then:
            1 == books?.size()

        when:
            books = Book.findAllNotPublishedByAuthor('Graeme')
        then:
            0 == books?.size()
    }

    void "Test findOrCreateBy For A Record That Does Not Exist In The Database"() {
        when:
            def book = Book.findOrCreateByAuthor('Someone')
            def highway = Highway.findOrCreateByName('Name')
        then:
            'Someone' == book.author
            null == book.title
                   
            'Name' == highway.name
            null == highway.other
            null == highway.id        
    }

    void "Test findOrCreateBy With An AND Clause"() {
        when:
            def book = Book.findOrCreateByAuthorAndTitle('Someone', 'Something')
            def highway = Highway.findOrCreateByNameAndOther('Name', 'Other', [allowFiltering:true])
        then:
            'Someone' == book.author
            'Something' == book.title
            
            'Name' == highway.name
            'Other' == highway.other
            null == highway.id
    }

    void "Test findOrCreateBy Throws Exception If An OR Clause Is Used"() {
        when:
            Book.findOrCreateByAuthorOrTitle('Someone', 'Something')

        then:
            thrown(MissingMethodException)
    }

    void "Test findOrSaveBy For A Record That Does Not Exist In The Database"() {
        when:
            def book = Book.findOrSaveByAuthorAndTitle('Some New Author', 'Some New Title')
            def highway = Highway.findOrSaveByNameAndOther('Name', 'Other', [allowFiltering:true])
        then:
            'Some New Author' == book.author
            'Some New Title' == book.title
            
            'Name' == highway.name
            'Other' == highway.other
            null != highway.id
    }

    void "Test findOrSaveBy For A Record That Does Exist In The Database"() {

        given:
            def originalId = new Highway(name: 'Name', other: 'Other').save().id

        when:
            def highway = Highway.findOrSaveByName('Name')

        then:
            'Name' == highway.name
            'Other' == highway.other
            originalId == highway.id
    }

    void "Test patterns which shold throw MissingMethodException"() {
            // Redis doesn't like Like queries...
        when:
            Book.findOrCreateByAuthorLike('B%')

        then:
            thrown UnsupportedOperationException

        when:
            Book.findOrCreateByAuthorInList(['Jeff'])

        then:
            thrown MissingMethodException

        when:
            Book.findOrCreateByAuthorOrTitle('Jim', 'Title')

        then:
            thrown MissingMethodException

        when:
            Book.findOrCreateByAuthorNotEqual('B')

        then:
            thrown UnsupportedOperationException

        when:
            Book.findOrCreateByAuthorGreaterThan('B')

        then:
            thrown InvalidQueryException

        when:
            Book.findOrCreateByAuthorLessThan('B')

        then:
            thrown InvalidQueryException

        when:
            Book.findOrCreateByAuthorBetween('A', 'B')

        then:
            thrown InvalidQueryException

        when:
            Book.findOrCreateByAuthorGreaterThanEquals('B')

        then:
            thrown InvalidQueryException

        when:
            Book.findOrCreateByAuthorLessThanEquals('B')

        then:
            thrown InvalidQueryException

            // GemFire doesn't like these...
        when:
            Book.findOrCreateByAuthorIlike('B%')

        then:
            thrown UnsupportedOperationException

        when:
            Book.findOrCreateByAuthorRlike('B%')

        then:
            thrown UnsupportedOperationException

        when:
            Book.findOrCreateByAuthorIsNull()

        then:
            thrown UnsupportedOperationException

        when:
            Book.findOrCreateByAuthorIsNotNull()

        then:
            thrown UnsupportedOperationException

        when:
            Book.findOrSaveByAuthorLike('B%')

        then:
            thrown UnsupportedOperationException

        when:
            Book.findOrSaveByAuthorInList(['Jeff'])

        then:
            thrown MissingMethodException

        when:
            Book.findOrSaveByAuthorOrTitle('Jim', 'Title')

        then:
            thrown MissingMethodException

        when:
            Book.findOrSaveByAuthorNotEqual('B')

        then:
            thrown UnsupportedOperationException

        when:
            Book.findOrSaveByAuthorGreaterThan('B')

        then:
            thrown InvalidQueryException

        when:
            Book.findOrSaveByAuthorLessThan('B')

        then:
            thrown InvalidQueryException

        when:
            Book.findOrSaveByAuthorBetween('A', 'B')

        then:
            thrown InvalidQueryException

        when:
            Book.findOrSaveByAuthorGreaterThanEquals('B')

        then:
            thrown InvalidQueryException

        when:
            Book.findOrSaveByAuthorLessThanEquals('B')

        then:
            thrown InvalidQueryException

        when:
            Book.findOrSaveByAuthorIlike('B%')

        then:
            thrown UnsupportedOperationException

        when:
            Book.findOrSaveByAuthorRlike('B%')

        then:
            thrown UnsupportedOperationException

        when:
            Book.findOrSaveByAuthorIsNull()

        then:
            thrown UnsupportedOperationException

        when:
            Book.findOrSaveByAuthorIsNotNull()

        then:
            thrown UnsupportedOperationException
    }
}
