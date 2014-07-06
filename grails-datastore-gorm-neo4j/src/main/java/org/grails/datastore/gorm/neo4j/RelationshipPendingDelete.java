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

class RelationshipPendingDelete extends PendingInsertAdapter<Object, Long> {

    private static Logger log = LoggerFactory.getLogger(RelationshipPendingDelete.class);

    private String relType;
    private CypherEngine cypherEngine;
    private EntityAccess target;

    RelationshipPendingDelete(EntityAccess source, String relType, EntityAccess target, CypherEngine cypherEngine) {
        super(source.getPersistentEntity(), -1l, source.getEntity(), source);
        this.target = target;
        this.cypherEngine = cypherEngine;
        this.relType = relType;
    }

    @Override
    public void run() {
        String labelFrom = ((GraphPersistentEntity)getEntity()).getLabel();
        String labelTo = null;
        String cypher;

        List params =  new ArrayList(2);
        params.add(getEntityAccess().getIdentifier());
        if (target!=null) {
            params.add(target.getIdentifier());
            labelTo = ((GraphPersistentEntity)target.getPersistentEntity()).getLabel();
            cypher = String.format("MATCH (from:%s {__id__: {1}})-[r:%s]->(to:%s {__id__: {2}}) DELETE r", labelFrom, relType, labelTo);
        } else {
            cypher = String.format("MATCH (from:%s {__id__: {1}})-[r:%s]->() DELETE r", labelFrom, relType);

        }
        cypherEngine.execute(cypher, params);
    }

}
