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
package org.springframework.datastore.mapping.mongo.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.datastore.mapping.document.config.Collection;

import com.mongodb.WriteConcern;

/**
 * Provides configuration options for mapping Mongo DBCollection instances
 *
 * @author Graeme Rocher
 */
@SuppressWarnings("rawtypes")
public class MongoCollection extends Collection {

    private String database;
    private WriteConcern writeConcern;
    private List<Map> compoundIndices = new ArrayList<Map>();

    /**
     * The database to use
     *
     * @return The name of the database
     */
    public String getDatabase() {
        return database;
    }
    /**
     * The name of the database to use
     *
     * @param database The database
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * @return The {@link WriteConcern} for the collection
     */
    public WriteConcern getWriteConcern() {
        return writeConcern;
    }

    /**
     * The {@link WriteConcern} for the collection
     */
    public void setWriteConcern(WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
    }

    /**
     * Sets a compound index definition
     *
     * @param compoundIndex The compount index
     */
    public void setCompoundIndex(Map compoundIndex) {
        if (compoundIndex != null) {
            compoundIndices.add(compoundIndex);
        }
    }

    /**
     * Return all defined compound indices
     *
     * @return The compound indices to return
     */
    public List<Map> getCompoundIndices() {
        return compoundIndices;
    }
}
