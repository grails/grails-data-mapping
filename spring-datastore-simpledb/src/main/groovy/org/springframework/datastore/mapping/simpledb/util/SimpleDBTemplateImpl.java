package org.springframework.datastore.mapping.simpledb.util;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.*;
import org.springframework.dao.DataAccessException;

import java.util.List;

/**
 * Implementation of SimpleDBTemplate using AWS java sdk. 
 * 
 * @author Roman Stepanenko
 * @since 0.1
 */
public class SimpleDBTemplateImpl implements SimpleDBTemplate {
    public SimpleDBTemplateImpl(AmazonSimpleDB sdb) {
        this.sdb = sdb;
    }

    public SimpleDBTemplateImpl(String accessKey, String secretKey) {
        if ( accessKey == null || "".equals(accessKey) || secretKey == null || "".equals(secretKey)) {
            throw new IllegalArgumentException("Please provide accessKey and secretKey");
        }

        sdb = new AmazonSimpleDBClient(new BasicAWSCredentials(accessKey, secretKey));
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

    @Override
    public void deleteAllItems(String domainName) throws DataAccessException {
        SelectRequest selectRequest = new SelectRequest("select itemName() from `"+domainName+"`");
        List<Item> items = sdb.select(selectRequest).getItems();
        for (Item item : items) {
            deleteItem(domainName, item.getName());
        }
    }

    public List<Item> query(String query) {
        SelectRequest selectRequest = new SelectRequest(query);
        List<Item> items = sdb.select(selectRequest).getItems();
        return items;
    }

    public void createDomain(String domainName) throws DataAccessException {
        CreateDomainRequest request = new CreateDomainRequest(domainName);
        sdb.createDomain(request);
    }

    @Override
    public List<String> listDomains() throws DataAccessException {
        ListDomainsRequest request = new ListDomainsRequest();
        ListDomainsResult result = sdb.listDomains(request);
        return result.getDomainNames();
    }


    public void deleteDomain(String domainName) throws DataAccessException {
        DeleteDomainRequest request = new DeleteDomainRequest(domainName);
        sdb.deleteDomain(request);
    }

    protected UpdateCondition getOptimisticVersionCondition(String expectedVersion) {
        return new UpdateCondition("version", expectedVersion,Boolean.TRUE);
    }

    private AmazonSimpleDB sdb;
}
