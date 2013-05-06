package org.grails.datastore.gorm.neo4j

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Relationship
import org.neo4j.graphdb.Direction
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty
import org.neo4j.graphdb.Traverser

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

    def traverse = { TraverseCommand cmd ->

        if (params.traverse ) {
            log.error "cmd: $cmd"
            if (!cmd.validate()) {
                cmd.errors.each {
                    log.error it
                }
            }
         //Node node = params.id ? neoService.getNodeById(params.id as long) : neoService.getReferenceNode();
         //node.traverse()
        } else {
    	}
	    [command: cmd]
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

    def fixDoubleRelationShips = {

        def counter = 0
        def removed = 0
        def intransaction = 0

        def tx = graphDatabaseService.beginTx()

        for (Node node in graphDatabaseService.allNodes) {
            counter++
            if (counter%10000==0) {
                log.error "Processing $counter"
            }

            if (intransaction > 5000) {
                log.error "intransaction: $intransaction -> new transaction"
                intransaction = 0
                tx.success()
                tx.finish()
                tx = graphDatabaseService.beginTx()
            }

            def relationshipsMap = [:]

            for (Relationship rel in node.getRelationships(Direction.OUTGOING)) {
                def reltype = rel.type.name()
                //reltypeCounter[reltype]  = reltypeCounter.get(reltype, 0)+1

                def key = [ reltype, rel.startNode.id, rel.endNode.id]
                //key.sort()

                if (relationshipsMap.containsKey(key)) {
                    relationshipsMap[key] << rel
                } else {
                    relationshipsMap[key] = [rel]
                }

            }
            def doubledRelationships = relationshipsMap.findAll {k, v -> v.size() > 1}

            doubledRelationships.each {
                log.debug "${it.key} -> ${it.value}"
                removed += it.value.size()-1
                intransaction += it.value.size()-1
                it.value[1..-1].each { rel -> rel.delete() }
            }

        }

        tx.success()
        tx.finish()


        [  removed: removed ]
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

public class TraverseCommand {

    long id
    Traverser.Order order
    Closure stopEvaluator
    Closure returnableEvaluator

}