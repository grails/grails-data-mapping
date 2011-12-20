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

import java.util.LinkedList;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.simpledb.model.*;
import org.grails.datastore.mapping.core.OptimisticLockingException;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.springframework.dao.DataAccessException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;

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
        return getInternal(domainName, id, 1);
    }
    private Item getInternal(String domainName, String id, int attempt) {
        GetAttributesRequest request = new GetAttributesRequest(domainName, id);
        try {
            List<Attribute> attributes = sdb.getAttributes(request).getAttributes();
            if (attributes.isEmpty()) {
                return null;
            }

            return new Item(id, attributes);
        } catch (AmazonServiceException e) {
            if (SimpleDBUtil.AWS_ERR_CODE_NO_SUCH_DOMAIN.equals(e.getErrorCode())) {
                throw new IllegalArgumentException("no such domain: "+domainName, e);
            } else if (SimpleDBUtil.AWS_ERR_CODE_SERVICE_UNAVAILABLE.equals(e.getErrorCode())) {
                //retry after a small pause
                SimpleDBUtil.sleepBeforeRetry(attempt);
                attempt++;
                return getInternal(domainName, id, attempt);
            } else {
                throw e;
            }
        }
    }

    public Item getConsistent(String domainName, String id) {
        return getConsistentInternal(domainName, id, 1);
    }
    private Item getConsistentInternal(String domainName, String id, int attempt) {
//        String selectExpression = "select * from `" + domainName + "` where id = '"+id+"'"; //todo

        //todo - handle exceptions and retries

        GetAttributesRequest request = new GetAttributesRequest(domainName, id);
        request.setConsistentRead(true);
        try {
            List<Attribute> attributes = sdb.getAttributes(request).getAttributes();
            if (attributes.isEmpty()) {
                return null;
            }

            return new Item(id, attributes);
        } catch (AmazonServiceException e) {
            if (SimpleDBUtil.AWS_ERR_CODE_NO_SUCH_DOMAIN.equals(e.getErrorCode())) {
                throw new IllegalArgumentException("no such domain: "+domainName, e);
            } else if (SimpleDBUtil.AWS_ERR_CODE_SERVICE_UNAVAILABLE.equals(e.getErrorCode())) {
                //retry after a small pause
                SimpleDBUtil.sleepBeforeRetry(attempt);
                attempt++;
                return getConsistentInternal(domainName, id, attempt);
            } else {
                throw e;
            }
        }
    }

    public void putAttributes(String domainName, String id, List<ReplaceableAttribute> attributes) throws DataAccessException {
        putAttributesInternal(domainName, id, attributes, 1);
    }
    private void putAttributesInternal(String domainName, String id, List<ReplaceableAttribute> attributes, int attempt) throws DataAccessException {
        try {
            PutAttributesRequest request = new PutAttributesRequest(domainName, id, attributes);
            sdb.putAttributes(request);
        } catch (AmazonServiceException e) {
            if (SimpleDBUtil.AWS_ERR_CODE_NO_SUCH_DOMAIN.equals(e.getErrorCode())) {
                throw new IllegalArgumentException("no such domain: "+domainName, e);
            } else if (SimpleDBUtil.AWS_ERR_CODE_SERVICE_UNAVAILABLE.equals(e.getErrorCode())) {
                //retry after a small pause
                SimpleDBUtil.sleepBeforeRetry(attempt);
                attempt++;
                putAttributesInternal(domainName, id, attributes, attempt);
            } else {
                throw e;
            }
        }
    }

    public void putAttributesVersioned(String domainName, String id, List<ReplaceableAttribute> attributes, String expectedVersion, PersistentEntity persistentEntity) throws DataAccessException {
        putAttributesVersionedInternal(domainName, id, attributes, expectedVersion, persistentEntity, 1);
    }
    private void putAttributesVersionedInternal(String domainName, String id, List<ReplaceableAttribute> attributes, String expectedVersion, PersistentEntity persistentEntity, int attempt) throws DataAccessException {
        PutAttributesRequest request = new PutAttributesRequest(domainName, id, attributes,
                getOptimisticVersionCondition(expectedVersion));
        try {
            sdb.putAttributes(request);
        } catch (AmazonServiceException e) {
            if (SimpleDBUtil.AWS_ERR_CODE_CONDITIONAL_CHECK_FAILED.equals(e.getErrorCode())) {
                throw new OptimisticLockingException(persistentEntity, id);
            } else if (SimpleDBUtil.AWS_ERR_CODE_NO_SUCH_DOMAIN.equals(e.getErrorCode())) {
                throw new IllegalArgumentException("no such domain: " + domainName, e);
            } else if (SimpleDBUtil.AWS_ERR_CODE_SERVICE_UNAVAILABLE.equals(e.getErrorCode())) {
                //retry after a small pause
                SimpleDBUtil.sleepBeforeRetry(attempt);
                attempt++;
                putAttributesVersionedInternal(domainName, id, attributes, expectedVersion, persistentEntity, attempt);
            } else {
                throw e;
            }
        }
    }

    public void deleteAttributes(String domainName, String id, List<Attribute> attributes) throws DataAccessException {
        deleteAttributesInternal(domainName, id, attributes, 1);
    }
    private void deleteAttributesInternal(String domainName, String id, List<Attribute> attributes, int attempt) throws DataAccessException {
        if (!attributes.isEmpty()) {
            DeleteAttributesRequest request = new DeleteAttributesRequest(domainName, id, attributes);
            try {
                sdb.deleteAttributes(request);
            } catch (AmazonServiceException e) {
                if (SimpleDBUtil.AWS_ERR_CODE_NO_SUCH_DOMAIN.equals(e.getErrorCode())) {
                    throw new IllegalArgumentException("no such domain: "+domainName, e);
                } else if (SimpleDBUtil.AWS_ERR_CODE_SERVICE_UNAVAILABLE.equals(e.getErrorCode())) {
                    //retry after a small pause
                    SimpleDBUtil.sleepBeforeRetry(attempt);
                    attempt++;
                    deleteAttributesInternal(domainName, id, attributes, attempt);
                } else {
                    throw e;
                }
            }
        }
    }

    public void deleteAttributesVersioned(String domainName, String id, List<Attribute> attributes, String expectedVersion, PersistentEntity persistentEntity) throws DataAccessException {
        deleteAttributesVersionedInternal(domainName, id, attributes, expectedVersion, persistentEntity, 1);
    }
    private void deleteAttributesVersionedInternal(String domainName, String id, List<Attribute> attributes, String expectedVersion, PersistentEntity persistentEntity, int attempt) throws DataAccessException {
        // If attribute list is empty AWS api will erase the whole item.
        // Do not do that, otherwise all the callers will have to check for empty list before calling
        if (!attributes.isEmpty()) {
            DeleteAttributesRequest request = new DeleteAttributesRequest(domainName, id, attributes, getOptimisticVersionCondition(expectedVersion));
            try {
                sdb.deleteAttributes(request);
            } catch (AmazonServiceException e) {
                if (SimpleDBUtil.AWS_ERR_CODE_CONDITIONAL_CHECK_FAILED.equals(e.getErrorCode())) {
                    throw new OptimisticLockingException(persistentEntity, id);
                } else if (SimpleDBUtil.AWS_ERR_CODE_NO_SUCH_DOMAIN.equals(e.getErrorCode())) {
                    throw new IllegalArgumentException("no such domain: " + domainName, e);
                } else if (SimpleDBUtil.AWS_ERR_CODE_SERVICE_UNAVAILABLE.equals(e.getErrorCode())) {
                    //retry after a small pause
                    SimpleDBUtil.sleepBeforeRetry(attempt);
                    attempt++;
                    deleteAttributesVersionedInternal(domainName, id, attributes, expectedVersion, persistentEntity, attempt);
                } else {
                    throw e;
                }
            }
        }
    }

    public void deleteItem(String domainName, String id) {
        deleteItemInternal(domainName, id, 1);
    }
    private void deleteItemInternal(String domainName, String id, int attempt) {
        DeleteAttributesRequest request = new DeleteAttributesRequest(domainName, id);
        try {
            sdb.deleteAttributes(request);
        } catch (AmazonServiceException e) {
            if (SimpleDBUtil.AWS_ERR_CODE_NO_SUCH_DOMAIN.equals(e.getErrorCode())) {
                throw new IllegalArgumentException("no such domain: " + domainName, e);
            } else if (SimpleDBUtil.AWS_ERR_CODE_SERVICE_UNAVAILABLE.equals(e.getErrorCode())) {
                //retry after a small pause
                SimpleDBUtil.sleepBeforeRetry(attempt);
                attempt++;
                deleteItemInternal(domainName, id, attempt);
            } else {
                throw e;
            }
        }
    }

    public boolean deleteAllItems(String domainName) throws DataAccessException {
        //determine the count currently - if it is small delete items individually, otherwise just drop/create domain
        SelectRequest countRequest = new SelectRequest("select count(*) from `"+domainName+"`");
        List<Item> items = sdb.select(countRequest).getItems();
        int count = Integer.parseInt(items.get(0).getAttributes().get(0).getValue());
        if (count >= 2500) {
            deleteDomain(domainName);
            createDomain(domainName);
        } else {
            SelectRequest selectRequest = new SelectRequest("select itemName() from `"+domainName+"` limit 2500");
            items = sdb.select(selectRequest).getItems();

            for (Item item : items) {
                deleteItem(domainName, item.getName());
            }
        }
        return count > 0;
    }

    public List<Item> query(String query) {
        return queryInternal(query, 1);
    }
    private List<Item> queryInternal(String query, int attempt) {
        List<Item> items = new LinkedList<Item>();
        try {
            SelectRequest selectRequest = new SelectRequest(query);
            SelectResult result = sdb.select(selectRequest);
            items.addAll(result.getItems());

            String nextToken = null;
            do {
                nextToken = result.getNextToken();
                if (nextToken != null) {
                    selectRequest = new SelectRequest(query).withNextToken(nextToken);
                    result = sdb.select(selectRequest);
                    items.addAll(result.getItems());
                }
            } while (nextToken != null);

            return items;
        } catch (AmazonServiceException e) {
            if (SimpleDBUtil.AWS_ERR_CODE_NO_SUCH_DOMAIN.equals(e.getErrorCode())) {
                throw new IllegalArgumentException("no such domain: " + query, e);
            } else if (SimpleDBUtil.AWS_ERR_CODE_SERVICE_UNAVAILABLE.equals(e.getErrorCode())) {
                //retry after a small pause
                SimpleDBUtil.sleepBeforeRetry(attempt);
                attempt++;
                return queryInternal(query, attempt);
            } else {
                throw e;
            }
        }
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
