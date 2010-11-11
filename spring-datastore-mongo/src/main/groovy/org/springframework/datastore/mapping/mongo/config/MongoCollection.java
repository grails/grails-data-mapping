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

import org.springframework.datastore.mapping.document.config.Collection;

import com.mongodb.WriteConcern;


/**
 * Provides configuration options for mapping Mongo DBCollection instances
 * 
 * @author Graeme Rocher
 *
 */
public class MongoCollection extends Collection {

	private String shard;
	private String database;
	private WriteConcern writeConcern;
	
	/**
	 * @return The name of the property to shard by
	 */
	public String getShard() {
		return shard;
	}
	/**
	 * The name of the property to shard by
	 * @param shard The name of the property to shard by
	 */
	public void setShard(String shard) {
		this.shard = shard;
	}
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
	
	
}
