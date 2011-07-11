package org.springframework.datastore.mapping.simpledb.config;

import org.springframework.datastore.mapping.keyvalue.mapping.config.Family;
import org.springframework.datastore.mapping.simpledb.util.SimpleDBConst;

import java.util.Map;

/**
 * Mapping for {@link org.springframework.datastore.mapping.simpledb.config.SimpleDBPersistentEntity} with the SimpleDB specific properties so that
 * the following can be used in the mapping:
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

    public Map getSharding() {
        return sharding;
    }

    public void setSharding(Map sharding) {
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

    protected String domain;
    protected Map sharding; //sharding configuration

    protected boolean shardingEnabled;
}
