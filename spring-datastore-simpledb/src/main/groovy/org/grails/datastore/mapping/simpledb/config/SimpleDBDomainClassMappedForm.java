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
package org.grails.datastore.mapping.simpledb.config;

import java.util.Map;

import org.grails.datastore.mapping.keyvalue.mapping.config.Family;
import org.grails.datastore.mapping.simpledb.util.SimpleDBConst;

/**
 * Mapping for
 * {@link org.grails.datastore.mapping.simpledb.config.SimpleDBPersistentEntity}
 * with the SimpleDB specific properties so that the following can be used in
 * the mapping:
 *
 * <pre>
 *      static mapping = {
 *          domain 'Person'
 *          sharding enabled:false, shards:3 //optional, needed only if sharding is used for this domain class
 *      }
 * </pre>
 * @author Roman Stepanenko
 * @since 0.1
 */
public class SimpleDBDomainClassMappedForm extends Family {

    protected String domain;
    protected Map<String, Object> sharding; //sharding configuration
    protected boolean shardingEnabled;

    public SimpleDBDomainClassMappedForm() {
        determineShardingConfiguration();
    }

    public SimpleDBDomainClassMappedForm(String domain) {
        this.domain = domain;
        determineShardingConfiguration();
    }

    public SimpleDBDomainClassMappedForm(String keyspace, String domain) {
        super(keyspace, domain);
        this.domain = domain;
        determineShardingConfiguration();
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
        super.setFamily(domain);
    }

    public boolean isShardingEnabled() {
        return shardingEnabled;
    }

    @Override
    public void setFamily(String family) {
        super.setFamily(family);
        domain = family;
    }

    public Map<String, Object> getSharding() {
        return sharding;
    }

    public void setSharding(Map<String, Object> sharding) {
        this.sharding = sharding;
        determineShardingConfiguration();
    }

    protected void determineShardingConfiguration() {
        if (sharding != null) {
            shardingEnabled = Boolean.TRUE.equals(sharding.get(SimpleDBConst.PROP_SHARDING_ENABLED));
        }
    }

    @Override
    public String toString() {
        return "SimpleDBDomainClassMappedForm{" +
                "domain='" + domain + '\'' +
                ", shardingEnabled=" + shardingEnabled +
                ", sharding=" + sharding +
                '}';
    }
}
