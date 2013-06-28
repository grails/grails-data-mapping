package org.grails.datastore.gorm.dirty.checking

import grails.gorm.dirty.checking.DirtyCheck
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import spock.lang.Specification

/**
 * @author Graeme Rocher
 */
class DirtyCheckTransformationSpec extends Specification {

    void "Test that the dirty checking transformations allows you to track changes to a class"() {
        when:"A new dirty checking instance is created"
            def b = new Book()

        then:"It implements the ChangeTrackable interface"
            b instanceof DirtyCheckable
            !b.hasChanged()
            !b.hasChanged("title")

        when:"The title is changed"
            b.title = "My Title"

        then:"There are no changes because we haven't started tracking"
            !b.hasChanged()
            !b.hasChanged("title")

        when:"We start tracking and change the title"
            b.trackChanges()
            b.title = "Changed"

        then:"The changes are tracked"
            b.hasChanged()
            b.hasChanged("title")
            !b.hasChanged("author")

        when:"A property with a getter and setter is changed"
            b.author = "Some Bloke"

        then:"We track that change too"
            b.hasChanged("author")
    }
}

@DirtyCheck
class Book {
    String title
    Date releaseDate

    private String author
    void setAuthor(String author) {
        this.author = author
    }
    String getAuthor() {
        return this.author
    }

}

