package org.grails.datastore.mapping.cassandra.config;

import groovy.lang.Closure;

import org.grails.datastore.gorm.cassandra.mapping.BasicCassandraMappingContext;
import org.grails.datastore.mapping.model.AbstractMappingContext;
import org.grails.datastore.mapping.model.MappingConfigurationStrategy;
import org.grails.datastore.mapping.model.MappingFactory;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.config.GormMappingConfigurationStrategy;
import org.springframework.util.Assert;

/**
 * Started by Jeff Beck(@beckje01) on 1/23/14.
 * 
 */
@SuppressWarnings({"rawtypes"})
public class CassandraMappingContext extends AbstractMappingContext {
    public static final String DEFAULT_KEYSPACE = "CassandraKeySpace";
    protected String keyspace;
    protected MappingFactory<Table, Column> mappingFactory;
    protected MappingConfigurationStrategy syntaxStrategy;
    protected BasicCassandraMappingContext springCassandraMappingContext;     
    private Closure defaultMapping;
    
    public CassandraMappingContext() {
        this(DEFAULT_KEYSPACE, null);
    }
    
    public CassandraMappingContext(String keyspace) {
    	this(keyspace, null);
    }
    
    /**
     * Constructs a context using the given keyspace
     *
     * @param keyspace
     *            The keyspace, this is typically the application name
     */
    public CassandraMappingContext(String keyspace, Closure defaultMapping) {
        Assert.hasText(keyspace, "Property [keyspace] must be set!");
        this.keyspace = keyspace;
        this.defaultMapping = defaultMapping;
        mappingFactory = createMappingFactory();
        syntaxStrategy = new GormMappingConfigurationStrategy(mappingFactory);

    }

    public Closure getDefaultMapping() {
        return defaultMapping;
    }
    
    protected MappingFactory<Table, Column> createMappingFactory() {
        GormCassandraMappingFactory cassandraMappingFactory = new GormCassandraMappingFactory(keyspace);
        cassandraMappingFactory.setDefaultMapping(defaultMapping);
        return cassandraMappingFactory;
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

    @Override
    protected PersistentEntity createPersistentEntity(Class javaClass, boolean external) {
        return new CassandraPersistentEntity(javaClass, this);
    }
}
