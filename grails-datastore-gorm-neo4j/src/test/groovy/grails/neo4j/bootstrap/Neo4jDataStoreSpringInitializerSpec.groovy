package grails.neo4j.bootstrap

import grails.persistence.Entity
import org.grails.datastore.gorm.neo4j.TestServer
import org.neo4j.server.web.WebServer
import org.neo4j.test.TestGraphDatabaseFactory
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
        when:"neo4j is initialised"
        def init = new Neo4jDataStoreSpringInitializer(Book)
        init.configure()

        then:"GORM for Neo4j is correctly configured"
        Book.count() == 0
    }

    void "Test Neo4jDataStoreSpringInitializer loads neo4j for REST"() {
        setup:
        def graphDb = new TestGraphDatabaseFactory().newImpermanentDatabase()
        def port
        WebServer webServer
        (port, webServer) = TestServer.startWebServer(graphDb)

        when:"neo4j is initialised"
        def config = [grails: [neo4j: [type: "rest", username:'neo4j', password:'letmein', location:"http://localhost:${port}/db/data"]]]
        def init = new Neo4jDataStoreSpringInitializer( config, Book)
        init.configure()

        then:"GORM for Neo4j is correctly configured"
        Book.count() == 0

//        cleanup:
//        graphDb.shutdown()
//        webServer.stop()
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
class Book {
    String title
}
