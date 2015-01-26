package org.grails.datastore.gorm.cassandra.mapping;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Calendar;
import java.util.Currency;
import java.util.Locale;
import java.util.TimeZone;

import grails.core.GrailsDomainClassProperty;
import org.grails.datastore.mapping.cassandra.config.CassandraMappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.cassandra.mapping.BasicCassandraPersistentProperty;
import org.springframework.data.cassandra.mapping.CassandraPersistentEntity;
import org.springframework.data.cassandra.mapping.CassandraPersistentProperty;
import org.springframework.data.cassandra.mapping.CassandraSimpleTypeHolder;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;
import org.springframework.validation.Errors;

/**
 * Extends default
 * {@link org.springframework.data.cassandra.mapping.BasicCassandraMappingContext}
 * to create CassandraPersistentProperty for GORM types not supported by Spring
 * Data Cassandra or Cassandra
 *
 */
public class BasicCassandraMappingContext extends org.springframework.data.cassandra.mapping.BasicCassandraMappingContext {
	
	CassandraMappingContext gormCassandraMappingContext;
	
	public BasicCassandraMappingContext(CassandraMappingContext gormCassandraMappingContext) {
		this.gormCassandraMappingContext = gormCassandraMappingContext;
	}

	@Override
	protected CassandraPersistentEntity<?> addPersistentEntity(TypeInformation<?> typeInformation) {
		if(!typeInformation.getType().isInterface()) {
			return super.addPersistentEntity(typeInformation);
		}
		return null;
	}

	@Override
	public CassandraPersistentProperty createPersistentProperty(Field field, PropertyDescriptor descriptor, CassandraPersistentEntity<?> owner, SimpleTypeHolder simpleTypeHolder) {
		PersistentEntity gormEntity = gormCassandraMappingContext.getPersistentEntity(owner.getName());			
		final CassandraPersistentProperty property = super.createPersistentProperty(field, descriptor, owner, simpleTypeHolder);
		final CassandraPersistentProperty transientProperty = new BasicCassandraPersistentProperty(field, descriptor, owner, (CassandraSimpleTypeHolder) simpleTypeHolder) {
			public boolean isTransient() {
				return true;
			}
		};
		if (field == null && !property.usePropertyAccess()) {
			return transientProperty;
		}
		if (field != null && Modifier.isTransient(field.getModifiers())) {
			return transientProperty;
		}
		if (field != null && grails.core.GrailsDomainClassProperty.ERRORS.equals(field.getName())) {
			return transientProperty;
		}

		if (field != null && field.getType().equals(Errors.class)) {
			return transientProperty;
		}

		Class<?> rawType = field != null ? field.getType() : descriptor != null ? descriptor.getPropertyType() : null;
		if (rawType == null) {
			return transientProperty;
		}
		if (rawType.isEnum()) {
			// persist as a string
			return new BasicCassandraPersistentProperty(field, descriptor, owner, (CassandraSimpleTypeHolder) simpleTypeHolder) {
				public com.datastax.driver.core.DataType getDataType() {
					return CassandraSimpleTypeHolder.getDataTypeFor(String.class);
				};

				public java.lang.Class<?> getType() {
					return String.class;
				};

				public boolean usePropertyAccess() {
					return true;
				};
			};
		} else if (URL.class.isAssignableFrom(rawType) || TimeZone.class.isAssignableFrom(rawType) || Locale.class.isAssignableFrom(rawType) || Currency.class.isAssignableFrom(rawType) || Calendar.class.isAssignableFrom(rawType)) {
			// persist as a string
			return new BasicCassandraPersistentProperty(field, descriptor, owner, (CassandraSimpleTypeHolder) simpleTypeHolder) {
				public com.datastax.driver.core.DataType getDataType() {
					return CassandraSimpleTypeHolder.getDataTypeFor(String.class);
				};

				public java.lang.Class<?> getType() {
					return String.class;
				};

				public boolean isEntity() {
					return false;
				};

				public boolean usePropertyAccess() {
					return true;
				};
			};
		} else if (field != null && GrailsDomainClassProperty.VERSION.equals(field.getName()) && !gormEntity.isVersioned()) {
			return transientProperty;
		}
		

		// for collections or maps of non-primitive types, i.e associations,
		// return transient property as spring data cassandra doesn't support
		if (!property.isTransient()) {
			if (property.isMap() || property.isCollectionLike()) {
				try {
					property.getDataType();
				} catch (InvalidDataAccessApiUsageException e) {
					return transientProperty;
				}
			}
		}
		return property;
	}
}
