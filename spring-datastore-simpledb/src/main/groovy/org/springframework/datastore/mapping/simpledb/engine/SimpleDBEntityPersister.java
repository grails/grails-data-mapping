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
package org.springframework.datastore.mapping.simpledb.engine;

import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.DataAccessException;
import org.springframework.datastore.mapping.engine.AssociationIndexer;
import org.springframework.datastore.mapping.engine.EntityAccess;
import org.springframework.datastore.mapping.engine.NativeEntryEntityPersister;
import org.springframework.datastore.mapping.engine.PropertyValueIndexer;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.PersistentProperty;
import org.springframework.datastore.mapping.model.types.Association;
import org.springframework.datastore.mapping.proxy.EntityProxy;
import org.springframework.datastore.mapping.query.Query;
import org.springframework.datastore.mapping.simpledb.SimpleDBDatastore;
import org.springframework.datastore.mapping.simpledb.SimpleDBSession;
import org.springframework.datastore.mapping.simpledb.model.types.SimpleDBTypeConverterRegistrar;
import org.springframework.datastore.mapping.simpledb.query.SimpleDBQuery;
import org.springframework.datastore.mapping.simpledb.util.SimpleDBConverterUtil;
import org.springframework.datastore.mapping.simpledb.util.SimpleDBTemplate;

import java.io.Serializable;
import java.util.*;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;

/**
 * A {@link org.springframework.datastore.mapping.engine.EntityPersister} implementation for the SimpleDB store
 *
 * @author Roman Stepanenko based on Graeme Rocher code for MongoDb and Redis
 * @since 0.1
 */
public class SimpleDBEntityPersister extends NativeEntryEntityPersister<NativeSimpleDBItem, Object> {

    protected SimpleDBTemplate simpleDBTemplate;
    protected String entityFamily;
    protected SimpleDBDomainResolver domainResolver;
    protected boolean hasNumericalIdentifier = false;
    protected boolean hasStringIdentifier = false;

