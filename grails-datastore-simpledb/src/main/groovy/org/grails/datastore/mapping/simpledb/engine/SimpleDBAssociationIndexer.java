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

import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import org.grails.datastore.mapping.engine.AssociationIndexer;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.simpledb.SimpleDBDatastore;
import org.grails.datastore.mapping.simpledb.SimpleDBSession;
import org.grails.datastore.mapping.simpledb.util.SimpleDBUtil;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * An {@link org.grails.datastore.mapping.engine.AssociationIndexer} implementation for the SimpleDB store
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
@SuppressWarnings("rawtypes")
public class SimpleDBAssociationIndexer implements AssociationIndexer {
    public static final String FOREIGN_KEY_ATTRIBUTE_NAME = "FK";

    private SimpleDBNativeItem nativeEntry;
    private Association association;
    private SimpleDBSession session;

    public SimpleDBAssociationIndexer(SimpleDBNativeItem nativeEntry, Association association, SimpleDBSession session) {
        this.nativeEntry = nativeEntry;
        this.association = association;
        this.session = session;
    }

    @Override
    public PersistentEntity getIndexedEntity() {
        return association.getAssociatedEntity();
    }

    @Override
    public void index(Object primaryKey, List foreignKeys) {
//        System.out.println("INDEX: index for id: "+primaryKey+", keys: "+foreignKeys+". entry: "+nativeEntry+", association: "+association);
        if (!association.isBidirectional()) { //we use additional table only for unidirectional
            SimpleDBAssociationInfo associationInfo = ((SimpleDBDatastore) session.getDatastore()).getAssociationInfo(association);
            //current implementation can not handle more than 255 ids because we store them in a multi-value attribute
            //and key this collection by the primary key of the entity
            List<ReplaceableAttribute> attributes = new LinkedList<ReplaceableAttribute>();
            for (Object foreignKey : foreignKeys) {
                attributes.add(new ReplaceableAttribute(FOREIGN_KEY_ATTRIBUTE_NAME, foreignKey.toString(), Boolean.TRUE));
            }
            session.getSimpleDBTemplate().putAttributes(associationInfo.getDomainName(), primaryKey.toString(), attributes);
        }
    }

    @Override
    public List query(Object primaryKey) {
//        System.out.println("INDEX: query for id: "+primaryKey+". entry: "+nativeEntry+", association: "+association);
        if (!association.isBidirectional()) { //we use additional table only for unidirectional
            SimpleDBAssociationInfo associationInfo = ((SimpleDBDatastore) session.getDatastore()).getAssociationInfo(association);
            String query = "select * from "+ SimpleDBUtil.quoteName(associationInfo.getDomainName())+" where itemName() = " + SimpleDBUtil.quoteValue(primaryKey.toString());
            List<Item> items = session.getSimpleDBTemplate().query(query);
            if (items.isEmpty()) {
                return Collections.EMPTY_LIST;
            } else if (items.size() > 1) {
                throw new IllegalArgumentException("current implementation stores all foreign keys in a single item, if more than one item is returned it is a data corruption");
            } else {
                Item item = items.get(0);
                List ids = new LinkedList();
                for (Attribute attribute : item.getAttributes()) {
                    if (FOREIGN_KEY_ATTRIBUTE_NAME.equals(attribute.getName())) {
                        ids.add(attribute.getValue());
                    }
                }
                return ids;
            }
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    @Override
    public void index(Object primaryKey, Object foreignKey) {
//        System.out.println("INDEX: index for id: "+primaryKey+", KEY: "+foreignKey+". entry: "+nativeEntry+", association: "+association);
        if (!association.isBidirectional()) { //we use additional table only for unidirectional
            throw new RuntimeException("not implemented: index(Object primaryKey, Object foreignKey)");
        }
    }
}
