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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.grails.datastore.mapping.dynamodb.DynamoDBDatastore;
import org.grails.datastore.mapping.dynamodb.DynamoDBSession;
import org.grails.datastore.mapping.dynamodb.util.DynamoDBUtil;
import org.grails.datastore.mapping.engine.AssociationIndexer;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.types.Association;

import com.amazonaws.services.dynamodb.model.AttributeAction;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodb.model.ComparisonOperator;
import com.amazonaws.services.dynamodb.model.Condition;

/**
 * An {@link org.grails.datastore.mapping.engine.AssociationIndexer} implementation for the DynamoDB store.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
@SuppressWarnings("rawtypes")
public class DynamoDBAssociationIndexer implements AssociationIndexer {
    public static final String FOREIGN_KEY_ATTRIBUTE_NAME = "FK";

    private Association association;
    private DynamoDBSession session;

    public DynamoDBAssociationIndexer(DynamoDBNativeItem nativeEntry, Association association, DynamoDBSession session) {
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

        DynamoDBAssociationInfo associationInfo = getDatastore().getAssociationInfo(association);
        //we store them in a multi-value attribute
        //and key this collection by the primary key of the entity

        Map<String, AttributeValueUpdate> updateItems = new HashMap<String, AttributeValueUpdate>();
        //collect all foreign keys into string list
        List<String> fks = new ArrayList<String>();
        for (Object foreignKey : foreignKeys) {
            fks.add(foreignKey.toString());
        }

        if (fks.isEmpty()) {
            //we can't create dynamodb entry without any attributes, so we must kill it
            session.getDynamoDBTemplate().deleteItem(associationInfo.getTableName(), DynamoDBUtil.createIdKey(primaryKey.toString()));
        } else {
            updateItems.put(FOREIGN_KEY_ATTRIBUTE_NAME,
                    new AttributeValueUpdate()
                            .withAction(AttributeAction.PUT)
                            .withValue(new AttributeValue().withSS(fks)));
            session.getDynamoDBTemplate().updateItem(associationInfo.getTableName(), DynamoDBUtil.createIdKey(primaryKey.toString()), updateItems);
        }

    }

    public List query(Object primaryKey) {
//        System.out.println("INDEX: query for id: "+primaryKey+". entry: "+nativeEntry+", association: "+association);
        if (!association.isBidirectional()) { //we use additional table only for unidirectional
            DynamoDBAssociationInfo associationInfo = getDatastore().getAssociationInfo(association);
            Map<String, AttributeValue> item = session.getDynamoDBTemplate().get(associationInfo.getTableName(), DynamoDBUtil.createIdKey(primaryKey.toString()));
            if (item == null) {
                return Collections.EMPTY_LIST;
            } else {
                return DynamoDBUtil.getAttributeValues(item, FOREIGN_KEY_ATTRIBUTE_NAME);
            }
        }

        //for bidirectional onToMany association the use the other entity to refer to this guy's PK via FK
        DynamoDBTableResolver tableResolver = getDatastore().getEntityDomainResolver(association.getAssociatedEntity());

        Map<String, Condition> filter = new HashMap<String, Condition>();
        DynamoDBUtil.addSimpleComparison(filter,
                association.getInverseSide().getName(),
                ComparisonOperator.EQ.toString(),
                primaryKey.toString(), false);

        List<Map<String, AttributeValue>> items = session.getDynamoDBTemplate().scan(tableResolver.getAllTablesForEntity().get(0), filter, Integer.MAX_VALUE);
        if (items.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        return DynamoDBUtil.collectIds(items);
    }

    private DynamoDBDatastore getDatastore() {
        return ((DynamoDBDatastore) session.getDatastore());
    }

    public void index(Object primaryKey, Object foreignKey) {
//        System.out.println("INDEX: index for id: "+primaryKey+", KEY: "+foreignKey+". entry: "+nativeEntry+", association: "+association);
        if (association.isBidirectional()) { //we use additional table only for unidirectional
            return;
        }

        throw new RuntimeException("not implemented: index(Object primaryKey, Object foreignKey)");
    }
}