    public SimpleDBEntityPersister(MappingContext mappingContext, PersistentEntity entity,
             SimpleDBSession simpleDBSession, ApplicationEventPublisher publisher) {
        super(mappingContext, entity, simpleDBSession, publisher);
        SimpleDBDatastore datastore = (SimpleDBDatastore) simpleDBSession.getDatastore();
        simpleDBTemplate = datastore.getSimpleDBTemplate(entity);

        hasNumericalIdentifier = Long.class.isAssignableFrom(entity.getIdentity().getType());
        hasStringIdentifier = String.class.isAssignableFrom(entity.getIdentity().getType());
        SimpleDBDomainResolverFactory resolverFactory = new SimpleDBDomainResolverFactory();
        domainResolver = resolverFactory.buildResolver(entity, datastore);
    }

//    @Override
//    protected DBObject getEmbedded(DBObject nativeEntry, String key) {
//        final Object embeddedDocument = nativeEntry.get(key);
//        if (embeddedDocument instanceof DBObject) {
//            return (DBObject) embeddedDocument;
//        }
//        return null;
//    }

//    @Override
//    protected void setEmbedded(DBObject nativeEntry, String key, DBObject embeddedEntry) {
//        nativeEntry.put(key, embeddedEntry);
//    }

//    @Override
//    protected void setEmbeddedCollection(DBObject nativeEntry, String key, Collection<?> instances,
//            List<DBObject> embeddedEntries) {
//        if (instances == null || instances.isEmpty()) {
//            return;
//        }
//
//        nativeEntry.put(key, embeddedEntries.toArray());
//    }

//    @Override
//    protected void loadEmbeddedCollection(@SuppressWarnings("rawtypes") EmbeddedCollection embeddedCollection,
//            EntityAccess ea, Object embeddedInstances, String propertyKey) {
//
//        Collection<Object> instances;
//        if (List.class.isAssignableFrom(embeddedCollection.getType())) {
//            instances = new ArrayList<Object>();
//        }
//        else {
//            instances = new HashSet<Object>();
//        }
//
//        if (embeddedInstances instanceof BasicDBList) {
//            BasicDBList list = (BasicDBList)embeddedInstances;
//            for (Object dbo : list) {
//                if (dbo instanceof BasicDBObject) {
//                    BasicDBObject nativeEntry = (BasicDBObject)dbo;
//                    String embeddedClassName = (String)nativeEntry.remove("$$embeddedClassName$$");
//                    PersistentEntity embeddedPersistentEntity =
//                        getMappingContext().getPersistentEntity(embeddedClassName);
//
//                    Object instance = newEntityInstance(embeddedPersistentEntity);
//                    refreshObjectStateFromNativeEntry(embeddedPersistentEntity, instance, null, nativeEntry);
//                    instances.add(instance);
//                }
//            }
//        }
//
//        ea.setProperty(propertyKey, instances);
//    }

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
            query.in(persistentEntity.getIdentity().getName(), (List)keys);
        }
        else {
            List<Serializable> keyList = new ArrayList<Serializable>();
            for (Serializable key : keys) {
                keyList.add(key);
            }
            query.in(persistentEntity.getIdentity().getName(), keyList);
        }

        List<Object> entityResults = new ArrayList<Object>();
        Iterator<Serializable> keyIterator = keys.iterator();
        Iterator<Object> listIterator = query.list().iterator();
        while (keyIterator.hasNext() && listIterator.hasNext()) {
            Serializable key = keyIterator.next();
            Object next = listIterator.next();
            if (next instanceof NativeSimpleDBItem) {
                entityResults.add(createObjectFromNativeEntry(getPersistentEntity(), key, (NativeSimpleDBItem)next));
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
            final NativeSimpleDBItem nativeEntry) {
        return UUID.randomUUID().toString(); //todo - allow user to specify id generator using normal gorm way
    }

    @SuppressWarnings("rawtypes")
    @Override
    public PropertyValueIndexer getPropertyIndexer(PersistentProperty property) {
        // We don't need to implement this for SimpleDB since SimpleDB automatically creates indexes for us
        return null;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public AssociationIndexer getAssociationIndexer(NativeSimpleDBItem nativeEntry, Association association) {
        throw new RuntimeException("not implemented: getAssociationIndexer");
    }

    @Override
    protected NativeSimpleDBItem createNewEntry(String family) {
        return new NativeSimpleDBItem();
    }

    @Override
    protected Object getEntryValue(NativeSimpleDBItem nativeEntry, String property) {
        return nativeEntry.get(property);
    }

//    @SuppressWarnings({ "rawtypes", "unchecked" })
//    private static List<? extends Class> convertToString = Arrays.asList(
//        BigDecimal.class,
//        BigInteger.class,
//        Locale.class,
//        TimeZone.class,
//        Currency.class,
//        URL.class);

    @Override
    protected void setEntryValue(NativeSimpleDBItem nativeEntry, String key, Object value) {
       if (value != null && !getMappingContext().isPersistentEntity(value)) {
           String stringValue = SimpleDBConverterUtil.convertToString(value, getMappingContext());

           nativeEntry.put(key, stringValue);
       }
    }

    @Override
    protected EntityAccess createEntityAccess(PersistentEntity persistentEntity, Object obj, NativeSimpleDBItem nativeEntry) {
        final NativeEntryModifyingEntityAccess ea = new SimpleDBNativeEntryModifyingEntityAccess(persistentEntity, obj);
        ea.setNativeEntry(nativeEntry);
        return ea;
    }

    @Override
    protected NativeSimpleDBItem retrieveEntry(final PersistentEntity persistentEntity,
            String family, final Serializable key) {
        String domain = domainResolver.resolveDomain((String)key);
        Item item = simpleDBTemplate.get(domain, (String) key);
        return item == null ? null : new NativeSimpleDBItem(item);
    }

    @Override
    protected Object storeEntry(final PersistentEntity persistentEntity, final EntityAccess entityAccess,
                                final Object storeId, final NativeSimpleDBItem entry) {
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
            final Object key, final NativeSimpleDBItem entry) {

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
                simpleDBTemplate.deleteAttributesVersioned(domain, id, deletes, stringCurrentVersion);
                simpleDBTemplate.putAttributesVersioned(domain, id, puts, stringCurrentVersion);
            } catch (DataAccessException e) {
                //we need to restore version to what it was before the attempt to update
                ea.setProperty("version", currentVersion);
                throw new RuntimeException(e);
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
