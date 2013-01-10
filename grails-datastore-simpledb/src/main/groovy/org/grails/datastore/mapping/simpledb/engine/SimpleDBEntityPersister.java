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
package org.grails.datastore.mapping.simpledb.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.grails.datastore.mapping.cache.TPCacheAdapterRepository;
import org.grails.datastore.mapping.engine.AssociationIndexer;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.engine.NativeEntryEntityPersister;
import org.grails.datastore.mapping.engine.PropertyValueIndexer;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.simpledb.SimpleDBDatastore;
import org.grails.datastore.mapping.simpledb.SimpleDBSession;
import org.grails.datastore.mapping.simpledb.model.types.SimpleDBTypeConverterRegistrar;
import org.grails.datastore.mapping.simpledb.query.SimpleDBQuery;
import org.grails.datastore.mapping.simpledb.util.SimpleDBConverterUtil;
import org.grails.datastore.mapping.simpledb.util.SimpleDBTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;

import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;

/**
 * A {@link org.grails.datastore.mapping.engine.EntityPersister} implementation for the SimpleDB store.
 *
 * @author Roman Stepanenko based on Graeme Rocher code for MongoDb and Redis
 * @since 0.1
 */
public class SimpleDBEntityPersister extends NativeEntryEntityPersister<SimpleDBNativeItem, Object> {

    protected SimpleDBTemplate simpleDBTemplate;
    protected String entityFamily;
    protected SimpleDBDomainResolver domainResolver;
    protected SimpleDBIdGenerator idGenerator;
    protected boolean hasNumericalIdentifier = false;
    protected boolean hasStringIdentifier = false;

    public SimpleDBEntityPersister(MappingContext mappingContext, PersistentEntity entity,
             SimpleDBSession simpleDBSession, ApplicationEventPublisher publisher, TPCacheAdapterRepository<SimpleDBNativeItem> cacheAdapterRepository) {
        super(mappingContext, entity, simpleDBSession, publisher, cacheAdapterRepository);
        SimpleDBDatastore datastore = (SimpleDBDatastore) simpleDBSession.getDatastore();
        simpleDBTemplate = datastore.getSimpleDBTemplate(entity);

        hasNumericalIdentifier = Long.class.isAssignableFrom(entity.getIdentity().getType());
        hasStringIdentifier = String.class.isAssignableFrom(entity.getIdentity().getType());
        domainResolver = datastore.getEntityDomainResolver(entity);
        idGenerator = datastore.getEntityIdGenerator(entity);
    }

    public Query createQuery() {
        return new SimpleDBQuery(getSession(), getPersistentEntity(), domainResolver, this, simpleDBTemplate);
    }

