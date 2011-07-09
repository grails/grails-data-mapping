package org.springframework.datastore.mapping.simpledb.engine;

import java.util.LinkedList;
import java.util.List;


/**
 * An implementation of the domain resolver which assumes there is no sharding - i.e. always
 * the same domain name for all the primary keys (for the same type of {@link org.springframework.datastore.mapping.model.PersistentEntity}
 */
public class ConstSimpleDBDomainResolver extends AbstractSimpleDBDomainResolver {
    public ConstSimpleDBDomainResolver(String entityFamily, String domainNamePrefix) {
        super(entityFamily, domainNamePrefix);
        domains = new LinkedList<String>();
        domains.add(this.entityFamily); //without sharding there is just one domain
    }

    public String resolveDomain(String id) {
        return entityFamily; //without sharding it is always the same one per PersistentEntity
    }

    public List<String> getAllDomainsForEntity() {
        return domains;
    }

    private List<String> domains;
}
