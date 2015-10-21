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
import org.grails.datastore.gorm.neo4j.Neo4jGormEnhancer;
import org.grails.datastore.gorm.neo4j.Neo4jUtils;
import org.grails.datastore.mapping.core.OptimisticLockingException;
import org.grails.datastore.mapping.core.impl.PendingUpdateAdapter;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.datastore.mapping.model.types.Simple;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Represents a pending node update
 *
 * @author Stefan
 * @author Graeme Rocher
 */
class NodePendingUpdate extends PendingUpdateAdapter<Object, Long> {

    private static Logger log = LoggerFactory.getLogger(NodePendingUpdate.class);
    private final MappingContext mappingContext;
    private final GraphDatabaseService graphDatabaseService;

    public NodePendingUpdate(EntityAccess ea, GraphDatabaseService graphDatabaseService, MappingContext mappingContext) {
        super(ea.getPersistentEntity(), (Long) ea.getIdentifier(), ea.getEntity(), ea);
        this.graphDatabaseService = graphDatabaseService;
        this.mappingContext = mappingContext;
    }

    @Override
    public void run() {
        Map<String, Object> simpleProps = new HashMap<String, Object>();
        Object id = getEntityAccess().getIdentifier();
        simpleProps.put("__id__", id);

        PersistentEntity persistentEntity = getEntityAccess().getPersistentEntity();

        // TODO: Restore correct dirty check handling here!!

//        DirtyCheckable dirtyCheckable = null;
//        if (getNativeEntry() instanceof DirtyCheckable) {
//            dirtyCheckable = (DirtyCheckable)getNativeEntry();
//        }
        for (PersistentProperty pp : persistentEntity.getPersistentProperties()) {
            if (pp instanceof Simple) {
//                boolean needsUpdate = dirtyCheckable==null ? true : dirtyCheckable.hasChanged(pp.getName());
                boolean needsUpdate = true; // TODO: do partial node updates via SET node += {props} for Neo4j 2.1

                if (needsUpdate) {
                    String name = pp.getName();
                    Object value = getEntityAccess().getProperty(name);
                    if (value != null) { // TODO: remove property when value is null
                        simpleProps.put(name,  Neo4jUtils.mapToAllowedNeo4jType(value, mappingContext));
                    }
                }
            }
        }
        Neo4jGormEnhancer.amendMapWithUndeclaredProperties(simpleProps, getNativeEntry(), mappingContext);

        String labels = ((GraphPersistentEntity)entity).getLabelsWithInheritance(getEntityAccess().getEntity());

        Map<String,Object> params =  new LinkedHashMap<String, Object>(2);
        params.put(GormProperties.IDENTITY, id);

        //TODO: set n={props} might remove dynamic properties
        StringBuilder cypherStringBuilder = new StringBuilder();
        cypherStringBuilder.append("MATCH (n%s) WHERE n.__id__={id}");
        if (persistentEntity.hasProperty("version", Long.class) && persistentEntity.isVersioned()) {
            Long version = (Long) getEntityAccess().getProperty("version");
            if (version == null) {
                version = 0l;
            }
            params.put(GormProperties.VERSION, version);
            cypherStringBuilder.append(" AND n.version={version}");
            long newVersion = version + 1;
            simpleProps.put("version", newVersion);
            getEntityAccess().setProperty("version", newVersion);
        }
        params.put("props", simpleProps);
        cypherStringBuilder.append(" SET n={props} RETURN id(n) as id");
        String cypher = String.format(cypherStringBuilder.toString(), labels);


        if( log.isDebugEnabled() ) {
            log.debug("Executing Update Cypher [{}] for parameters [{}]", cypher, params);
        }

        final Result executionResult = graphDatabaseService.execute(cypher, params);
        Map<String, Object> result = IteratorUtil.singleOrNull(executionResult);
        if (result == null) {
            throw new OptimisticLockingException(persistentEntity, id);
        }

    }
}