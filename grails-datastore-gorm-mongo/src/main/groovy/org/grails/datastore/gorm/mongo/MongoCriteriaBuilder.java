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

import org.springframework.datastore.mapping.core.Datastore;
import org.springframework.datastore.mapping.mongo.query.MongoQuery.Near;
import org.springframework.datastore.mapping.mongo.query.MongoQuery.WithinBox;
import org.springframework.datastore.mapping.mongo.query.MongoQuery.WithinCircle;
import org.springframework.datastore.mapping.query.Query;

/**
 * Extends the default CriteriaBuilder implementation with Geolocation methods
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class MongoCriteriaBuilder extends CriteriaBuilder {

    public MongoCriteriaBuilder(Class targetClass, Datastore datastore, Query query) {
        super(targetClass, datastore, query);
    }

    public MongoCriteriaBuilder(Class targetClass, Datastore datastore) {
        super(targetClass, datastore);
    }

    /**
     * Geospacial query for values near the given two dimensional list
     *
     * @param property The property
     * @param value A two dimensional list of values
     * @return this Criterion
     */
    public Query.Criterion near(String property, List value) {
        validatePropertyName(property, "near");
        return addToCriteria(new Near(property, value));
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
    public Query.Criterion withinBox(String property, List value) {
        validatePropertyName(property, "withinBox");
        return addToCriteria(new WithinBox(property, value));
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
    public Query.Criterion withinCircle(String property, List value) {
        validatePropertyName(property, "withinBox");
        return addToCriteria(new WithinCircle(property, value));
    }
}
