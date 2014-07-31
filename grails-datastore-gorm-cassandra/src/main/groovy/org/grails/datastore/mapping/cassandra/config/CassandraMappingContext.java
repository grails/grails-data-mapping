package org.grails.datastore.mapping.cassandra.config;

import grails.gorm.CassandraEntity;

import java.lang.annotation.Annotation;

import org.grails.datastore.gorm.cassandra.mapping.BasicCassandraMappingContext;
import org.grails.datastore.mapping.model.AbstractMappingContext;
import org.grails.datastore.mapping.model.MappingConfigurationStrategy;
import org.grails.datastore.mapping.model.MappingFactory;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.config.GormMappingConfigurationStrategy;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

/**
 * Started by Jeff Beck(@beckje01) on 1/23/14.
 * 
 */
public class CassandraMappingContext extends AbstractMappingContext {
    public static final String DEFAULT_KEYSPACE = "CassandraKeySpace";
    protected String keyspace;
    protected MappingFactory<Table, Column> mappingFactory;
    protected MappingConfigurationStrategy syntaxStrategy;
    protected BasicCassandraMappingContext springCassandraMappingContext;     
    
    public CassandraMappingContext() {
        this(DEFAULT_KEYSPACE);
    }
    /**
     * Constructs a context using the given keyspace
     *
     * @param keyspace
     *            The keyspace, this is typically the application name
     */
    public CassandraMappingContext(String keyspace) {
        Assert.hasText(keyspace, "Property [keyspace] must be set");
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
        Annotation cassandraAnnotation = AnnotationUtils.findAnnotation(javaClass, CassandraEntity.class);
        if (cassandraAnnotation != null) {
            return new CassandraPersistentEntity(javaClass, this);
        } else {
            return null;        
        }
    }
}