    public SimpleDBDomainResolver getDomainResolver() {
        return domainResolver;
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
            if (next instanceof SimpleDBNativeItem) {
                entityResults.add(createObjectFromNativeEntry(getPersistentEntity(), key, (SimpleDBNativeItem)next));
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
        String domain = domainResolver.resolveDomain((String)key);
        simpleDBTemplate.deleteItem(domain, (String) key);
    }

    @Override
    protected Object generateIdentifier(final PersistentEntity persistentEntity,
            final SimpleDBNativeItem nativeEntry) {
        return idGenerator.generateIdentifier(persistentEntity, nativeEntry);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public PropertyValueIndexer getPropertyIndexer(PersistentProperty property) {
        // We don't need to implement this for SimpleDB since SimpleDB automatically creates indexes for us
        return null;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public AssociationIndexer getAssociationIndexer(SimpleDBNativeItem nativeEntry, Association association) {
        return new SimpleDBAssociationIndexer(nativeEntry, association, (SimpleDBSession) session);
    }

    @Override
    protected SimpleDBNativeItem createNewEntry(String family) {
        return new SimpleDBNativeItem();
    }

    @Override
    protected Object getEntryValue(SimpleDBNativeItem nativeEntry, String property) {
        return nativeEntry.get(property);
    }

    @Override
    protected void setEntryValue(SimpleDBNativeItem nativeEntry, String key, Object value) {
       if (!getMappingContext().isPersistentEntity(value)) {
           String stringValue = SimpleDBConverterUtil.convertToString(value, getMappingContext());

           nativeEntry.put(key, stringValue);
       }
    }

    @Override
    protected EntityAccess createEntityAccess(PersistentEntity persistentEntity, Object obj, SimpleDBNativeItem nativeEntry) {
        final NativeEntryModifyingEntityAccess ea = new SimpleDBNativeEntryModifyingEntityAccess(persistentEntity, obj);
        ea.setNativeEntry(nativeEntry);
        return ea;
    }

    @Override
    protected SimpleDBNativeItem retrieveEntry(final PersistentEntity persistentEntity,
            String family, final Serializable key) {
        String domain = domainResolver.resolveDomain((String)key);
        Item item = simpleDBTemplate.get(domain, (String) key);
        return item == null ? null : new SimpleDBNativeItem(item);
    }

    @Override
    protected Object storeEntry(final PersistentEntity persistentEntity, final EntityAccess entityAccess,
                                final Object storeId, final SimpleDBNativeItem entry) {
        String id = storeId.toString();
        String domain = domainResolver.resolveDomain(id);

        //for non-null values we should call putAttributes, for nulls we should just do nothing during creation
        List<ReplaceableAttribute> allAttributes = entry.createReplaceableItem().getAttributes();

        List<ReplaceableAttribute> puts = new LinkedList<ReplaceableAttribute>();
        for (ReplaceableAttribute attribute : allAttributes) {
            if (attribute.getValue() != null) {
                puts.add(attribute);
            }
        }

        simpleDBTemplate.putAttributes(domain, id, puts);
        return storeId; //todo should we return string id here?
    }

    @Override
    public void updateEntry(final PersistentEntity persistentEntity, final EntityAccess ea,
            final Object key, final SimpleDBNativeItem entry) {

        String id = key.toString();
        String domain = domainResolver.resolveDomain(id);

        //for non-null values we should call putAttributes, for nulls we should call delete attributes
        List<ReplaceableAttribute> allAttributes = entry.createReplaceableItem().getAttributes();

        List<ReplaceableAttribute> puts = new LinkedList<ReplaceableAttribute>();
        List<Attribute> deletes = new LinkedList<Attribute>();

        //we have to put *new* (incremented) version as part of the 'version' value and use the old version value in the conditional update.
        //if the update fails we have to restore the version to the old value
        Object currentVersion = null;
        String stringCurrentVersion = null;
        if (isVersioned(ea)) {
            currentVersion = ea.getProperty("version");
            stringCurrentVersion = convertVersionToString(currentVersion);
            incrementVersion(ea); //increment version now before we save it
        }

        for (ReplaceableAttribute attribute : allAttributes) {
            if ("version".equals(attribute.getName())) {
                //ignore it, it will be explicitly added later right before the insert by taking incrementing and taking new one
            } else {
                if (attribute.getValue() != null) {
                    puts.add(attribute);
                } else {
                    deletes.add(new Attribute(attribute.getName(), null));
                }
            }
        }

        if (isVersioned(ea)) {
            puts.add(createAttributeForVersion(ea)); //update the version
            try {
                simpleDBTemplate.deleteAttributesVersioned(domain, id, deletes, stringCurrentVersion, persistentEntity);
                simpleDBTemplate.putAttributesVersioned(domain, id, puts, stringCurrentVersion, persistentEntity);
            } catch (DataAccessException e) {
                //we need to restore version to what it was before the attempt to update
                ea.setProperty("version", currentVersion);
                throw e;
            }
        } else {
            simpleDBTemplate.deleteAttributes(domain, id, deletes);
            simpleDBTemplate.putAttributes(domain, id, puts);
        }
    }

    protected ReplaceableAttribute createAttributeForVersion(EntityAccess ea) {
        ReplaceableAttribute attrToPut;
        Object updatedVersion = ea.getProperty("version");
        String stringUpdatedVersion = convertVersionToString(updatedVersion);
        attrToPut = new ReplaceableAttribute("version", stringUpdatedVersion,
                Boolean.TRUE);
        return attrToPut;
    }

    protected String convertVersionToString(Object currentVersion) {
        if (currentVersion == null) {
            return null;
        }

        if (currentVersion instanceof Long) {
            return SimpleDBTypeConverterRegistrar.LONG_TO_STRING_CONVERTER.convert((Long) currentVersion);
        }

        return currentVersion.toString();
    }

    @Override
    protected void deleteEntries(String family, final List<Object> keys) {
        for (Object key : keys) {
            deleteEntry(family, key, null); //todo - optimize for bulk removal
        }
    }

    /**
     * Provides proper conversion of 'version' field from Integer to Long - since we use numeric padding in SimpleDB
     * there are different amounts of zeros for Long and Integer - and it matters for optimistic locking assertions
     * on SimpleDB level.
     */
    protected class SimpleDBNativeEntryModifyingEntityAccess extends NativeEntryModifyingEntityAccess {
        public SimpleDBNativeEntryModifyingEntityAccess(PersistentEntity persistentEntity, Object entity) {
            super(persistentEntity, entity);
        }

        @Override
        public void setProperty(String name, Object value) {
            if ("version".equals(name) && value instanceof Integer) {
                value = ((Integer)value).longValue();
            }
            super.setProperty(name, value);
        }
    }
}
