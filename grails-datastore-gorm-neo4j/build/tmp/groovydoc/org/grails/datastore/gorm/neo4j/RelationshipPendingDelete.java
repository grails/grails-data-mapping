package org.grails.datastore.gorm.neo4j;

import org.grails.datastore.gorm.neo4j.engine.CypherEngine;
import org.grails.datastore.mapping.core.impl.PendingInsertAdapter;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.model.types.Association;
import org.neo4j.helpers.collection.MapUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by stefan on 15.02.14.
 */

class RelationshipPendingDelete extends PendingInsertAdapter<Object, Long> {

    private static Logger log = LoggerFactory.getLogger(RelationshipPendingDelete.class);

    private String relType;
    private CypherEngine cypherEngine;
    private EntityAccess target;

    RelationshipPendingDelete(EntityAccess source, String relType, EntityAccess target, CypherEngine cypherEngine) {
        super(source.getPersistentEntity(), -1l, source.getEntity(), source);
        this.target = target;
        this.cypherEngine = cypherEngine;
    }

    @Override
    public void run() {
        Map<String,Object> params = MapUtil.map(
                "fromId", getEntityAccess().getIdentifier(),
                "toId", target.getIdentifier()
        );
        String cypher = String.format("MATCH (from)-[r:%s]->(to) WHERE from.__id__={fromId} AND to.__id__={toId} DELETE r", relType);
        cypherEngine.execute(cypher, params);
    }

}
