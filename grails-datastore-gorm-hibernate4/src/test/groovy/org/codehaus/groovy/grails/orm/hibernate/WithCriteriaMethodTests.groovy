package org.codehaus.groovy.grails.orm.hibernate

import static junit.framework.Assert.*
import org.junit.Test

class WithCriteriaMethodTests extends AbstractGrailsHibernateTests {

    @Test
    void testWithCriteriaMethod() {
        def authors = []
        authors << WithCriteriaMethodAuthor.newInstance()
        authors << WithCriteriaMethodAuthor.newInstance()
        authors << WithCriteriaMethodAuthor.newInstance()

        authors[0].name = "Stephen King"
        authors[1].name = "John Grisham"
        authors[2].name = "James Patterson"
        authors*.save(true)

        def results = WithCriteriaMethodAuthor.withCriteria {
            like('name','J%')
        }

        assertEquals 2, results.size()
    }

    @Override
    protected getDomainClasses() {
        [WithCriteriaMethodAuthor, WithCriteriaMethodBook]
    }
}

class WithCriteriaMethodBook {
    Long id
    Long version
    def belongsTo = WithCriteriaMethodAuthor
    WithCriteriaMethodAuthor author
    String title
    boolean equals(obj) { title == obj?.title }
    int hashCode() { title ? title.hashCode() : super.hashCode() }
    String toString() { title }

    static constraints = {
        author(nullable:true)
    }
}
class WithCriteriaMethodAuthor {
    Long id
    Long version
    String name
    Set books
    def relatesToMany = [books:WithCriteriaMethodBook]
    boolean equals(obj) { name == obj?.name }
    int hashCode() { name ? name.hashCode() : super.hashCode() }
    String toString() { name }
}
