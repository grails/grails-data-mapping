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

package org.grails.datastore.gorm.mongo;

import grails.gorm.CriteriaBuilder;

import java.util.List;
import java.util.Map;

import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.mongo.query.MongoQuery.Near;
import org.grails.datastore.mapping.mongo.query.MongoQuery.WithinBox;
import org.grails.datastore.mapping.mongo.query.MongoQuery.WithinCircle;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.api.Criteria;
import org.grails.datastore.mapping.query.api.QueryArgumentsAware;

/**
 * Extends the default CriteriaBuilder implementation with Geolocation methods
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public class MongoCriteriaBuilder extends CriteriaBuilder {

    public MongoCriteriaBuilder(final Class<?> targetClass, final Session session, final Query query) {
        super(targetClass, session, query);
    }

    public MongoCriteriaBuilder(final Class<?> targetClass, final Session session) {
        super(targetClass, session);
    }

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public Criteria near(String property, List<?> value) {
        validatePropertyName(property, "near");
        addToCriteria(new Near(property, value));
        return this;
    }

    /**
     * Geospacial query for values within a given box. A box is defined as a multi-dimensional list in the form
     *
     * [[40.73083, -73.99756], [40.741404,  -73.988135]]
     *
     * @param property The property
     * @param value A multi-dimensional list of values
     * @return this Criterion
     */
    public Criteria withinBox(String property, List<?> value) {
        validatePropertyName(property, "withinBox");
        addToCriteria(new WithinBox(property, value));
        return this;
    }

    /**
     * Geospacial query for values within a given circle. A circle is defined as a multi-dimensial list containing the position of the center and the radius:
     *
     * [[50, 50], 10]
     *
     * @param property The property
     * @param value A multi-dimensional list of values
     * @return this Criterion
     */
    public Criteria withinCircle(String property, List<?> value) {
        validatePropertyName(property, "withinBox");
        addToCriteria(new WithinCircle(property, value));
        return this;
    }

    public Criteria arguments(Map arguments) {
        ((QueryArgumentsAware)this.query).setArguments(arguments);
        return this;
    }
}
