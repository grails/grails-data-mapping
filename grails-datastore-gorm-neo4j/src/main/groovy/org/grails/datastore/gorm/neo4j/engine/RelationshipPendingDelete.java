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

import org.grails.datastore.gorm.neo4j.CypherBuilder;
import org.grails.datastore.gorm.neo4j.GraphPersistentEntity;
import org.grails.datastore.mapping.core.impl.PendingInsertAdapter;
import org.grails.datastore.mapping.core.impl.PendingOperationAdapter;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a pending relationship delete
 *
 * @author Stefan
 * @author Graeme Rocher
 */
public class RelationshipPendingDelete extends PendingOperationAdapter<Object, Long> {

    public static final String CYPHER_DELETE_WITH_TARGET = "MATCH (from%s {__id__: {start}})-[r:%s]->(to%s {__id__: {end}}) DELETE r";
    public static final String CYPHER_DELETE_RELATIONSHIP = "MATCH (from%s {__id__: {start}})-[r:%s]->() DELETE r";
    private static Logger log = LoggerFactory.getLogger(RelationshipPendingDelete.class);

    private final String relType;
    private final EntityAccess target;
    private final GraphDatabaseService graphDatabaseService;

    public RelationshipPendingDelete(EntityAccess source, String relType, EntityAccess target, GraphDatabaseService graphDatabaseService) {
        super(source.getPersistentEntity(), (Long) source.getIdentifier(), source.getEntity());
        this.target = target;
        this.graphDatabaseService = graphDatabaseService;
        this.relType = relType;
    }

    @Override
    public void run() {
        String labelsFrom = ((GraphPersistentEntity) getEntity()).getLabelsAsString();
        String labelsTo;
        String cypher;

        Map<String, Object> params = new LinkedHashMap<String, Object>(2);
        params.put(CypherBuilder.START, getNativeKey());
        if (target != null) {
            params.put(CypherBuilder.END, target.getIdentifier());
            labelsTo = ((GraphPersistentEntity) target.getPersistentEntity()).getLabelsAsString();
            cypher = String.format(CYPHER_DELETE_WITH_TARGET, labelsFrom, relType, labelsTo);
        } else {
            cypher = String.format(CYPHER_DELETE_RELATIONSHIP, labelsFrom, relType);

        }
        if (log.isDebugEnabled()) {
            log.debug("DELETE Cypher [{}] for parameters [{}]", cypher, params);
        }
        graphDatabaseService.execute(cypher, params);
    }
}
