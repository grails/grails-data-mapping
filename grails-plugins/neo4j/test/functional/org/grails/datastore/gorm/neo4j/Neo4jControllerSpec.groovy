package org.grails.datastore.gorm.neo4j

import geb.spock.GebReportingSpec
import org.grails.datastore.gorm.neo4j.pages.Neo4jPage
import org.grails.datastore.gorm.neo4j.pages.Neo4jPageForward
import spock.lang.Stepwise


@Stepwise
class Neo4jControllerSpec extends GebReportingSpec {

    def "navigating nodes"() {
        when:
        to Neo4jPage
        then:
        true
    }

    def "forwards work"() {
        when:
        to Neo4jPageForward
        then:
        true
    }

}
