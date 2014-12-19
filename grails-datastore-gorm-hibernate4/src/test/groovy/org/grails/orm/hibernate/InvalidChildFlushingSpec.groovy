package org.grails.orm.hibernate

import grails.persistence.Entity
import spock.lang.Issue

/**
 */
@Issue('GRAILS-8922')
class InvalidChildFlushingSpec extends GormSpec{

    @Issue('GRAILS-8922')
    void "test one-to-many with new invalid child object and call to save() on child object"() {

        given:"A domain with a one-to-many association"
            def author = new InvalidChildFlushingAuthor(name: 'author1')
            author.save(failOnError: true, flush: true)

            def book = new InvalidChildFlushingBook(author: author, name: 'invalid')
            author.addToBooks(book)

        when:"An object with an invalid child is saved and the session is flushed"
            assert !book.save()
            session.flush()

        // problem continues to exist in Hibernate, if this starts passing problem fixed in Hibernate
        then:"An error thrown"
            thrown Exception
    }

    @Issue('GRAILS-8922')
    void "test one-to-one with new invalid child object"() {
        given:"A domain with a one-to-one association"
            def face = new InvalidChildFlushingFace()
            face.save(failOnError: true, flush: true)

            def nose = new InvalidChildFlushingNose(face: face, size: 'invalid')
            face.nose = nose

        when:"An object with an invalid child is saved and the session is flushed"
            assert !nose.save()

        then:"No error occurs"
            nose != null
            InvalidChildFlushingNose.count () == 0

        when:"The session is flushed"
            session.flush()

        // problem continues to exist in Hibernate, if this starts passing problem fixed in Hibernate
        then:"An error thrown"
            thrown Exception
    }

    @Issue('GRAILS-8922')
    void "test one-to-one with existing valid child object changed to invalid"() {
        given:"A domain with a one-to-one association"
            def face = new InvalidChildFlushingFace()
            face.save(failOnError: true, flush: true)

            def nose = new InvalidChildFlushingNose(face: face, size: 'valid')
            face.nose = nose
            nose.save(failOnError: true, flush: true)

        when:"An object with an invalid child is updated and the session is flushed"
            nose.size = 'invalid'
            assert !nose.save()

            session.flush()

        then:"No errors occured"
            nose != null
    }

    @Override
    List getDomainClasses() {
        return [InvalidChildFlushingAuthor, InvalidChildFlushingBook, InvalidChildFlushingFace, InvalidChildFlushingNose]
    }
}
@Entity
class InvalidChildFlushingAuthor {
    Long id
    Long version

    String name

    Set books
    static hasMany = [books:InvalidChildFlushingBook]
}

@Entity
class InvalidChildFlushingBook {
    Long id
    Long version

    InvalidChildFlushingAuthor author
    static belongsTo = [author:InvalidChildFlushingAuthor]

    String name

    static constraints = {
        name(validator: {it != 'invalid'})
    }
}

@Entity
class InvalidChildFlushingFace {
    Long id
    Long version

    InvalidChildFlushingNose nose
    static hasOne = [nose:InvalidChildFlushingNose]

    static constraints = {
        nose(nullable: true)
    }

}

@Entity
class InvalidChildFlushingNose {
    Long id
    Long version

    InvalidChildFlushingFace face
    static belongsTo = [face:InvalidChildFlushingFace]

    String size

    static constraints = {
        size(validator: {it != 'invalid'})
    }
}
