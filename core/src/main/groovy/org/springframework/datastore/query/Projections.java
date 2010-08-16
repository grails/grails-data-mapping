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
package org.springframework.datastore.query;

/**
 * Projections used to customize the results of a query
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class Projections {
    public static final Query.IdProjection ID_PROJECTION = new Query.IdProjection();
    public static final Query.CountProjection COUNT_PROJECTION = new Query.CountProjection();

    /**
     * Projection used to obtain the id of an object
     * @return The IdProjection instance
     */
    public static Query.IdProjection id() {
        return ID_PROJECTION;
    }

    /**
     * Projection that returns the number of records from the query
     * instead of the results themselves
     *
     * @return The CountProjection instance
     */
    public static Query.CountProjection count() {
        return COUNT_PROJECTION;
    }
}
