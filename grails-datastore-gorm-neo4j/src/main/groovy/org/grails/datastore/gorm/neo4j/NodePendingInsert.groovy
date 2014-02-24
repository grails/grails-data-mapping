package org.grails.datastore.gorm.neo4j

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.neo4j.engine.CypherEngine
import org.grails.datastore.mapping.core.impl.PendingInsertAdapter
import org.grails.datastore.mapping.engine.EntityAccess
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Simple
import org.neo4j.helpers.collection.IteratorUtil

/**
 * Created by stefan on 15.02.14.
 */
@CompileStatic
class NodePendingInsert extends PendingInsertAdapter<Object, Long> {

    CypherEngine cypherEngine
    MappingContext mappingContext

    NodePendingInsert(Long nativeKey, EntityAccess ea, CypherEngine cypherEngine, MappingContext mappingContext) {
        super(ea.persistentEntity, nativeKey, ea.entity, ea)
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
        def cypher = "CREATE (n${labels} {props}) return id(n) as id"

        long id = IteratorUtil.single(cypherEngine.execute(cypher, [props: simpleProps]))["id"] as long
        entityAccess.setProperty("id", id)
    }
}