/* Copyright (C) 2010 SpringSource
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
package org.grails.datastore.gorm.neo4j

import org.grails.datastore.gorm.neo4j.engine.CypherEngine
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.grails.datastore.mapping.transactions.Transaction

/**
 * delegate tx methods to cypherEngine
 * @author Stefan Armbruster <stefan@armbruster-it.de>
 */
class Neo4jTransaction implements Transaction {

    protected final Logger log = LoggerFactory.getLogger(getClass())

    CypherEngine cypherEngine
    boolean active = true

    Neo4jTransaction(CypherEngine cypherEngine) {
        this.cypherEngine = cypherEngine
        cypherEngine.beginTx()
    }

    void commit() {
        cypherEngine.commit()
        active = false
    }

    void rollback() {
        cypherEngine.rollback()
        active = false
    }

    Object getNativeTransaction() {
        // TODO: consider returing cypherEngine
        throw new UnsupportedOperationException();
    }

    boolean isActive() {
        active
    }

    void setTimeout(int timeout) {
        throw new UnsupportedOperationException()
    }
}
