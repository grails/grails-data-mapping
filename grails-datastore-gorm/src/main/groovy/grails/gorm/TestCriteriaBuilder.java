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
package grails.gorm;

import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.api.Criteria;

/**
 * Extends the default CriteriaBuilder implementation.
 *
 * @author Guilherme Souza based on Graeme Rocher code for MongoDB
 * @since 3.1.3
 */
public class TestCriteriaBuilder extends CriteriaBuilder {

    public TestCriteriaBuilder(final Class<?> targetClass, final Session session, final Query query) {
        super(targetClass, session, query);
    }

    public TestCriteriaBuilder(final Class<?> targetClass, final Session session) {
        super(targetClass, session);
    }

   public Criteria readOnly(boolean readOnly) {
   		//no-op for now
        return this;
   }
}
