package org.springframework.datastore.mapping.simpledb.util;

import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.ReplaceableItem;
import org.springframework.dao.DataAccessException;

import java.util.List;

/**
 * AWS SimpleDB template. This is a low-level way of accessing SimpleDB, currently is uses AWS SDK API as the return and parameter types.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public interface SimpleDBTemplate {
    Item get(String domainName, String id) throws org.springframework.dao.DataAccessException;

    void putAttributes(String domainName, String id, List<ReplaceableAttribute> attributes) throws org.springframework.dao.DataAccessException;

    /**
     * Puts attributes conditioned on the specified version - used for optimistic locking. If the specified expectedVersion does not match what is in
     * simpleDB, exception is thrown and no changes are made to the simpleDB
     * @param domainName
     * @param id
     * @param attributes
     * @param expectedVersion
     * @throws org.springframework.dao.DataAccessException
     */
    void putAttributesVersioned(String domainName, String id, List<ReplaceableAttribute> attributes, String expectedVersion) throws org.springframework.dao.DataAccessException;

    /**
     * If attributes is empty this method will do nothing - otherwise the whole item will be deleted. Use dedicated deleteItem method to delete item.
     *
     * @param domainName
     * @param id
     * @param attributes
     * @throws DataAccessException
     */
    void deleteAttributes(String domainName, String id, List<Attribute> attributes) throws DataAccessException;

    /**
     * Deletes attributes conditioned on the specified version - used for optimistic locking. If the specified expectedVersion does not match what is in
     * simpleDB, exception is thrown and no changes are made to the simpleDB.
     *
     * If attributes is empty this method will do nothing - otherwise the whole item will be deleted. Use dedicated deleteItem method to delete item.
     *
     * @param domainName
     * @param id
     * @param attributes
     * @param expectedVersion
     * @throws org.springframework.dao.DataAccessException
     */
    void deleteAttributesVersioned(String domainName, String id, List<Attribute> attributes, String expectedVersion) throws DataAccessException;

    /**
     * Deletes the specified item with all of its attributes.
     * @param domainName
     * @param id
     */
    void deleteItem(String domainName, String id);
}
