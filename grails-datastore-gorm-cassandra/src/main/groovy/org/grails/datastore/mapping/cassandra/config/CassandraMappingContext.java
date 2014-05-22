package org.grails.datastore.mapping.cassandra.config;

import org.grails.datastore.mapping.model.AbstractMappingContext;
import org.grails.datastore.mapping.model.MappingConfigurationStrategy;
import org.grails.datastore.mapping.model.MappingFactory;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.config.GormMappingConfigurationStrategy;
import org.springframework.data.cassandra.mapping.BasicCassandraMappingContext;
import org.springframework.util.Assert;

/**
 * Started by Jeff Beck(@beckje01) on 1/23/14.
 * <p>
 * Just building out Mapping for Cassandra stating model with keyvalue? hacking
 * honestly
 * </p>
 */
public class CassandraMappingContext extends AbstractMappingContext {
    protected String keyspace;
    protected MappingFactory<Table, Column> mappingFactory;
    protected MappingConfigurationStrategy syntaxStrategy;
    protected BasicCassandraMappingContext springCassandraMappingContext;     
    
    /**
     * Constructs a context using the given keyspace
     *
     * @param keyspace
     *            The keyspace, this is typically the application name
     */
    public CassandraMappingContext(String keyspace) {
        Assert.notNull(keyspace, "Argument [keyspace] cannot be null");
        this.keyspace = keyspace;
        mappingFactory = createMappingFactory();
        syntaxStrategy = new GormMappingConfigurationStrategy(mappingFactory);

    }

    protected MappingFactory<Table, Column> createMappingFactory() {
        return new GormCassandraMappingFactory(keyspace);
    }

    public String getKeyspace() {
        return keyspace;
    }

  
    public void setMappingFactory(MappingFactory<Table, Column> mappingFactory) {
        this.mappingFactory = mappingFactory;
    }

    public void setSyntaxStrategy(MappingConfigurationStrategy syntaxStrategy) {
        this.syntaxStrategy = syntaxStrategy;
    }          

    public BasicCassandraMappingContext getSpringCassandraMappingContext() {
        return springCassandraMappingContext;
    }

    public void setSpringCassandraMappingContext(BasicCassandraMappingContext springCassandraMappingContext) {
        this.springCassandraMappingContext = springCassandraMappingContext;
    }

    @Override
    public MappingConfigurationStrategy getMappingSyntaxStrategy() {
        return syntaxStrategy;
    }

    @Override
    public MappingFactory<Table, Column> getMappingFactory() {
        return mappingFactory;
    }

    @Override
    protected PersistentEntity createPersistentEntity(@SuppressWarnings("rawtypes") Class javaClass) {
        return new CassandraPersistentEntity(javaClass, this);
    }
}
