package test

import grails.neo4j.bootstrap.Neo4jDataStoreSpringInitializer

/**
 * Created by graemerocher on 13/11/15.
 */
class Neo4jGrails2PluginSpec extends spock.lang.Specification {

    void "Test that mongodb works with Grails 2"() {
        when:"A new book is created that is invalid"
        def b = new Book(title:"")

        then:"the book is invalid"
        !b.validate()
        !b.save()

        when:"the book is made valid"
        b.title = "The Stand"

        then:"It can be saved"
        b.save(flush:true)
        Book.count() == 1
        Book.find("MATCH (n:Book) WHERE n.title = {1} RETURN n", ["The Stand"])

    }
}
