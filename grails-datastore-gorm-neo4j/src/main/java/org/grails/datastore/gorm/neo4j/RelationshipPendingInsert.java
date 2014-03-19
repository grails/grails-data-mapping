package org.grails.datastore.gorm.neo4j;

import org.grails.datastore.gorm.neo4j.engine.CypherEngine;
import org.grails.datastore.mapping.core.impl.PendingInsertAdapter;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.types.Association;
import org.neo4j.helpers.collection.MapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Created by stefan on 15.02.14.
 */
class RelationshipPendingInsert extends PendingInsertAdapter<Object, Long> {

    private static Logger log = LoggerFactory.getLogger(RelationshipPendingInsert.class);

    private CypherEngine cypherEngine;
    private MappingContext mappingContext;
    private Association association;
    private Neo4jSession session;

    RelationshipPendingInsert(EntityAccess source, Association association, CypherEngine cypherEngine, MappingContext mappingContext, Neo4jSession session) {
        super(source.getPersistentEntity(), -1l, source.getEntity(), source);
        this.cypherEngine = cypherEngine;
        this.mappingContext = mappingContext;
        this.association = association;
        this.session = session;
    }

    @Override
    public void run() {
        Long id = (Long)getEntityAccess().getIdentifier();

        boolean reversed = RelationshipUtils.useReversedMappingFor(association);
        String relType = RelationshipUtils.relationshipTypeUsedFor(association);

        Object targets = getEntityAccess().getProperty(association.getName());
        if (!(targets instanceof Collection)) {
            targets = Collections.singletonList(targets);
        }

        for (Object target: (Collection) targets) {

            EntityAccess targetEntityAccess = new EntityAccess(association.getOwner(), target);
            Long targetId = (Long)targetEntityAccess.getIdentifier();

            Long fromId = id;
            Long toId = targetId;

            if (reversed) {
                fromId = targetId;
                toId = id;
            }

            if (!session.containsOrAddPersistentRelationship(fromId, toId, relType)) {
                Map<String,Object> params = MapUtil.map("fromId", fromId, "toId", toId);
                String cypher = String.format("MATCH (from), (to) WHERE from.__id__={fromId} AND to.__id__={toId} CREATE (from)-[:%s]->(to)", relType);
                cypherEngine.execute(cypher, params);
            } else {
                log.debug(String.format("skip creating relationship (%d)-[:%s]->(%d), already exisiting", fromId, relType, toId));
            }
        }
    }

}
