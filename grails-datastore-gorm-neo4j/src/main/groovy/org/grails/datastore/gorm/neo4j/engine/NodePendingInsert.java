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

import org.grails.datastore.gorm.neo4j.*;
import org.grails.datastore.mapping.core.impl.PendingInsertAdapter;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.engine.types.CustomTypeMarshaller;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.datastore.mapping.model.types.Custom;
import org.grails.datastore.mapping.model.types.Simple;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Represents a pending node insert
 *
 * @author Stefan
 * @author Graeme Rocher
 */
public class NodePendingInsert extends PendingInsertAdapter<Object, Long> {

    private static Logger log = LoggerFactory.getLogger(NodePendingUpdate.class);
    private final GraphDatabaseService graphDatabaseService;
    private final MappingContext mappingContext;

    public NodePendingInsert(Long nativeKey, EntityAccess ea, GraphDatabaseService graphDatabaseService, MappingContext mappingContext) {
        super(ea.getPersistentEntity(), nativeKey, ea.getEntity(), ea);
        this.graphDatabaseService = graphDatabaseService;
        this.mappingContext = mappingContext;
        ea.setIdentifier(nativeKey);
    }

    @Override
    public void run() {
        Map<String, Object> simpleProps = new HashMap<String, Object>();
        simpleProps.put(CypherBuilder.IDENTIFIER, nativeKey);
        final EntityAccess access = getEntityAccess();
        final PersistentEntity entity = access.getPersistentEntity();
        final Neo4jMappingContext mappingContext = (Neo4jMappingContext) this.mappingContext;
        for (PersistentProperty pp : entity.getPersistentProperties()) {
            if (pp instanceof Simple) {
                String name = pp.getName();
                Object value = access.getProperty(name);
                if (value != null) {
                    simpleProps.put(name, mappingContext.convertToNative(value));
                }
            }
            else if(pp instanceof Custom) {
                Custom<Map<String,Object>> custom = (Custom<Map<String,Object>>)pp;
                final CustomTypeMarshaller<Object, Map<String, Object>, Map<String, Object>> customTypeMarshaller = custom.getCustomTypeMarshaller();
                Object value = access.getProperty(pp.getName());
                customTypeMarshaller.write(custom, value, simpleProps);
            }
        }

        Map<String, List<Object>> dynamicRelProps = Neo4jGormEnhancer.amendMapWithUndeclaredProperties(simpleProps, getNativeEntry(), mappingContext);

        String labels = ((GraphPersistentEntity) entity).getLabelsWithInheritance(access.getEntity());
        String cypher = String.format("CREATE (n%s {props})", labels);

        final Map<String, Object> createParams = Collections.<String, Object>singletonMap(CypherBuilder.PROPS, simpleProps);
        if(log.isDebugEnabled()) {
            log.debug("Executing Create Cypher [{}] for parameters [{}]", cypher, createParams);
        }
        graphDatabaseService.execute(cypher, createParams);

        for (Map.Entry<String, List<Object>> e: dynamicRelProps.entrySet()) {
            for (Object o: e.getValue()) {
                GraphPersistentEntity gpe = (GraphPersistentEntity) mappingContext.getPersistentEntity(o.getClass().getName());
                final String labelsWithInheritance = gpe.getLabelsWithInheritance(o);
                cypher = String.format("MATCH (a%s {__id__:{id}}), (b%s {__id__:{related}}) MERGE (a)-[:%s]->(b)", labels, labelsWithInheritance, e.getKey() );
                Map<String,Object> params =  new LinkedHashMap<String, Object>(2);
                params.put(GormProperties.IDENTITY, nativeKey);
                params.put(CypherBuilder.RELATED, gpe.getMappingContext().createEntityAccess(gpe, o).getIdentifier());

                if(log.isDebugEnabled()) {
                    log.debug("Executing Merge Cypher [{}] for parameters [{}]", cypher, params);
                }
                graphDatabaseService.execute(cypher, params);
            }
        }
    }

}