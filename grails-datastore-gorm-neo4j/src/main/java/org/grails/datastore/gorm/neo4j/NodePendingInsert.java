package org.grails.datastore.gorm.neo4j;

import org.grails.datastore.gorm.neo4j.engine.CypherEngine;
import org.grails.datastore.mapping.core.impl.PendingInsertAdapter;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.Simple;

import java.util.*;

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
        ea.setIdentifier(nativeKey);
    }

    @Override
    public void run() {
        Map<String, Object> simpleProps = new HashMap<String, Object>();
        simpleProps.put("__id__", nativeKey);
        for (PersistentProperty pp : getEntityAccess().getPersistentEntity().getPersistentProperties()) {
            if (pp instanceof Simple) {
                String name = pp.getName();
                Object value = getEntityAccess().getProperty(name);
                if (value != null) {
                    simpleProps.put(name, Neo4jUtils.mapToAllowedNeo4jType( value, mappingContext));
                }
            }
        }

        Map<String, List<Object>> dynamicRelProps = Neo4jGormEnhancer.amendMapWithUndeclaredProperties(simpleProps, getNativeEntry(), mappingContext);

        String labels = ((GraphPersistentEntity)entity).getLabelsWithInheritance(getEntityAccess().getEntity());
        String cypher = String.format("CREATE (n%s {1})", labels);

        cypherEngine.execute(cypher, Collections.singletonList(simpleProps));

        for (Map.Entry<String, List<Object>> e: dynamicRelProps.entrySet()) {
            for (Object o: e.getValue()) {
                GraphPersistentEntity gpe = (GraphPersistentEntity)mappingContext.getPersistentEntity(o.getClass().getName());
                cypher = String.format("MATCH (a%s {__id__:{1}}), (b%s {__id__:{2}}) MERGE (a)-[:%s]->(b)", labels, gpe.getLabelsWithInheritance(o), e.getKey() );
                List params = new ArrayList();
                params.add(nativeKey);
                params.add(new EntityAccess(gpe, o).getIdentifier());
                cypherEngine.execute(cypher, params);
            }
        }
    }

}