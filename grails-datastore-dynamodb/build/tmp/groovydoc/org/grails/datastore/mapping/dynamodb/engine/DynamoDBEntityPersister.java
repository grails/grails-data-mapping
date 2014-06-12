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
package org.grails.datastore.mapping.dynamodb.engine;

import com.amazonaws.services.dynamodb.model.AttributeAction;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.AttributeValueUpdate;
import org.grails.datastore.mapping.cache.TPCacheAdapterRepository;
import org.grails.datastore.mapping.dynamodb.util.DynamoDBUtil;
import org.grails.datastore.mapping.engine.AssociationIndexer;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.engine.NativeEntryEntityPersister;
import org.grails.datastore.mapping.engine.PropertyValueIndexer;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.dynamodb.DynamoDBDatastore;
import org.grails.datastore.mapping.dynamodb.DynamoDBSession;
import org.grails.datastore.mapping.dynamodb.query.DynamoDBQuery;
import org.grails.datastore.mapping.dynamodb.util.DynamoDBConverterUtil;
import org.grails.datastore.mapping.dynamodb.util.DynamoDBTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;

import java.io.Serializable;
import java.util.*;

/**
 * A {@link org.grails.datastore.mapping.engine.EntityPersister} implementation for the DynamoDB store.
 *
 * @author Roman Stepanenko based on Graeme Rocher code for MongoDb and Redis
 * @since 0.1
 */
public class DynamoDBEntityPersister extends NativeEntryEntityPersister<DynamoDBNativeItem, Object> {

    protected DynamoDBTemplate dynamoDBTemplate;
    protected String entityFamily;
    protected DynamoDBTableResolver tableResolver;
    protected DynamoDBIdGenerator idGenerator;
    protected boolean hasNumericalIdentifier = false;
    protected boolean hasStringIdentifier = false;

    public DynamoDBEntityPersister(MappingContext mappingContext, PersistentEntity entity,
                                   DynamoDBSession dynamoDBSession, ApplicationEventPublisher publisher, TPCacheAdapterRepository<DynamoDBNativeItem> cacheAdapterRepository) {
        super(mappingContext, entity, dynamoDBSession, publisher, cacheAdapterRepository);
        DynamoDBDatastore datastore = (DynamoDBDatastore) dynamoDBSession.getDatastore();
        dynamoDBTemplate = datastore.getDynamoDBTemplate(entity);

        hasNumericalIdentifier = Long.class.isAssignableFrom(entity.getIdentity().getType());
        hasStringIdentifier = String.class.isAssignableFrom(entity.getIdentity().getType());
        tableResolver = datastore.getEntityDomainResolver(entity);
        idGenerator = datastore.getEntityIdGenerator(entity);
    }

    public Query createQuery() {
        return new DynamoDBQuery(getSession(), getPersistentEntity(), tableResolver, this, dynamoDBTemplate);
    }

    public DynamoDBTableResolver getTableResolver() {
        return tableResolver;
    }

