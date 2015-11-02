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
import org.grails.datastore.gorm.neo4j.RelationshipUtils;
import org.grails.datastore.mapping.core.impl.PendingInsertAdapter;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.model.types.ToOne;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;

/**
 * Represents a pending relationship insert
 *
 * @author Stefan
 * @author Graeme Rocher
 *
 */
public class RelationshipPendingInsert extends PendingInsertAdapter<Object, Serializable> {

    public static final String CYPHER_DELETE_RELATIONSHIP = "MATCH (from%s {"+CypherBuilder.IDENTIFIER+": {start}})%s() DELETE r";
    public static final String CYPHER_DELETE_NATIVE_RELATIONSHIP = "MATCH (from%s)%s() WHERE ID(from) = {start} DELETE r";

    private static Logger log = LoggerFactory.getLogger(RelationshipPendingInsert.class);
    private final GraphDatabaseService graphDatabaseService;
    private final Association association;
    private final Collection<Serializable> targetIdentifiers;
    private final boolean isUpdate;



    public RelationshipPendingInsert(EntityAccess parent, Association association, Collection<Serializable> pendingInserts, GraphDatabaseService graphDatabaseService, boolean isUpdate) {
        super(parent.getPersistentEntity(), -1L, parent.getEntity(), parent);

        this.graphDatabaseService = graphDatabaseService;
        this.targetIdentifiers = pendingInserts;
        this.association = association;
        this.isUpdate = isUpdate;
    }

    public RelationshipPendingInsert(EntityAccess parent, Association association, Collection<Serializable> pendingInserts, GraphDatabaseService graphDatabaseService) {
        this(parent, association, pendingInserts, graphDatabaseService, false);
    }


    @Override
    public void run() {

        Map<String,Object> params =  new LinkedHashMap<String, Object>(2);
        final Object parentId = getEntityAccess().getIdentifier();
        params.put(CypherBuilder.START, parentId);
        params.put(CypherBuilder.END, targetIdentifiers);

        final GraphPersistentEntity graphParent = (GraphPersistentEntity) getEntity();
        final GraphPersistentEntity graphChild = (GraphPersistentEntity) association.getAssociatedEntity();
        final boolean nativeParent = graphParent.getIdGenerator() == null;
        String labelsFrom = graphParent.getLabelsAsString();
        String labelsTo = graphChild.getLabelsAsString();

        final String relMatch = Neo4jQuery.matchForAssociation(association, "r");

        boolean reversed = RelationshipUtils.useReversedMappingFor(association);
        if(!reversed && (association instanceof ToOne) && isUpdate) {
            // delete any previous

            String cypher;
            if(nativeParent) {
                cypher = String.format(CYPHER_DELETE_NATIVE_RELATIONSHIP, labelsFrom, relMatch);
            }
            else {
                cypher = String.format(CYPHER_DELETE_RELATIONSHIP, labelsFrom, relMatch);
            }

            if(log.isDebugEnabled()) {
                log.debug("DELETE Cypher [{}] for parameters [{}]", cypher, params);
            }
            graphDatabaseService.execute(cypher, Collections.singletonMap(CypherBuilder.START,parentId));
        }

        StringBuilder cypherQuery = new StringBuilder("MATCH (from").append(labelsFrom).append("), (to").append(labelsTo).append(") WHERE ");

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
        cypherQuery.append(" CREATE (from)").append(relMatch).append("(to)");

        String cypher = cypherQuery.toString();

        if (log.isDebugEnabled()) {
            log.debug("CREATE Cypher [{}] for parameters [{}]", cypher, params);
        }
        graphDatabaseService.execute(cypher, params);
    }

}
