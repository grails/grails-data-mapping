package grails.neo4j.bootstrap

import grails.persistence.Entity
import org.grails.datastore.gorm.neo4j.Neo4jDatastore
import org.grails.datastore.gorm.neo4j.TestServer
import org.grails.datastore.gorm.neo4j.rest.GrailsCypherRestGraphDatabase
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.server.web.WebServer
import org.neo4j.test.TestGraphDatabaseFactory
import org.springframework.context.ApplicationContext
import spock.lang.Specification

/*
 * Copyright 2014 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author graemerocher
 */
class Neo4jDataStoreSpringInitializerSpec extends Specification {

    void "Test Neo4jDataStoreSpringInitializer loads neo4j correctly"() {
        setup:"neo4j is initialised"
        def init = new Neo4jDataStoreSpringInitializer(Author, Book)
        def ctx = init.configure()

        when:"A GORm method is executed"
        int count = Book.count()
        then:"GORM for Neo4j is correctly configured"
        count == 0

        when:
        Author author
        Author.withTransaction {
            author = new Author(name: "Stephen King")
                    .addToBooks(title: "")
            author.validate()

        }

        then:"GORM for Neo4j is correctly configured"
        author.errors.hasErrors()

        cleanup:
        ctx.close()
    }


    void "Test Neo4jDataStoreSpringInitializer loads neo4j for REST"() {
        setup:
        def graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase()
        def port
        WebServer webServer
        (port, webServer) = TestServer.startWebServer(graphDb)

        when:"neo4j is initialised"
        def config = [grails: [neo4j: [type: "rest", location:"http://localhost:${port}/db/data"]]]
        def init = new Neo4jDataStoreSpringInitializer( config, Author, Book)
        def ctx = init.configure()

        then:"GORM for Neo4j is correctly configured"
        Book.count() == 0
        ctx.getBean(GraphDatabaseService) instanceof GrailsCypherRestGraphDatabase

        when:"A book is saved"
        Book.withTransaction {
            def a = new Author(name:"Stephen King")
            def b = new Book(title: "The Stand")
            a.addToBooks(b)
            a.save(flush:true)
        }

        then:"The save count is 1"
        Book.count() == 1

        when:"A book is queried"
        Book.withSession { it.clear() }
        Book book = Book.findByTitle("The Stand")

        then:"The result is correct"
        book != null
        book.title == "The Stand"
        book.author.name == "Stephen King"

        cleanup:
        graphDb.shutdown()
        webServer?.stop()
    }


    void "Test configuration from map Neo4jDataStoreSpringInitializer loads neo4j correctly"() {
        when:"neo4j is initialised"

        def config = [grails: [neo4j: [url: "jdbc:foo:bar", options:[one:"two"]]]]
        def init = new Neo4jDataStoreSpringInitializer(config,Book)
//        init.configure()

        then:"GORM for Neo4j is correctly configured"
        init.configuration.getProperty("grails.neo4j.url") == "jdbc:foo:bar"
        init.configuration.getProperty("grails.neo4j.options", Map.class) == [one:"two"]

    }
}

@Entity
class Author {
    String name
    static hasMany = [books:Book]

    static constraints = {
        name blank:false
    }
}
@Entity
class Book {
    String title
    static belongsTo = [author:Author]
    static constraints = {
        title blank:false
    }
}
