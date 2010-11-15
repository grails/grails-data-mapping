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

import com.basho.riak.client.RiakConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.datastore.mapping.core.AbstractDatastore;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.riak.util.RiakJavaClientTemplate;

import java.util.Map;

/**
 * @author Jon Brisbin <jon.brisbin@npcinternational.com>
 */
public class RiakDatastore extends AbstractDatastore implements InitializingBean, DisposableBean {

  public static final String CONFIG_HOST = "host";
  public static final String CONFIG_PORT = "port";
  public static final String CONFIG_PREFIX = "prefix";

  public static final String DEFAULT_HOST = "localhost";
  public static final String DEFAULT_PORT = "8098";
  public static final String DEFAULT_PREFIX = "/riak";

  private String host = DEFAULT_HOST;
  private String port = DEFAULT_PORT;
  private String prefix = DEFAULT_PREFIX;

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
      host = connectionDetails.containsKey(CONFIG_HOST) ? connectionDetails.get(CONFIG_HOST) : DEFAULT_HOST;
      port = connectionDetails.containsKey(CONFIG_PORT) ? connectionDetails.get(CONFIG_PORT) : DEFAULT_PORT;
      prefix = connectionDetails.containsKey(CONFIG_PREFIX) ? connectionDetails.get(CONFIG_PREFIX) : DEFAULT_PREFIX;
    }
  }

  @Override
  protected Session createSession(Map<String, String> connectionDetails) {
    String host = this.host;
    String port = this.port;
    String prefix = this.prefix;
    if (connectionDetails != null) {
      host = connectionDetails.containsKey(CONFIG_HOST) ? connectionDetails.get(CONFIG_HOST) : DEFAULT_HOST;
      port = connectionDetails.containsKey(CONFIG_PORT) ? connectionDetails.get(CONFIG_PORT) : DEFAULT_PORT;
      prefix = connectionDetails.containsKey(CONFIG_PREFIX) ? connectionDetails.get(CONFIG_PREFIX) : DEFAULT_PREFIX;
    }
    RiakConfig riakConfig = new RiakConfig(host, port, prefix);
    return new RiakSession(this, mappingContext, new RiakJavaClientTemplate(riakConfig));
  }

  public void destroy() throws Exception {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void afterPropertiesSet() throws Exception {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}
