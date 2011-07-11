package org.springframework.datastore.mapping.simpledb.engine;

public abstract class AbstractSimpleDBDomainResolver implements SimpleDBDomainResolver {
    public AbstractSimpleDBDomainResolver(String entityFamily, String domainNamePrefix) {
        this.entityFamily = entityFamily;
        this.domainNamePrefix = domainNamePrefix;
        if ( domainNamePrefix != null ) {
            this.entityFamily = domainNamePrefix + this.entityFamily;
        }
    }

    protected String entityFamily;
    protected String domainNamePrefix;
}
