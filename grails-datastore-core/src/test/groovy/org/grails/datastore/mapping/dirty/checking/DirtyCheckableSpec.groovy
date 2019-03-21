package org.grails.datastore.mapping.dirty.checking


import groovy.transform.Sortable
import spock.lang.Issue
import spock.lang.Specification

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
