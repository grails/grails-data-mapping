package org.grails.datastore.gorm.neo4j

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship
import org.neo4j.graphdb.Direction
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty

class Neo4jController {

	GraphDatabaseService graphDatabaseService

    static defaultAction = "node"

    def beforeInterceptor = {
        if (!["127.0.0.1", "0:0:0:0:0:0:0:1"].contains(request.remoteAddr)) {
            render(status: 503, text: 'Access limited to localhost')
            return false
        }
    }

    def node = {
        def node = params.id ? graphDatabaseService.getNodeById(params.long("id")) : graphDatabaseService.referenceNode;
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
        def reltypeCounter = [:]

        for (Node node in graphDatabaseService.allNodes) {
            def type = node.getProperty("type", "n/a")
            typeCounter[type]  = typeCounter.get(type, 0)+1

            for (Relationship rel in node.getRelationships(Direction.OUTGOING)) {
                def reltype = rel.type.name()
                reltypeCounter[reltype]  = reltypeCounter.get(reltype, 0)+1

            }
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
            for (GrailsDomainClassProperty prop in dc.persistantProperties) {

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
}
