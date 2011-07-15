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
package org.springframework.datastore.mapping.simpledb.util;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.DeleteAttributesRequest;
import com.amazonaws.services.simpledb.model.DeleteDomainRequest;
import com.amazonaws.services.simpledb.model.GetAttributesRequest;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.ListDomainsRequest;
import com.amazonaws.services.simpledb.model.ListDomainsResult;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.services.simpledb.model.UpdateCondition;

/**
 * Implementation of SimpleDBTemplate using AWS Java SDK.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public class SimpleDBTemplateImpl implements SimpleDBTemplate {

    private AmazonSimpleDB sdb;

    public SimpleDBTemplateImpl(AmazonSimpleDB sdb) {
        this.sdb = sdb;
    }

    public SimpleDBTemplateImpl(String accessKey, String secretKey) {
        Assert.isTrue(StringUtils.hasLength(accessKey) && StringUtils.hasLength(secretKey),
            "Please provide accessKey and secretKey");

        sdb = new AmazonSimpleDBClient(new BasicAWSCredentials(accessKey, secretKey));
    }

    public Item get(String domainName, String id) {
//        String selectExpression = "select * from `" + domainName + "` where id = '"+id+"'"; //todo

        //todo - handle exceptions and retries

        GetAttributesRequest request = new GetAttributesRequest(domainName, id);
        List<Attribute> attributes = sdb.getAttributes(request).getAttributes();
        if (attributes.isEmpty()) {
            return null;
        }

        return new Item(id, attributes);
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
        if (!attributes.isEmpty()) {
            DeleteAttributesRequest request = new DeleteAttributesRequest(domainName, id, attributes);
            sdb.deleteAttributes(request);
        }
    }

    public void deleteAttributesVersioned(String domainName, String id, List<Attribute> attributes, String expectedVersion) throws DataAccessException {
        // If attribute list is empty AWS api will erase the whole item.
        // Do not do that, otherwise all the callers will have to check for empty list before calling
        if (!attributes.isEmpty()) {
            DeleteAttributesRequest request = new DeleteAttributesRequest(domainName, id, attributes, getOptimisticVersionCondition(expectedVersion));
            sdb.deleteAttributes(request);
        }
    }

    public void deleteItem(String domainName, String id) {
        DeleteAttributesRequest request = new DeleteAttributesRequest(domainName, id);
        sdb.deleteAttributes(request);
    }

    public void deleteAllItems(String domainName) throws DataAccessException {
        SelectRequest selectRequest = new SelectRequest("select itemName() from `"+domainName+"`");
        List<Item> items = sdb.select(selectRequest).getItems();
        for (Item item : items) {
            deleteItem(domainName, item.getName());
        }
    }

    public List<Item> query(String query) {
        SelectRequest selectRequest = new SelectRequest(query);
        return sdb.select(selectRequest).getItems();
    }

    public void createDomain(String domainName) throws DataAccessException {
        CreateDomainRequest request = new CreateDomainRequest(domainName);
        sdb.createDomain(request);
    }

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
}
