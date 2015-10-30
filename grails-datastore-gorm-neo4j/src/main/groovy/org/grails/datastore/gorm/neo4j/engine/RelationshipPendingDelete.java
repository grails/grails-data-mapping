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
import org.grails.datastore.mapping.model.types.Association;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a pending relationship delete
 *
 * @author Stefan
 * @author Graeme Rocher
 */
public class RelationshipPendingDelete extends PendingOperationAdapter<Object, Serializable> {

    private static Logger log = LoggerFactory.getLogger(RelationshipPendingDelete.class);

    private final GraphDatabaseService graphDatabaseService;
    private final Association association;
    private final Collection<Serializable> targetIdentifiers;


    public RelationshipPendingDelete(EntityAccess parent, Association association, Collection<Serializable> pendingInserts, GraphDatabaseService graphDatabaseService) {
        super(parent.getPersistentEntity(), (Serializable) parent.getIdentifier(), parent.getEntity());
        this.targetIdentifiers = pendingInserts;
        this.graphDatabaseService = graphDatabaseService;
        this.association = association;
    }

    @Override
    public void run() {
        final GraphPersistentEntity graphParent = (GraphPersistentEntity) getEntity();
        final GraphPersistentEntity graphChild = (GraphPersistentEntity) association.getAssociatedEntity();

        final String labelsFrom = graphParent.getLabelsAsString();
        final String labelsTo = graphChild.getLabelsAsString();
        final String relMatch = Neo4jQuery.matchForAssociation(association, "r");

        final Map<String, Object> params = new LinkedHashMap<String, Object>(2);
        params.put(CypherBuilder.START, getNativeKey());
        params.put(CypherBuilder.END, targetIdentifiers);


        StringBuilder cypherQuery = new StringBuilder("MATCH (from").append(labelsFrom).append(")").append(relMatch).append("(to").append(labelsTo).append(") WHERE ");

        final boolean nativeParent = graphParent.getIdGenerator() == null;

        if(nativeParent) {
            cypherQuery.append("ID(from) = {start}");
        }
        else {
            cypherQuery.append("from.").append(CypherBuilder.IDENTIFIER).append(" = {start}");
        }
        cypherQuery.append(" AND ");
        if(graphChild.getIdGenerator() == null) {
            cypherQuery.append(" ID(to) IN {end} ");
        }
        else {
            cypherQuery.append("to.").append(CypherBuilder.IDENTIFIER).append(" IN {end}");
        }
        cypherQuery.append(" DELETE r");

        String cypher = cypherQuery.toString();
        if (log.isDebugEnabled()) {
            log.debug("DELETE Cypher [{}] for parameters [{}]", cypher, params);
        }
        graphDatabaseService.execute(cypher, params);
    }
}
