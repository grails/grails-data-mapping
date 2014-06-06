package org.grails.datastore.gorm.neo4j;

import org.grails.datastore.gorm.neo4j.engine.CypherEngine;
import org.grails.datastore.mapping.core.OptimisticLockingException;
import org.grails.datastore.mapping.core.impl.PendingUpdateAdapter;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.Simple;
import org.neo4j.helpers.collection.IteratorUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        Object id = getEntityAccess().getIdentifier();
        simpleProps.put("__id__", id);

        PersistentEntity persistentEntity = getEntityAccess().getPersistentEntity();
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
                        simpleProps.put(name,  Neo4jUtils.mapToAllowedNeo4jType( value, mappingContext));
                    }
                }
            }
        }
        Neo4jGormEnhancer.amendMapWithUndeclaredProperties(simpleProps, getNativeEntry(), mappingContext);

        String labels = ((GraphPersistentEntity)entity).getLabelsWithInheritance();

        List params = new ArrayList(2);
        params.add(id);

        //TODO: set n={props} might remove dynamic properties
        StringBuilder cypherStringBuilder = new StringBuilder();
        cypherStringBuilder.append("MATCH (n%s) WHERE n.__id__={").append(params.size()).append("}");
        if (persistentEntity.hasProperty("version", Long.class) && persistentEntity.isVersioned()) {
            Long version = (Long) getEntityAccess().getProperty("version");
            if (version == null) {
                version = 0l;
            }
            params.add(version);
            cypherStringBuilder.append(" AND n.version={").append(params.size()).append("}");
            long newVersion = version + 1;
            simpleProps.put("version", newVersion);
            getEntityAccess().setProperty("version", newVersion);
        }
        params.add(simpleProps);
        cypherStringBuilder.append(" SET n={").append(params.size()).append("} RETURN id(n) as id");
        String cypher = String.format(cypherStringBuilder.toString(), labels);


        Map<String, Object> result = IteratorUtil.singleOrNull(cypherEngine.execute(cypher, params));
        if (result == null) {
            throw new OptimisticLockingException(persistentEntity, id);
        }

    }
}