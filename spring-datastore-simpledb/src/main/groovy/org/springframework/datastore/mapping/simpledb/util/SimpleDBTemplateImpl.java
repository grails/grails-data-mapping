package org.springframework.datastore.mapping.simpledb.util;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.*;
import org.springframework.dao.DataAccessException;
import org.springframework.datastore.mapping.model.PersistentEntity;

import java.util.List;

/**
 * A template which handle one specific type of {@link org.springframework.datastore.mapping.model.PersistentEntity}.
 * 
 * @author Roman Stepanenko
 * @since 0.1
 */
public class SimpleDBTemplateImpl implements SimpleDBTemplate {
    public SimpleDBTemplateImpl(AmazonSimpleDB sdb, PersistentEntity persistentEntity) {
        this.sdb = sdb;
        this.persistentEntity = persistentEntity;
    }

    public SimpleDBTemplateImpl(String accessKey, String secretKey, PersistentEntity persistentEntity) {
        if ( accessKey == null || "".equals(accessKey) || secretKey == null || "".equals(secretKey)) {
            throw new IllegalArgumentException("Please provide accessKey and secretKey");
        }

        sdb = new AmazonSimpleDBClient(new BasicAWSCredentials(accessKey, secretKey));
        this.persistentEntity = persistentEntity;
    }

    public Item get(String domainName, String id) {
//        String selectExpression = "select * from `" + domainName + "` where id = '"+id+"'"; //todo

        //todo - handle exceptions and retries

        GetAttributesRequest request = new GetAttributesRequest(domainName, id);
        List<Attribute> attributes = sdb.getAttributes(request).getAttributes();
        if ( attributes.size() == 0 ) {
            return null;
        }

        Item item = new Item(id, attributes);

        return item;
    }

    public void putAttributes(String domainName, String id, List<ReplaceableAttribute> attributes) throws DataAccessException {
        PutAttributesRequest request = new PutAttributesRequest(domainName, id, attributes);
        sdb.putAttributes(request);
    }

    public void putAttributesVersioned(String domainName, String id, List<ReplaceableAttribute> attributes, String expectedVersion) throws DataAccessException {
        PutAttributesRequest request = new PutAttributesRequest(domainName, id, attributes,
                getOptimisticVersionCondition(expectedVersion));
        sdb.putAttributes(request);
    }

    public void deleteAttributes(String domainName, String id, List<Attribute> attributes) throws DataAccessException {
        if ( !attributes.isEmpty() ) {
            DeleteAttributesRequest request = new DeleteAttributesRequest(domainName, id, attributes);
            sdb.deleteAttributes(request);
        }
    }

    public void deleteAttributesVersioned(String domainName, String id, List<Attribute> attributes, String expectedVersion) throws DataAccessException {
        // If attribute list is empty AWS api will erase the whole item.
        // Do not do that, otherwise all the callers will have to check for empty list before calling  
        if ( !attributes.isEmpty() ) {
            DeleteAttributesRequest request = new DeleteAttributesRequest(domainName, id, attributes, getOptimisticVersionCondition(expectedVersion));
            sdb.deleteAttributes(request);
        }
    }

    public void deleteItem(String domainName, String id) {
        DeleteAttributesRequest request = new DeleteAttributesRequest(domainName, id);
        sdb.deleteAttributes(request);
    }

    public List<Item> query(String query) {
        SelectRequest selectRequest = new SelectRequest(query);
        List<Item> items = sdb.select(selectRequest).getItems();
        return items;
    }

    protected UpdateCondition getOptimisticVersionCondition(String expectedVersion) {
        return new UpdateCondition("version", expectedVersion,Boolean.TRUE);
    }

    private AmazonSimpleDB sdb;
    private PersistentEntity persistentEntity; //the entity this template represents
}