    @Override
    protected boolean doesRequirePropertyIndexing() {
        return false;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected List<Object> retrieveAllEntities(PersistentEntity persistentEntity,
            Iterable<Serializable> keys) {

        Query query = session.createQuery(persistentEntity.getJavaClass());

        if (keys instanceof List) {
            if (((List)keys).isEmpty()) {
                return Collections.EMPTY_LIST;
            }
            query.in(persistentEntity.getIdentity().getName(), (List)keys);
        }
        else {
            List<Serializable> keyList = new ArrayList<Serializable>();
            for (Serializable key : keys) {
                keyList.add(key);
            }
            if (keyList.isEmpty()) {
                return Collections.EMPTY_LIST;
            }
            query.in(persistentEntity.getIdentity().getName(), keyList);
        }

        List<Object> entityResults = new ArrayList<Object>();
        Iterator<Serializable> keyIterator = keys.iterator();
        Iterator<Object> listIterator = query.list().iterator();
        while (keyIterator.hasNext() && listIterator.hasNext()) {
            Serializable key = keyIterator.next();
            Object next = listIterator.next();
            if (next instanceof DynamoDBNativeItem) {
                entityResults.add(createObjectFromNativeEntry(getPersistentEntity(), key, (DynamoDBNativeItem)next));
            }
            else {
                entityResults.add(next);
            }
        }

        return entityResults;
    }

    @Override
    protected List<Object> retrieveAllEntities(PersistentEntity persistentEntity, Serializable[] keys) {
        return retrieveAllEntities(persistentEntity, Arrays.asList(keys));
    }

    @Override
    public String getEntityFamily() {
        return entityFamily;
    }

    @Override
    protected void deleteEntry(String family, Object key, Object entry) {
        String domain = tableResolver.resolveTable((String) key);
        dynamoDBTemplate.deleteItem(domain, DynamoDBUtil.createIdKey((String) key));
    }

    @Override
    protected Object generateIdentifier(final PersistentEntity persistentEntity,
            final DynamoDBNativeItem nativeEntry) {
        return idGenerator.generateIdentifier(persistentEntity, nativeEntry);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public PropertyValueIndexer getPropertyIndexer(PersistentProperty property) {
        // We don't need to implement this for DynamoDB since DynamoDB automatically creates indexes for us
        return null;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public AssociationIndexer getAssociationIndexer(DynamoDBNativeItem nativeEntry, Association association) {
        return new DynamoDBAssociationIndexer(nativeEntry, association, (DynamoDBSession) session);
    }

    @Override
    protected DynamoDBNativeItem createNewEntry(String family) {
        return new DynamoDBNativeItem();
    }

    @Override
    protected Object getEntryValue(DynamoDBNativeItem nativeEntry, String property) {
        return nativeEntry.get(property);
    }

    @Override
    protected void setEntryValue(DynamoDBNativeItem nativeEntry, String key, Object value) {
       if (value != null && !getMappingContext().isPersistentEntity(value)) {
           String stringValue = DynamoDBConverterUtil.convertToString(value, getMappingContext());
           boolean isNumber = DynamoDBConverterUtil.isNumber(value);

           nativeEntry.put(key, stringValue, isNumber);
       }
    }

//    @Override
//    protected EntityAccess createEntityAccess(PersistentEntity persistentEntity, Object obj, DynamoDBNativeItem nativeEntry) {
//        final NativeEntryModifyingEntityAccess ea = new DynamoDBNativeEntryModifyingEntityAccess(persistentEntity, obj);
//        ea.setNativeEntry(nativeEntry);
//        return ea;
//    }

    @Override
    protected DynamoDBNativeItem retrieveEntry(final PersistentEntity persistentEntity,
            String family, final Serializable key) {
        String table = tableResolver.resolveTable((String) key);
        Map<String,AttributeValue> item = dynamoDBTemplate.get(table, DynamoDBUtil.createIdKey((String) key));
        return item == null ? null : new DynamoDBNativeItem(item);
    }

    @Override
    protected Object storeEntry(final PersistentEntity persistentEntity, final EntityAccess entityAccess,
                                final Object storeId, final DynamoDBNativeItem entry) {
        String id = storeId.toString();
        String table = tableResolver.resolveTable(id);

        Map<String, AttributeValue> allAttributes = entry.createItem();
        entry.put("id", id, false);

        dynamoDBTemplate.putItem(table, allAttributes);
        return storeId; //todo should we return string id here?
    }

    @Override
    public void updateEntry(final PersistentEntity persistentEntity, final EntityAccess ea,
            final Object key, final DynamoDBNativeItem entry) {

        String id = key.toString();
        String table = tableResolver.resolveTable(id);

        Map<String, AttributeValue> allAttributes = entry.createItem();

        Map<String, AttributeValueUpdate> updates = new HashMap<String, AttributeValueUpdate>();

        //we have to put *new* (incremented) version as part of the 'version' value and use the old version value in the conditional update.
        //if the update fails we have to restore the version to the old value
        Object currentVersion = null;
        String stringCurrentVersion = null;
        if (isVersioned(ea)) {
            currentVersion = ea.getProperty("version");
            stringCurrentVersion = convertVersionToString(currentVersion);
            incrementVersion(ea); //increment version now before we save it
        }

        for (Map.Entry<String, AttributeValue> e : allAttributes.entrySet()) {
            if ("version".equals(e.getKey())) {
                //ignore it, it will be explicitly added later right before the insert by taking incrementing and taking new one
            } else if ("id".equals(e.getKey())) {
                //ignore it, we do not want to mark it as PUT - dynamo will freak out because it is primary key (can't be updated)
            } else {
                AttributeValue av = e.getValue();
                if (av.getS() != null || av.getN() != null) {
                    updates.put(e.getKey(), new AttributeValueUpdate(av, AttributeAction.PUT));
                } else {
                    updates.put(e.getKey(), new AttributeValueUpdate(null, AttributeAction.DELETE)); //http://stackoverflow.com/questions/9142074/deleting-attribute-in-dynamodb
                }
            }
        }

        if (isVersioned(ea)) {
            putAttributeForVersion(updates, ea); //update the version
            try {
                dynamoDBTemplate.updateItemVersioned(table, DynamoDBUtil.createIdKey(id), updates, stringCurrentVersion, persistentEntity);
            } catch (DataAccessException e) {
                //we need to restore version to what it was before the attempt to update
                ea.setProperty("version", currentVersion);
                throw e;
            }
        } else {
            dynamoDBTemplate.updateItem(table, DynamoDBUtil.createIdKey(id), updates);
        }
    }

    protected void putAttributeForVersion(Map<String, AttributeValueUpdate> updates, EntityAccess ea) {
        AttributeValueUpdate attrToPut;
        Object updatedVersion = ea.getProperty("version");
        String stringUpdatedVersion = convertVersionToString(updatedVersion);
        attrToPut = new AttributeValueUpdate(new AttributeValue().withN(stringUpdatedVersion),
                AttributeAction.PUT);
        updates.put("version", attrToPut);
    }

    protected String convertVersionToString(Object currentVersion) {
        if (currentVersion == null) {
            return null;
        }

        return currentVersion.toString();
    }

    @Override
    protected void deleteEntries(String family, final List<Object> keys) {
        for (Object key : keys) {
            deleteEntry(family, key, null); //todo - optimize for bulk removal
        }
    }
}
