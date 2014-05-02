package org.grails.datastore.mapping.cassandra.config;

import org.grails.datastore.mapping.keyvalue.mapping.config.AnnotationKeyValueMappingFactory;
import org.grails.datastore.mapping.keyvalue.mapping.config.Family;
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValue;
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext;
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValuePersistentEntity;
import org.grails.datastore.mapping.model.AbstractMappingContext;
import org.grails.datastore.mapping.model.MappingConfigurationStrategy;
import org.grails.datastore.mapping.model.MappingFactory;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.config.DefaultMappingConfigurationStrategy;
import org.grails.datastore.mapping.model.config.GormMappingConfigurationStrategy;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Started by Jeff Beck(@beckje01) on 1/23/14.
 * <p>
 * Just building out Mapping for Cassandra stating model with keyvalue? hacking honestly
 * </p>
 */
public class CassandraMappingContext extends AbstractMappingContext {
    protected MappingFactory<Family, KeyValue> mappingFactory;
    protected MappingConfigurationStrategy syntaxStrategy;
    protected String keyspace;
    public static final String GROOVY_OBJECT_CLASS = "groovy.lang.GroovyObject";

    @Override
    public void setCanInitializeEntities(boolean canInitializeEntities) {
        super.setCanInitializeEntities(canInitializeEntities);
        syntaxStrategy.setCanExpandMappingContext(false);
    }

    /**
     * Constructs a context using the given keyspace
     *
     * @param keyspace The keyspace, this is typically the application name
     */
    public CassandraMappingContext(String keyspace) {
        Assert.notNull(keyspace, "Argument [keyspace] cannot be null");
        this.keyspace = keyspace;
        initializeDefaultMappingFactory(keyspace);

        if (ClassUtils.isPresent(GROOVY_OBJECT_CLASS, KeyValueMappingContext.class.getClassLoader())) {
            syntaxStrategy = new GormMappingConfigurationStrategy(mappingFactory);
        }
        else {
            syntaxStrategy = new DefaultMappingConfigurationStrategy(mappingFactory);
        }
        
    }
    
    public String getKeyspace() {
        return keyspace;
    }

    protected void initializeDefaultMappingFactory(String keyspace) {
        // TODO: Need to abstract the construction of these to support JPA syntax etc.
        if (ClassUtils.isPresent(GROOVY_OBJECT_CLASS, KeyValueMappingContext.class.getClassLoader())) {
            mappingFactory = new CassandraKeyValueMappingFactory(keyspace);
        }
        else {
            mappingFactory = new AnnotationKeyValueMappingFactory(keyspace);
        }
    }

    public void setMappingFactory(MappingFactory<Family, KeyValue> mappingFactory) {
        this.mappingFactory = mappingFactory;
    }

    public void setSyntaxStrategy(MappingConfigurationStrategy syntaxStrategy) {
        this.syntaxStrategy = syntaxStrategy;
    }

    @Override
    public MappingConfigurationStrategy getMappingSyntaxStrategy() {
        return syntaxStrategy;
    }

    @Override
    public MappingFactory<Family, KeyValue> getMappingFactory() {
        return mappingFactory;
    }

    @Override
    protected PersistentEntity createPersistentEntity(@SuppressWarnings("rawtypes") Class javaClass) {
        return new KeyValuePersistentEntity(javaClass, this);
    }
}
