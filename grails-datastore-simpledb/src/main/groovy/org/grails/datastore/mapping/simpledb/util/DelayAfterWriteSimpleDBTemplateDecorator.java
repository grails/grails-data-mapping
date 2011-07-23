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

import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import org.springframework.dao.DataAccessException;

import java.util.List;

/**
 * Simple decorator used in testing to fight eventual consistency of SimpleDB.
 */
public class DelayAfterWriteSimpleDBTemplateDecorator implements SimpleDBTemplate {

    private SimpleDBTemplate template;
    private long delayMillis;

    public DelayAfterWriteSimpleDBTemplateDecorator(SimpleDBTemplate template, long delayMillis) {
        this.template = template;
        this.delayMillis = delayMillis;
    }

    public void createDomain(String domainName) throws DataAccessException {
        template.createDomain(domainName);
        pause();
    }

    public boolean deleteAllItems(String domainName) throws DataAccessException {
        boolean result = template.deleteAllItems(domainName);
        if (result) {
            pause(); //pause only if there were items to delete
        }
        return result;
    }

    public void deleteAttributes(String domainName, String id, List<Attribute> attributes) throws DataAccessException {
        template.deleteAttributes(domainName, id, attributes);
        pause();
    }

    public void deleteAttributesVersioned(String domainName, String id, List<Attribute> attributes, String expectedVersion) throws DataAccessException {
        template.deleteAttributesVersioned(domainName, id, attributes, expectedVersion);
        pause();
    }

    public void deleteDomain(String domainName) throws DataAccessException {
        template.deleteDomain(domainName);
        pause();
    }

    public void deleteItem(String domainName, String id) throws DataAccessException {
        template.deleteItem(domainName, id);
        pause();
    }

    public Item get(String domainName, String id) throws DataAccessException {
        return template.get(domainName, id);
    }

    public List<String> listDomains() throws DataAccessException {
        return template.listDomains();
    }

    public void putAttributes(String domainName, String id, List<ReplaceableAttribute> attributes) throws DataAccessException {
        template.putAttributes(domainName, id, attributes);
//        pause();      //for tests we use DelayAfterWriteSimpleDBSession which pauses after flush
    }

    public void putAttributesVersioned(String domainName, String id, List<ReplaceableAttribute> attributes, String expectedVersion) throws DataAccessException {
        template.putAttributesVersioned(domainName, id, attributes, expectedVersion);
//        pause();      //for tests we use DelayAfterWriteSimpleDBSession which pauses after flush
    }

    public List<Item> query(String query) throws DataAccessException {
        return template.query(query);
    }

    private void pause(){
        try { Thread.sleep(delayMillis); } catch (InterruptedException e) { /* ignored */ }
    }
}
