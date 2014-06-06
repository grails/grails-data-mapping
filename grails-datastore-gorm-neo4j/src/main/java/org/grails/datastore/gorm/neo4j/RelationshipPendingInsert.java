package org.grails.datastore.gorm.neo4j;

import org.grails.datastore.gorm.neo4j.engine.CypherEngine;
import org.grails.datastore.mapping.core.impl.PendingInsertAdapter;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by stefan on 15.02.14.
 */
class RelationshipPendingInsert extends PendingInsertAdapter<Object, Long> {

    private static Logger log = LoggerFactory.getLogger(RelationshipPendingInsert.class);

    private CypherEngine cypherEngine;
    private EntityAccess target;
    private String relType;

    RelationshipPendingInsert(EntityAccess source, String relType, EntityAccess target, CypherEngine cypherEngine) {
        super(source.getPersistentEntity(), -1l, source.getEntity(), source);
        this.relType = relType;
        this.target = target;
        this.cypherEngine = cypherEngine;
    }

    @Override
    public void run() {

        List params = new ArrayList();
        params.add(getEntityAccess().getIdentifier());
        params.add(target.getIdentifier());

        String labelFrom = ((GraphPersistentEntity)getEntity()).getLabel();
        String labelTo = ((GraphPersistentEntity)target.getPersistentEntity()).getLabel();
        String cypher = String.format("MATCH (from:%s {__id__:{1}}), (to:%s {__id__:{2}}) CREATE (from)-[:%s]->(to)", labelFrom, labelTo, relType);
        cypherEngine.execute(cypher, params);
    }

}
