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

