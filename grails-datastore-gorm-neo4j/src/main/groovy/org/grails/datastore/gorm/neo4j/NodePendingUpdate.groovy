package org.grails.datastore.gorm.neo4j

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.neo4j.engine.CypherEngine
import org.grails.datastore.mapping.core.impl.PendingUpdateAdapter
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Simple
import org.neo4j.helpers.collection.IteratorUtil

/**
 * Created by stefan on 15.02.14.
 */
@CompileStatic
class NodePendingUpdate extends PendingUpdateAdapter<Object, Long> {

    CypherEngine cypherEngine
    MappingContext mappingContext

    NodePendingUpdate(EntityAccess ea, CypherEngine cypherEngine, MappingContext mappingContext) {
        super(ea.persistentEntity, ea.getIdentifier() as Long, ea.entity, ea)
        this.cypherEngine = cypherEngine
        this.mappingContext = mappingContext
    }

    @Override
    void run() {
        def simpleProps = [:]
        for (PersistentProperty pp in entityAccess.persistentEntity.persistentProperties) {
            if (pp instanceof Simple) {
                def value = entityAccess.getProperty(pp.name)
                if (value != null) {
                    simpleProps[pp.name] = Neo4jUtils.mapToAllowedNeo4jType( value, mappingContext)
                }
            }
        }

        def labels = ((GraphPersistentEntity)entity).labelsWithInheritance
        //TODO: set n={props} might remove dynamic properties
        def cypher = "MATCH (n${labels}) WHERE id(n)={id} SET n={props} return id(n) as id"
        IteratorUtil.single(cypherEngine.execute(cypher, [props: simpleProps, id: entityAccess.getIdentifier() as Long ]))["id"] as long
    }
}