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
package org.grails.datastore.mapping.mongo.config;

import java.util.HashMap;
import java.util.Map;

import org.grails.datastore.mapping.document.config.Attribute;

/**
 * Extends {@link Attribute} class with additional Mongo specific configuration
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class MongoAttribute extends Attribute {

    public static final String INDEX_TYPE = "type";

    @SuppressWarnings("rawtypes")
    private Map indexAttributes;
    private boolean reference = false;


    public boolean isReference() {
        return reference;
    }

    public void setReference(boolean reference) {
        this.reference = reference;
    }

    @SuppressWarnings("rawtypes")
    public Map getIndexAttributes() {
        return indexAttributes;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setIndexAttributes(Map indexAttributes) {
        if (this.indexAttributes == null) {
            this.indexAttributes = indexAttributes;
        }
        else {
            this.indexAttributes.putAll(indexAttributes);
        }
    }

    public void setField(String name) {
        setTargetName(name);
    }

    public String getField() {
        return getTargetName();
    }

    @SuppressWarnings("unchecked")
    public void setGeoIndex(boolean geoIndex) {
        if (geoIndex) {
            setIndex(true);
            initIndexAttributes();
            indexAttributes.put(INDEX_TYPE, "2d");
        }
    }


    public void setGeoIndex(String indexType) {
        if ("2d".equals(indexType) || "2dsphere".equals(indexType)) {
            setIndex(true);
            initIndexAttributes();
            indexAttributes.put(INDEX_TYPE, indexType);
        }
    }

    @SuppressWarnings("rawtypes")
    void initIndexAttributes() {
        if (indexAttributes == null) {
            indexAttributes = new HashMap();
        }
    }
}
