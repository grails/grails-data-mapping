package org.grails.datastore.mapping.cassandra;

import groovy.util.ConfigObject;
import org.grails.datastore.mapping.document.config.Attribute;
import org.grails.datastore.mapping.document.config.Collection;
import org.grails.datastore.mapping.keyvalue.mapping.config.Family;
import org.grails.datastore.mapping.keyvalue.mapping.config.GormKeyValueMappingFactory;
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValue;
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValuePersistentEntity;
import org.grails.datastore.mapping.model.AbstractMappingContext;
import org.grails.datastore.mapping.model.MappingConfigurationStrategy;
import org.grails.datastore.mapping.model.MappingFactory;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.config.GormMappingConfigurationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Started by Jeff Beck(@beckje01) on 1/23/14.
 * <p/>
 * Just building out Mapping for Cassandra stating model with keyvalue? hacking honestly
 */
public class CassandraMappingContext extends AbstractMappingContext {

	private static Logger log = LoggerFactory.getLogger(CassandraMappingContext.class);

	MappingFactory<Family, KeyValue> mappingFactory;
	MappingConfigurationStrategy syntaxStrategy;

	/*
			Neo4jMappingContext() {
        mappingFactory = new GraphGormMappingFactory()
        syntaxStrategy = new GormMappingConfigurationStrategy(mappingFactory)
        //addTypeConverter(new StringToNumberConverterFactory().getConverter(BigDecimal))
        addTypeConverter(new StringToShortConverter())
        addTypeConverter(new StringToBigIntegerConverter())
         …
    }
	 */

	public CassandraMappingContext(ConfigObject configObject) {

		String defaultKeySpace;
		if (configObject.get("keyspace") instanceof String) {
			defaultKeySpace = (String)configObject.get("keyspace");
		} else {
			log.warn("No default keyspace configured using " + CassandraDatastore.DEFAULT_KEYSPACE);
			defaultKeySpace = CassandraDatastore.DEFAULT_KEYSPACE;
		}

		mappingFactory = new CassandraKeyValueMappingFactory(defaultKeySpace);
		syntaxStrategy = new GormMappingConfigurationStrategy(mappingFactory);
	}

	@Override
	public MappingConfigurationStrategy getMappingSyntaxStrategy() {
		return syntaxStrategy;
	}

	@Override
	public MappingFactory getMappingFactory() {
		return mappingFactory;
	}

	@Override
	protected PersistentEntity createPersistentEntity(Class javaClass) {
		KeyValuePersistentEntity persistentEntity = new KeyValuePersistentEntity(javaClass, this);
		mappingFactory.createMappedForm(persistentEntity); //TODO: ? populates mappingFactory.entityToPropertyMap as a side effect ?

		return persistentEntity;
	}
}
