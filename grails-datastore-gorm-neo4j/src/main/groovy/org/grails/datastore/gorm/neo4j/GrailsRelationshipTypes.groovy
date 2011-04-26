package org.grails.datastore.gorm.neo4j

import org.neo4j.graphdb.RelationshipType

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 26.04.11
 * Time: 13:09
 * To change this template use File | Settings | File Templates.
 */
enum GrailsRelationshipTypes implements RelationshipType {
    SUBREFERENCE,
    INSTANCE
}
