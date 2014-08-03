package org.grails.datastore.gorm.neo4j

import grails.converters.JSON
import grails.test.spock.IntegrationSpec
import org.codehaus.groovy.grails.web.json.JSONElement
import org.neo4j.graphdb.DynamicRelationshipType
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Transaction

class GetOrSetSpec extends IntegrationSpec {

    GraphDatabaseService graphDatabaseService
    Transaction tx

    def setup() {
        tx = graphDatabaseService.beginTx()
    }

    def cleanup() {
        tx.close()
    }

    def "test getOrSet method on nodes"() {
        setup:
        def node = graphDatabaseService.createNode()

        when: 'setting a property'
        node."$propertyName" = propertyValue

        then: 'retrieving the property'
        node."$propertyName" == propertyValue

        where:
        propertyName  | propertyValue
        'name'        | 'abc'
        'createdDate' | new Date()
        'count'       | 5
        'price'       | 2.12f
    }

    def "marshalling test for nodes"() {
        when:
        def n = graphDatabaseService.createNode()
        n.setProperty('myproperty', 'myvalue')
        def json = marshalAsJSON(n)


        then:
        json.id == n.id
        json.myproperty == 'myvalue'
        json.relationships == []

    }

    def "marshalling test for relationship"() {
        when:
        def startNode = graphDatabaseService.createNode()
        startNode.setProperty('myproperty', 'startnode')
        def endNode = graphDatabaseService.createNode()
        endNode.setProperty('myproperty', 'endnode')
        def rel = startNode.createRelationshipTo(endNode, DynamicRelationshipType.withName('RELTYPE'))
        rel.setProperty('myproperty', 'rel')
        def json = marshalAsJSON(rel)

        then:
        json.id == rel.id
        json.myproperty == 'rel'
        json.startNode == startNode.id
        json.endNode == endNode.id
        json.type == 'RELTYPE'

    }

    private JSONElement marshalAsJSON(object) {
        def sw = new StringWriter()
        (object as JSON).render(sw)
        JSON.parse(sw.toString())
    }



}
