package org.springframework.datastore.mapping.simpledb.util;

import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import org.springframework.dao.DataAccessException;

import java.util.List;

/**
 * Simple decorator used in testing to fight eventual consistency of SimpleDB.
 */
public class DelayAfterWriteSimpleDBTemplateDecorator implements SimpleDBTemplate{
    public DelayAfterWriteSimpleDBTemplateDecorator(SimpleDBTemplate template, long delayMillis) {
        this.template = template;
        this.delayMillis = delayMillis;
    }

    @Override
    public void createDomain(String domainName) throws DataAccessException {
        template.createDomain(domainName);
        pause();
    }

    @Override
    public boolean deleteAllItems(String domainName) throws DataAccessException {
        boolean result = template.deleteAllItems(domainName);
        if (result) {
            pause(); //pause only if there were items to delete
        }
        return result;
    }

    @Override
    public void deleteAttributes(String domainName, String id, List<Attribute> attributes) throws DataAccessException {
        template.deleteAttributes(domainName, id, attributes);
        pause();
    }

    @Override
    public void deleteAttributesVersioned(String domainName, String id, List<Attribute> attributes, String expectedVersion) throws DataAccessException {
        template.deleteAttributesVersioned(domainName, id, attributes, expectedVersion);
        pause();
    }

    @Override
    public void deleteDomain(String domainName) throws DataAccessException {
        template.deleteDomain(domainName);
        pause();
    }

    @Override
    public void deleteItem(String domainName, String id) throws DataAccessException {
        template.deleteItem(domainName, id);
        pause();
    }

    @Override
    public Item get(String domainName, String id) throws DataAccessException {
        return template.get(domainName, id);
    }

    @Override
    public List<String> listDomains() throws DataAccessException {
        return template.listDomains();
    }

    @Override
    public void putAttributes(String domainName, String id, List<ReplaceableAttribute> attributes) throws DataAccessException {
        template.putAttributes(domainName, id, attributes);
        pause();
    }

    @Override
    public void putAttributesVersioned(String domainName, String id, List<ReplaceableAttribute> attributes, String expectedVersion) throws DataAccessException {
        template.putAttributesVersioned(domainName, id, attributes, expectedVersion);
        pause();
    }

    @Override
    public List<Item> query(String query) throws DataAccessException {
        return template.query(query);
    }

    private void pause(){
        try { Thread.sleep(delayMillis); } catch (InterruptedException e) { }
    }

    private SimpleDBTemplate template;
    private long delayMillis;
}
