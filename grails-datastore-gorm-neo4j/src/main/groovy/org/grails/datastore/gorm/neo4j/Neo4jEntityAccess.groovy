package org.grails.datastore.gorm.neo4j

import org.springframework.datastore.mapping.engine.NativeEntryEntityPersister.NativeEntryModifyingEntityAccess
import org.springframework.datastore.mapping.model.PersistentEntity

class Neo4jEntityAccess extends NativeEntryModifyingEntityAccess {

    public Neo4jEntityAccess(PersistentEntity persistentEntity, Object entity) {
        super(persistentEntity, entity)
    }

    @Override
    public void setProperty(String name, Object value) {

        if (getProperty(name)!=value) {
            super.setProperty(name, value)
        }
    }
}
