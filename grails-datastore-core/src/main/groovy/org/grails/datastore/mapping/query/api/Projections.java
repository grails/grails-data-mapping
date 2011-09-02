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

package org.grails.datastore.mapping.query.api;


/**
 * Models a list of projections
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public interface Projections {
    /**
      * A Projection that obtains the id of an object
      * @return The projection list
      */
    Projections id();

    /**
     * Count the number of records returned
     * @return The projection list
     */
    Projections count();

    /**
     * Count the number of records returned
     * @return The projection list
     */
    Projections countDistinct(String property);

    /**
     * Projection to return only distinct records
     *
     * @return The projection list
     */
    Projections distinct();

    /**
     * Projection to return only distinct records
     *
     * @return The projection list
     */
    Projections distinct(String property);

    /**
     * Count the number of records returned
     * @return The projection list
     */
    Projections rowCount();

    /**
     * A projection that obtains the value of a property of an entity
     * @param name The name of the property
     * @return The PropertyProjection instance
     */
    Projections property(String name);

    /**
     * Computes the sum of a property
     *
     * @param name The name of the property
     * @return The PropertyProjection instance
     */
    Projections sum(String name);

    /**
     * Computes the min value of a property
     *
     * @param name The name of the property
     * @return The PropertyProjection instance
     */
    Projections min(String name);

    /**
     * Computes the max value of a property
     *
     * @param name The name of the property
     * @return The PropertyProjection instance
     */
    Projections max(String name);

    /**
     * Computes the average value of a property
     *
     * @param name The name of the property
     * @return The PropertyProjection instance
     */
    Projections avg(String name);
}
