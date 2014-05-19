package org.grails.datastore.mapping.cassandra.config;

import groovy.lang.Closure;
import org.grails.datastore.mapping.config.groovy.MappingConfigurationBuilder;
import org.grails.datastore.mapping.keyvalue.mapping.config.Family;
import org.grails.datastore.mapping.keyvalue.mapping.config.GormKeyValueMappingFactory;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.util.List;

public class GormCassandraMappingFactory extends GormKeyValueMappingFactory {

    private static Logger log = LoggerFactory.getLogger(GormCassandraMappingFactory.class);
	private String keyspace;
	private Closure defaultMapping;

	public GormCassandraMappingFactory(String keyspace) {
		super(keyspace);
		this.keyspace = keyspace;
	}

	@Override
	public Family createMappedForm(PersistentEntity entity) {

		ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(entity.getJavaClass());
		Family family = BeanUtils.instantiate(getEntityMappedFormType());
		MappingConfigurationBuilder builder = new MappingConfigurationBuilder(family, getPropertyMappedFormType());

		if (defaultMapping != null) {
			builder.evaluate(defaultMapping);
		}
		List<Closure> values = cpf.getStaticPropertyValuesFromInheritanceHierarchy(GormProperties.MAPPING, Closure.class);
		for (int i = values.size(); i > 0; i--) {
			Closure value = values.get(i - 1);
			builder.evaluate(value);
		}
		values = cpf.getStaticPropertyValuesFromInheritanceHierarchy(GormProperties.CONSTRAINTS, Closure.class);
		for (int i = values.size(); i > 0; i--) {
			Closure value = values.get(i - 1);
			builder.evaluate(value);
		}
		entityToPropertyMap.put(entity, builder.getProperties());

		if (family.getKeyspace() == null) {
			family.setKeyspace(keyspace);
		}
		log.trace("family is {}", family.getFamily());
		if (family.getFamily() == null) {
			family.setFamily(entity.getDecapitalizedName());
	        log.trace("family set to {}", family.getFamily());
		}
		return family;
	}

	public void setDefaultMapping(Closure defaultMapping) {
		super.setDefaultMapping(defaultMapping);
		this.defaultMapping = defaultMapping;
	}

}
