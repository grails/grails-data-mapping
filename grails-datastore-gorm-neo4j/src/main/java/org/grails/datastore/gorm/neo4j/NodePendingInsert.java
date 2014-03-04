package org.grails.datastore.gorm.neo4j;

import org.grails.datastore.gorm.neo4j.engine.CypherEngine;
import org.grails.datastore.mapping.core.impl.PendingInsertAdapter;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.Simple;
import org.neo4j.helpers.collection.IteratorUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by stefan on 15.02.14.
 */
class NodePendingInsert extends PendingInsertAdapter<Object, Long> {

    private CypherEngine cypherEngine;
    private MappingContext mappingContext;

    public NodePendingInsert(Long nativeKey, EntityAccess ea, CypherEngine cypherEngine, MappingContext mappingContext) {
        super(ea.getPersistentEntity(), nativeKey, ea.getEntity(), ea);
        this.cypherEngine = cypherEngine;
        this.mappingContext = mappingContext;
    }

    @Override
    public void run() {
        Map<String, Object> simpleProps = new HashMap<String, Object>();
        for (PersistentProperty pp : getEntityAccess().getPersistentEntity().getPersistentProperties()) {
            if (pp instanceof Simple) {
                String name = pp.getName();
                Object value = getEntityAccess().getProperty(name);
                if (value != null) {
                    simpleProps.put(name, Neo4jUtils.mapToAllowedNeo4jType( value, mappingContext));
                }
            }
        }

        String labels = ((GraphPersistentEntity)entity).getLabelsWithInheritance();
        String cypher = String.format("CREATE (n%s {props}) return id(n) as id", labels);

        Object id = IteratorUtil.single(cypherEngine.execute(cypher, Collections.singletonMap("props", simpleProps))).get("id");
        getEntityAccess().setIdentifier(id);
    }
}