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
package org.grails.datastore.gorm.neo4j.engine

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.query.AbstractResultList
import org.neo4j.graphdb.Result


/**
 * A Neo4j result list for decoding objects from the {@link Result} interface
 *
 * @author Graeme Rocher
 * @since 5.0
 */
@CompileStatic
class Neo4jResultList extends AbstractResultList {

    final Neo4jEntityPersister entityPersister;

    Neo4jResultList(int offset, Result cursor, Neo4jEntityPersister entityPersister) {
        super(offset, (Iterator<Object>)cursor)
        this.entityPersister = entityPersister
    }

    @Override
    Result getCursor() {
        return (Result)super.getCursor()
    }

    @Override
    protected Object nextDecoded() {
        Map<String,Object> map = (Map<String,Object>) cursor.next()
        return entityPersister.unmarshallOrFromCache(entityPersister.getPersistentEntity(), map)
    }


    @Override
    void close() throws IOException {
        cursor.close()
    }
}
