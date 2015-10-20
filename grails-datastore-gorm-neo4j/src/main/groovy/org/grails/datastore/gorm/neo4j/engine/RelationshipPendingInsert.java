/*
 * Copyright 2015 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.datastore.gorm.neo4j.engine;

import org.grails.datastore.gorm.neo4j.GraphPersistentEntity;
import org.grails.datastore.mapping.core.impl.PendingInsertAdapter;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a pending relationship insert
 *
 * @author Stefan
 */
public class RelationshipPendingInsert extends PendingInsertAdapter<Object, Long> {

    private static Logger log = LoggerFactory.getLogger(RelationshipPendingInsert.class);

    private CypherEngine cypherEngine;
    private EntityAccess target;
    private String relType;

    public RelationshipPendingInsert(EntityAccess source, String relType, EntityAccess target, CypherEngine cypherEngine) {
        super(source.getPersistentEntity(), -1l, source.getEntity(), source);
        this.relType = relType;
        this.target = target;
        this.cypherEngine = cypherEngine;

//        validateNonExistingRelationship("constructor");

    }

/*
    private void validateNonExistingRelationship(String phase) {
        List params = new ArrayList();
        params.add(getEntityAccess().getIdentifier());
        params.add(target.getIdentifier());

        String labelFrom = ((GraphPersistentEntity)getEntity()).getLabel();
        String labelTo = ((GraphPersistentEntity)target.getPersistentEntity()).getLabel();
        String cypher = String.format("MATCH (from:%s {__id__:{1}})-[r:%s]->(to:%s {__id__:{2}}) RETURN r", labelFrom, relType, labelTo);
        CypherResult r = cypherEngine.execute(cypher, params);
        int count = IteratorUtil.count(r);
        if (count >0) {
            log.error("oops, relationship already exists during " + phase);
            //throw new IllegalStateException("oops, relationship already exists");
        }
    }
*/

    @Override
    public void run() {

//        validateNonExistingRelationship("execution");

        List params = new ArrayList();
        params.add(getEntityAccess().getIdentifier());
        params.add(target.getIdentifier());

        String labelsFrom = ((GraphPersistentEntity)getEntity()).getLabelsAsString();
        String labelsTo = ((GraphPersistentEntity)target.getPersistentEntity()).getLabelsAsString();
        String cypher = String.format("MATCH (from%s {__id__:{1}}), (to%s {__id__:{2}}) MERGE (from)-[:%s]->(to)", labelsFrom, labelsTo, relType);
        cypherEngine.execute(cypher, params);
    }

}
