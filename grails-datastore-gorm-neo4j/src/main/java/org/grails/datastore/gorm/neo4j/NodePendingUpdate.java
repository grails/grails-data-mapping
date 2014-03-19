package org.grails.datastore.gorm.neo4j;

import org.grails.datastore.gorm.neo4j.engine.CypherEngine;
import org.grails.datastore.mapping.core.impl.PendingUpdateAdapter;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.Simple;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by stefan on 15.02.14.
 */
class NodePendingUpdate extends PendingUpdateAdapter<Object, Long> {

    private CypherEngine cypherEngine;
    private MappingContext mappingContext;

    public NodePendingUpdate(EntityAccess ea, CypherEngine cypherEngine, MappingContext mappingContext) {
        super(ea.getPersistentEntity(), (Long) ea.getIdentifier(), ea.getEntity(), ea);
        this.cypherEngine = cypherEngine;
        this.mappingContext = mappingContext;
    }

    @Override
    public void run() {
        Map<String, Object> simpleProps = new HashMap<String, Object>();
        simpleProps.put("__id__", getEntityAccess().getIdentifier());

        for (PersistentProperty pp : getEntityAccess().getPersistentEntity().getPersistentProperties()) {
            if (pp instanceof Simple) {
                String name = pp.getName();
                Object value = getEntityAccess().getProperty(name);
                if (value != null) {
                    simpleProps.put(name,  Neo4jUtils.mapToAllowedNeo4jType( value, mappingContext));
                }
            }
        }

        String labels = ((GraphPersistentEntity)entity).getLabelsWithInheritance();
        //TODO: set n={props} might remove dynamic properties
        String cypher = String.format("MATCH (n%s) WHERE n.__id__={id} SET n={props}", labels);

        Map<String,Object> params = new HashMap<String, Object>();
        params.put("props", simpleProps);
        params.put("id", getEntityAccess().getIdentifier());

        cypherEngine.execute(cypher, params);
    }
}