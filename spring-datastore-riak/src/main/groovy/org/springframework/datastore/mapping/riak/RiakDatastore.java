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

package org.springframework.datastore.mapping.riak;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.keyvalue.riak.core.RiakTemplate;
import org.springframework.datastore.mapping.core.AbstractDatastore;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.riak.util.Ignore404sErrorHandler;

/**
 * A {@link org.springframework.datastore.mapping.core.Datastore} implemenation for the Riak
 * Key/Value store.
 *
 * @author J. Brisbin <jon@jbrisbin.com>
 */
public class RiakDatastore extends AbstractDatastore implements InitializingBean, DisposableBean {

    public static final String CONFIG_DEFAULT_URI = "defaultUri";
    public static final String CONFIG_MAPRED_URI = "mapReduceUri";
    public static final String CONFIG_USE_CACHE = "useCache";

    public static final String DEFAULT_URI = "http://localhost:8098/riak/{bucket}/{key}";
    public static final String DEFAULT_MAPRED_URI = "http://localhost:8098/mapred";
    public static final boolean DEFAULT_USE_CACHE = true;

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The full URI to use on the {@link org.springframework.data.keyvalue.riak.core.RiakTemplate}.
     */
    private String defaultUri = DEFAULT_URI;
    /**
     * The Map/Reduce URI to use on the {@link org.springframework.data.keyvalue.riak.core.RiakTemplate}.
     */
    private String mapReduceUri = DEFAULT_MAPRED_URI;
    /**
     * Whether or not to use the internal, ETag-based object cache.
     */
    private boolean useCache = DEFAULT_USE_CACHE;

    public RiakDatastore() {
        this(new KeyValueMappingContext(""));
    }

    public RiakDatastore(MappingContext mappingContext) {
        this(mappingContext, null);
    }

    public RiakDatastore(MappingContext mappingContext, Map<String, String> connectionDetails) {
        super(mappingContext, connectionDetails);
        initializeConverters(mappingContext);
        if (connectionDetails != null) {
            defaultUri = connectionDetails.containsKey(CONFIG_DEFAULT_URI) ? connectionDetails.get(
                    CONFIG_DEFAULT_URI) : DEFAULT_URI;
            mapReduceUri = connectionDetails.containsKey(CONFIG_MAPRED_URI) ? connectionDetails.get(
                    CONFIG_MAPRED_URI) : DEFAULT_MAPRED_URI;
            useCache = connectionDetails.containsKey(CONFIG_USE_CACHE) ? Boolean.parseBoolean(
                    connectionDetails.get(
                            CONFIG_USE_CACHE).toString()) : DEFAULT_USE_CACHE;
        }
    }

    @Override
    protected Session createSession(Map<String, String> connDetails) {
        @SuppressWarnings("hiding") String defaultUri = this.defaultUri;
        if (connDetails != null) {
            defaultUri = connDetails.containsKey(CONFIG_DEFAULT_URI) ? connDetails.get(
                    CONFIG_DEFAULT_URI) : DEFAULT_URI;
            mapReduceUri = connDetails.containsKey(CONFIG_MAPRED_URI) ? connDetails.get(
                    CONFIG_MAPRED_URI) : DEFAULT_MAPRED_URI;
            useCache = connDetails.containsKey(CONFIG_USE_CACHE) ? Boolean.parseBoolean(
                    connDetails.get(
                            CONFIG_USE_CACHE).toString()) : DEFAULT_USE_CACHE;
        }
        RiakTemplate riak = new RiakTemplate(defaultUri, mapReduceUri);
        riak.setUseCache(useCache);
        riak.getRestTemplate().setErrorHandler(new Ignore404sErrorHandler());
        try {
            riak.afterPropertiesSet();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return new RiakSession(this, mappingContext, riak);
    }

    public void destroy() throws Exception {
    }

    public void afterPropertiesSet() throws Exception {
    }
}
