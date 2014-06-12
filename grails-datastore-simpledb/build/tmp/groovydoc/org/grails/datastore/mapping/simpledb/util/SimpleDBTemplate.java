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
package org.grails.datastore.mapping.simpledb.util;

import java.util.List;

import org.grails.datastore.mapping.model.PersistentEntity;
import org.springframework.dao.DataAccessException;

import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;

/**
 * AWS SimpleDB template. This is a low-level way of accessing SimpleDB,
 * currently is uses AWS SDK API as the return and parameter types.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public interface SimpleDBTemplate {

    Item get(String domainName, String id) throws DataAccessException;

    /**
     * Same as get but with consistent read flag.
     * @param domainName
     * @param id
     * @return
     * @throws DataAccessException
     */
    Item getConsistent(String domainName, String id) throws DataAccessException;

    void putAttributes(String domainName, String id, List<ReplaceableAttribute> attributes) throws DataAccessException;

    /**
     * Puts attributes conditioned on the specified version - used for optimistic
     * locking. If the specified expectedVersion does not match what is in
     * simpleDB, exception is thrown and no changes are made to the simpleDB
     *
     * @param domainName
     * @param id
     * @param attributes
     * @param expectedVersion
     * @param persistentEntity
     * @throws DataAccessException
     */
    void putAttributesVersioned(String domainName, String id, List<ReplaceableAttribute> attributes,
                                String expectedVersion, PersistentEntity persistentEntity) throws DataAccessException;

    /**
     * If attributes is empty this method will do nothing - otherwise the whole
     * item will be deleted. Use dedicated deleteItem method to delete item.
     *
     * @param domainName
     * @param id
     * @param attributes
     * @throws DataAccessException
     */
    void deleteAttributes(String domainName, String id, List<Attribute> attributes) throws DataAccessException;

    /**
     * Deletes attributes conditioned on the specified version - used for
     * optimistic locking. If the specified expectedVersion does not match what
     * is in simpleDB, exception is thrown and no changes are made to the
     * simpleDB. If attributes is empty this method will do nothing - otherwise
     * the whole item will be deleted. Use dedicated deleteItem method to delete
     * item.
     *
     * @param domainName
     * @param id
     * @param attributes
     * @param expectedVersion
     * @param persistentEntity
     * @throws DataAccessException
     */
    void deleteAttributesVersioned(String domainName, String id, List<Attribute> attributes, String expectedVersion, PersistentEntity persistentEntity)
            throws DataAccessException;

    /**
     * Deletes the specified item with all of its attributes.
     *
     * @param domainName
     * @param id
     */
    void deleteItem(String domainName, String id) throws DataAccessException;

    /**
     * Returns true if any item was deleted, in other words if domain was empty it returns false.
     * @param domainName
     * @return
     * @throws DataAccessException
     */
    boolean deleteAllItems(String domainName) throws DataAccessException;

    /**
     *
     * @param query
     * @param max maximum amount of items to return (inclusive). Note that
     * this max parameter is very different from LIMIT parameter used in the
     * actual query: LIMIT controls pretty much 'page size' while this parameter
     * control total number of items returned.
     * @return
     * @throws DataAccessException
     */
    List<Item> query(String query, int max) throws DataAccessException;

    void deleteDomain(String domainName) throws DataAccessException;

    void createDomain(String domainName) throws DataAccessException;

    List<String> listDomains() throws DataAccessException;
}
