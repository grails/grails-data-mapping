package org.grails.datastore.gorm.neo4j

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.datastore.gorm.neo4j.engine.CypherEngine
import org.grails.datastore.mapping.core.impl.PendingInsertAdapter
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.types.Association

/**
 * Created by stefan on 15.02.14.
 */
@CompileStatic
@Slf4j
class RelationshipPendingInsert extends PendingInsertAdapter<Object, Long> {

    CypherEngine cypherEngine
    MappingContext mappingContext
    Association association
    Neo4jSession session

    RelationshipPendingInsert(EntityAccess source, Association association, CypherEngine cypherEngine, MappingContext mappingContext, Neo4jSession session) {
        super(source.persistentEntity, -1, source.entity, source)
        this.cypherEngine = cypherEngine
        this.mappingContext = mappingContext
        this.association = association
        this.session = session
    }

    @Override
    void run() {
        Long id = entityAccess.identifier as Long
        def relType = association.name.toUpperCase()

        Map<String, Collection<Long>> relationshipsMap = session.getPersistentRelationships(id)
        if (relationshipsMap == null) {
            relationshipsMap = [:]
            session.setPersistentRelationships(id, relationshipsMap)
        }

        Collection<Long> rels = relationshipsMap.get(relType)
        if (!(rels instanceof ArrayList)) { // cypher might return scala immutable collections here
            Collection<Long> existingRels = rels
            rels = new ArrayList<>()
            if (existingRels!=null) {
                rels.addAll(existingRels)
            }
            relationshipsMap.put(relType, rels)
        }

        for (target in entityAccess.getProperty(association.name)) {
            Long targetId = target["id"] as Long
            if (!(targetId in rels)) {
                cypherEngine.execute("MATCH (from), (to) WHERE id(from)={fromId} AND id(to)={toId} CREATE (from)-[:$relType]->(to)", [fromId: id, toId: targetId])
                rels.add(targetId)
            } else {
                log.debug "skip creating relationship ($id)-[:$relType]->($targetId), already exisiting"
            }
        }
    }
}
