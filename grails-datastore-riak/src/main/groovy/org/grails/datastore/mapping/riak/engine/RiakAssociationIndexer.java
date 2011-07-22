/*
 * Copyright (c) 2010 by J. Brisbin <jon@jbrisbin.com>
 *     Portions (c) 2010 by NPC International, Inc. or the
 *     original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grails.datastore.mapping.riak.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.keyvalue.riak.core.RiakTemplate;
import org.grails.datastore.mapping.engine.AssociationIndexer;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.types.Association;

/**
 * @author J. Brisbin <jon@jbrisbin.com>
 */
@SuppressWarnings({"unchecked"})
public class RiakAssociationIndexer implements AssociationIndexer<Long, Long> {

    @SuppressWarnings("unused")
    private static final Pattern linkPattern = Pattern.compile(
            "^<(.+)/(.+)/(.+)>; riaktag=\"(.+)\"");
    private RiakTemplate riakTemplate;
    @SuppressWarnings("unused")
    private ConversionService conversionService;
    private Association association;
    PersistentEntity owner;
    PersistentEntity child;

    public RiakAssociationIndexer(RiakTemplate riakTemplate, ConversionService conversionService, Association association) {
        this.riakTemplate = riakTemplate;
        this.conversionService = conversionService;
        this.association = association;
        this.owner = association.getOwner();
        this.child = association.getAssociatedEntity();
    }

    public void index(Long primaryKey, List<Long> foreignKeys) {
        for (Long foreignKey : foreignKeys) {
            link(foreignKey, primaryKey);
        }
    }

    public void index(Long primaryKey, Long foreignKey) {
        link(foreignKey, primaryKey);
    }

    protected void link(Long childKey, Long ownerKey) {
        String bucketName = String.format("%s.%s.%s",
                owner.getName(),
                association.getName(),
                ownerKey);
        riakTemplate.set(bucketName, childKey, "");
        riakTemplate.link(child.getName(), childKey, bucketName, childKey, "target");
        //SimpleBucketKeyPair<String, Long> bkpOwner = new SimpleBucketKeyPair<String, Long>(owner.getName(), ownerKey);
        //riakTemplate.link(bkpChild, bkpOwner, "owner");
    }

    public List<Long> query(Long primaryKey) {
        String bucketName = String.format("%s.%s.%s",
                owner.getName(),
                association.getName(),
                primaryKey);
        Object obj = riakTemplate.getBucketSchema(bucketName, true).get("keys");
        List<Long> keys = new ArrayList<Long>();
        if (obj instanceof List) {
            for (String key : (List<String>) obj) {
                keys.add(Long.parseLong(key));
            }
        }
        return keys;
    }

    public PersistentEntity getIndexedEntity() {
        return association.getAssociatedEntity();
    }
}
