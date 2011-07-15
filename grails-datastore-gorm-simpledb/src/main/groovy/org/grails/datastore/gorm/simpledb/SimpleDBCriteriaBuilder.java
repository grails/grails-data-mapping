/* Copyright (C) 2011 SpringSource
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
package org.grails.datastore.gorm.simpledb;

import grails.gorm.CriteriaBuilder;

import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.query.Query;

/**
 * Extends the default CriteriaBuilder implementation.
 *
 * @author Roman Stepanenko based on Graeme Rocher code for MongoDb and Redis
 * @since 0.1
 */
public class SimpleDBCriteriaBuilder extends CriteriaBuilder {

    public SimpleDBCriteriaBuilder(final Class<?> targetClass, final Session session, final Query query) {
        super(targetClass, session, query);
    }

    public SimpleDBCriteriaBuilder(final Class<?> targetClass, final Session session) {
        super(targetClass, session);
    }
}
