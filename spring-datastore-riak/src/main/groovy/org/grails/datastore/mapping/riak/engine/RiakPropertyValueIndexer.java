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

import org.grails.datastore.mapping.engine.PropertyValueIndexer;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.springframework.data.keyvalue.riak.core.RiakTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author J. Brisbin <jon@jbrisbin.com>
 */
public class RiakPropertyValueIndexer implements PropertyValueIndexer<Long> {

    @SuppressWarnings("unused")
    private RiakTemplate riakTemplate;
    @SuppressWarnings("unused")
    private MappingContext mappingContext;
    @SuppressWarnings("unused")
    private RiakEntityPersister riakEntityPersister;
    @SuppressWarnings("unused")
    private PersistentProperty property;

    public RiakPropertyValueIndexer(RiakTemplate riakTemplate, MappingContext mappingContext, RiakEntityPersister riakEntityPersister, PersistentProperty property) {
        this.riakTemplate = riakTemplate;
        this.mappingContext = mappingContext;
        this.riakEntityPersister = riakEntityPersister;
        this.property = property;
    }

    public void index(Object value, Long primaryKey) {
    }

    public List<Long> query(Object value) {
        return query(value, 0, -1);
    }

    public List<Long> query(Object value, int offset, int max) {
        @SuppressWarnings("unused")
        String js = "function(sdata) {\n" +
        "  var data = Riak.mapValuesJson(sdata);\n" +
        "  ejsLog(\"/tmp/mapred.log\", JSON.stringify(sdata));\n" +
        "  ejsLog(\"/tmp/mapred.log\", JSON.stringify(data));\n" +
        "  return [data];\n" +
        "}";
        return new ArrayList<Long>();
    }

    public String getIndexName(Object value) {
        return null;
    }

    public void deindex(Object value, Long primaryKey) {
    }
}
