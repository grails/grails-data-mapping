package org.grails.datastore.gorm.mongo

import grails.gorm.tests.GormDatastoreSpec
import grails.persistence.Entity
import org.bson.types.ObjectId
import org.grails.datastore.mapping.proxy.EntityProxy
import spock.lang.Issue

/**
 * @author Graeme Rocher
 */
class InheritanceWithSingleEndedAssociationSpec extends GormDatastoreSpec {

    @Issue('GPMONGODB-304')
    void "Test that inheritance works correctly with single ended associations"() {
        given:"An association that uses a parent class type"

            def a = new NodeA(a: 'A')
            def b = new NodeB(b: 'B', childNode: a)
            a.save()
            b.save(flush:true)
            session.clear()

        when:"The association is queried with the get method"
            def nodeB = NodeB.get(b.id)


        then:"The correct type is returned for the association"
            nodeB.childNode instanceof EntityProxy
            nodeB.childNode.target instanceof NodeA

        when:"The association is queried with a finder"
            nodeB = NodeB.findById(b.id)
        then:"The correct type is returned for the association"
            nodeB.childNode.target instanceof NodeA

//        nodeB = NodeB.findByB('B')
//        assertTrue(nodeB.childNode instanceof NodeA) // doesn't work, childNode is a Node


    }

    @Override
    List getDomainClasses() {
        [Node, NodeA, NodeB]
    }
}

@Entity
class Node {

    ObjectId id

    String name

    static constraints = {
    }

    static mapping = {
        version false
//        collection "node"
    }
}

@Entity
class NodeA extends Node {
    String a
}

@Entity
class NodeB extends Node {
    String b
    Node childNode
}