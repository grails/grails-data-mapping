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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.grails.datastore.mapping.engine.AssociationIndexer;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.simpledb.SimpleDBDatastore;
import org.grails.datastore.mapping.simpledb.SimpleDBSession;
import org.grails.datastore.mapping.simpledb.util.SimpleDBUtil;

import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;

/**
 * An {@link org.grails.datastore.mapping.engine.AssociationIndexer} implementation for the SimpleDB store.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
@SuppressWarnings("rawtypes")
public class SimpleDBAssociationIndexer implements AssociationIndexer {
    public static final String FOREIGN_KEY_ATTRIBUTE_NAME = "FK";

    private Association association;
    private SimpleDBSession session;

    public SimpleDBAssociationIndexer(SimpleDBNativeItem nativeEntry, Association association, SimpleDBSession session) {
        this.association = association;
        this.session = session;
    }

    public PersistentEntity getIndexedEntity() {
        return association.getAssociatedEntity();
    }

    public void index(Object primaryKey, List foreignKeys) {
//        System.out.println("INDEX: index for id: "+primaryKey+", keys: "+foreignKeys+". entry: "+nativeEntry+", association: "+association);
        if (association.isBidirectional()) { //we use additional table only for unidirectional
            return;
        }

        SimpleDBAssociationInfo associationInfo = getDatastore().getAssociationInfo(association);
        //current implementation can not handle more than 255 ids because we store them in a multi-value attribute
        //and key this collection by the primary key of the entity
        List<ReplaceableAttribute> attributes = new LinkedList<ReplaceableAttribute>();
        for (Object foreignKey : foreignKeys) {
            attributes.add(new ReplaceableAttribute(FOREIGN_KEY_ATTRIBUTE_NAME, foreignKey.toString(), Boolean.TRUE));
        }
        session.getSimpleDBTemplate().putAttributes(associationInfo.getDomainName(), primaryKey.toString(), attributes);
    }

    public List query(Object primaryKey) {
//        System.out.println("INDEX: query for id: "+primaryKey+". entry: "+nativeEntry+", association: "+association);
        if (!association.isBidirectional()) { //we use additional table only for unidirectional
            SimpleDBAssociationInfo associationInfo = getDatastore().getAssociationInfo(association);
            String query = "SELECT * FROM "+ SimpleDBUtil.quoteName(associationInfo.getDomainName())+" WHERE itemName() = " + SimpleDBUtil.quoteValue(primaryKey.toString())+" LIMIT 2500";
            List<Item> items = session.getSimpleDBTemplate().query(query, Integer.MAX_VALUE);
            if (items.isEmpty()) {
                return Collections.EMPTY_LIST;
            } else if (items.size() > 1) {
                throw new IllegalArgumentException("current implementation stores all foreign keys in a single item, if more than one item is returned it is a data corruption");
            } else {
                return SimpleDBUtil.collectAttributeValues(items.get(0), FOREIGN_KEY_ATTRIBUTE_NAME);
            }
        }

        //for bidirectional onToMany association the use the other entity to refer to this guy's PK via FK
        SimpleDBDomainResolver domainResolver = getDatastore().getEntityDomainResolver(association.getAssociatedEntity());

        //todo - implement for sharding (must look in all shards)
        String query = "SELECT itemName() FROM "+ SimpleDBUtil.quoteName(domainResolver.getAllDomainsForEntity().get(0)) +
                " WHERE " + SimpleDBUtil.quoteName(association.getInverseSide().getName()) +
                " = " + SimpleDBUtil.quoteValue(primaryKey.toString()) + " LIMIT 2500";

        List<Item> items = session.getSimpleDBTemplate().query(query, Integer.MAX_VALUE);
        if (items.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        return SimpleDBUtil.collectItemNames(items);
    }

    private SimpleDBDatastore getDatastore() {
        return ((SimpleDBDatastore) session.getDatastore());
    }

    public void index(Object primaryKey, Object foreignKey) {
//        System.out.println("INDEX: index for id: "+primaryKey+", KEY: "+foreignKey+". entry: "+nativeEntry+", association: "+association);
        if (association.isBidirectional()) { //we use additional table only for unidirectional
            return;
        }

        throw new RuntimeException("not implemented: index(Object primaryKey, Object foreignKey)");
    }
}
