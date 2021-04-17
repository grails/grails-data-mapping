package org.grails.datastore.mapping.dirty.checking

import groovy.transform.Sortable
import groovy.transform.Generated
import spock.lang.Issue
import spock.lang.Specification

import java.lang.reflect.Method

class DirtyCheckableSpec extends Specification {

    @Issue('https://github.com/grails/grails-data-mapping/issues/1231')
    def 'setting a field that implements Comparable dirty checks properly'() {
        given: 'a person'
        def person = new Person(name: 'John Doe')

        and: 'a blog post'
        def blogPost = new BlogPost(title: 'Hello World', content: 'some content')
        person.lastViewedPost = blogPost

        and: 'a second blog post is made'
        def anotherBlogPost = new BlogPost(title: blogPost.title, content: 'different content, same title')

        and: 'change list is reset'
        person.trackChanges()

        when: 'the lastViewedPost is set to something different'
        person.lastViewedPost = anotherBlogPost

        and: 'markDirty is called (normally would be weaved in by a DirtyCheckingTransformer)'
        person.markDirty('lastViewedPost', anotherBlogPost, blogPost)

        then:
        person.hasChanged()
        person.hasChanged('lastViewedPost')
        person.getOriginalValue('lastViewedPost').equals(blogPost)
    }

    def 'setting a field that is a boolean dirty checks properly'() {
        given: 'a class with a boolean property'
        def animal = new Animal()
        animal.trackChanges()

        when:"A boolean property is changed"
        animal.barks = true
        animal.markDirty("barks", true, false)

        then:"the property changed"
        animal.barks
        animal.hasChanged()
        animal.hasChanged("barks")

        when:"it is set to false"
        animal.trackChanges() // reset
        animal.barks = false
        animal.markDirty("barks", false, true)

        then:"the property changed"
        !animal.barks
        animal.hasChanged()
        animal.hasChanged("barks")

    }

    void "test that all DirtyCheckable trait methods are marked as Generated"() {
        expect: "all DirtyCheckable methods are marked as Generated on implementation class"
        DirtyCheckable.getMethods().each { Method traitMethod ->
            assert Animal.class.getMethod(traitMethod.name, traitMethod.parameterTypes).isAnnotationPresent(Generated)
        }
    }
}

class Animal implements DirtyCheckable {
    boolean barks
}

class Person implements DirtyCheckable {
    String name
    BlogPost lastViewedPost
}

@Sortable(includes = ['title'])
class BlogPost {
    String title
    String content
}
