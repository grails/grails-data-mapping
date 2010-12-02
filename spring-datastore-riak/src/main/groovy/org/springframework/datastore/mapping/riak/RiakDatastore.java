/*
 * Copyright (c) 2010 by NPC International, Inc.
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

package org.springframework.datastore.mapping.riak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.riak.core.RiakTemplate;
import org.springframework.datastore.mapping.core.AbstractDatastore;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext;
import org.springframework.datastore.mapping.model.MappingContext;

import java.util.Map;

/**
 * @author Jon Brisbin <jon.brisbin@npcinternational.com>
 */
public class RiakDatastore extends AbstractDatastore implements InitializingBean, DisposableBean {

  public static final String CONFIG_DEFAULT_URI = "defaultUri";
  public static final String CONFIG_MAPRED_URI = "mapReduceUri";
  public static final String CONFIG_USE_CACHE = "useCache";

  public static final String DEFAULT_URI = "http://localhost:8098/riak/{bucket}/{key}";
  public static final String DEFAULT_MAPRED_URI = "http://localhost:8098/mapred";
  public static final boolean DEFAULT_USE_CACHE = true;

  private final Logger log = LoggerFactory.getLogger(getClass());

  private String defaultUri = DEFAULT_URI;
  private String mapReduceUri = DEFAULT_MAPRED_URI;
  private boolean useCache = DEFAULT_USE_CACHE;
  private Object writeQuorum = "all";
  private Object durableWriteQuorum = "all";

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

  public Object getWriteQuorum() {
    return writeQuorum;
  }

  public void setWriteQuorum(Object writeQuorum) {
    this.writeQuorum = writeQuorum;
  }

  public Object getDurableWriteQuorum() {
    return durableWriteQuorum;
  }

  public void setDurableWriteQuorum(Object durableWriteQuorum) {
    this.durableWriteQuorum = durableWriteQuorum;
  }

  @Override
  protected Session createSession(Map<String, String> connectionDetails) {
    String defaultUri = this.defaultUri;
    if (connectionDetails != null) {
      defaultUri = connectionDetails.containsKey(CONFIG_DEFAULT_URI) ? connectionDetails.get(
          CONFIG_DEFAULT_URI) : DEFAULT_URI;
      mapReduceUri = connectionDetails.containsKey(CONFIG_MAPRED_URI) ? connectionDetails.get(
          CONFIG_MAPRED_URI) : DEFAULT_MAPRED_URI;
      useCache = connectionDetails.containsKey(CONFIG_USE_CACHE) ? Boolean.parseBoolean(
          connectionDetails.get(
              CONFIG_USE_CACHE).toString()) : DEFAULT_USE_CACHE;
    }
    RiakTemplate riak = new RiakTemplate(defaultUri, mapReduceUri);
    riak.setUseCache(useCache);
    try {
      riak.afterPropertiesSet();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return new RiakSession(this, mappingContext, riak);
  }

  public void destroy() throws Exception {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void afterPropertiesSet() throws Exception {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}
