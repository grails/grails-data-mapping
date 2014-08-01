package org.grails.datastore.gorm.neo4j

import org.grails.datastore.gorm.neo4j.engine.CypherEngine
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Label
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship
import org.neo4j.graphdb.Direction
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.neo4j.graphdb.RelationshipType
import org.neo4j.helpers.collection.IteratorUtil
import org.neo4j.tooling.GlobalGraphOperations

class Neo4jController {

	GraphDatabaseService graphDatabaseService
    CypherEngine cypherEngine

    static defaultAction = "node"

    def beforeInterceptor = {
        if (!["127.0.0.1", "0:0:0:0:0:0:0:1"].contains(request.remoteAddr)) {
            render(status: 503, text: 'Access limited to localhost')
            return false
        }
    }

    def node = {
        def node = graphDatabaseService.getNodeById(params.long("id") ?: 0)
        assert node        
        [node: node]
    }

    def relationship = {
        def rel = graphDatabaseService.getRelationshipById(params.long("id"))
        assert rel
        [rel:rel]
    }

    def statistics = {

        def typeCounter = [:]
        def reltypeCounter = [:].withDefault { 0 }

        def ggo = GlobalGraphOperations.at(graphDatabaseService)

        for (Label label in ggo.allLabels) {
            def count = IteratorUtil.count(ggo.getAllNodesWithLabel(label));
            typeCounter[label.name()] = count
        }


        for (Relationship rel in ggo.getAllRelationships()) {
            reltypeCounter[rel.type.name()]++

        }

        [typeCounter:typeCounter, reltypeCounter:reltypeCounter]
    }

    def domain = {

        def filter=["bidirectional", "oneToMany", "manyToOne", "manyToMany", "hasOne", "oneToOne", "owningSide"]
        def filters = params.list("filter")
        def domainProps = []
        def domainClassesNames = params.list("domainClasses")

        def allDomainClasses = grailsApplication.domainClasses.collect {it}. // use a clone
                sort { it.name }

        def domainClasses = allDomainClasses.findAll { domainClassesNames.contains(it.name) }

        for (GrailsDomainClass dc in domainClasses) {
            for (GrailsDomainClassProperty prop in dc.persistentProperties) {

                if (filters.any { prop."$it" } ) {
                //if (!params.filter || params.filter=='all' || prop.association) {
                    domainProps << prop
                }
            }
        }
		
        [       domainProps:domainProps,
                domainClassesNames:domainClassesNames, 
                allDomainClasses: allDomainClasses,
                filter:filter,
                filters:filters
        ]
	}

    def doforward() {
        forward action:"node"
    }

    def cypher = {
        cypherEngine.execute( "CREATE (n:Dummy) return n")
        cypherEngine.commit(  )
    }
}
