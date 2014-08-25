package org.grails.datastore.gorm.cassandra.mapping;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Calendar;
import java.util.Currency;
import java.util.Locale;
import java.util.TimeZone;

import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.springframework.data.cassandra.mapping.BasicCassandraPersistentProperty;
import org.springframework.data.cassandra.mapping.CassandraPersistentEntity;
import org.springframework.data.cassandra.mapping.CassandraPersistentProperty;
import org.springframework.data.cassandra.mapping.CassandraSimpleTypeHolder;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/**
 * Extends default {@link org.springframework.data.cassandra.mapping.BasicCassandraMappingContext} to create 
 * CassandraPersistentProperty for GORM types not supported by Spring Data Cassandra or Cassandra
 *
 */
public class BasicCassandraMappingContext extends org.springframework.data.cassandra.mapping.BasicCassandraMappingContext {
    @Override
    public CassandraPersistentProperty createPersistentProperty(Field field, PropertyDescriptor descriptor, CassandraPersistentEntity<?> owner, SimpleTypeHolder simpleTypeHolder) {
        if (field != null && Modifier.isTransient(field.getModifiers())) {
            return new BasicCassandraPersistentProperty(field, descriptor, owner, (CassandraSimpleTypeHolder) simpleTypeHolder) {
                public boolean isTransient() { return true ;}
            };
        }
        if (field != null && GrailsDomainClassProperty.ERRORS.equals(field.getName())) {
            return new BasicCassandraPersistentProperty(field, descriptor, owner, (CassandraSimpleTypeHolder) simpleTypeHolder) {
                public boolean isTransient() { return true ;}
            };
        }            
        Class<?> rawType = field != null ? field.getType() : descriptor != null ? descriptor.getPropertyType() : null;
        if (rawType == null) {
            return new BasicCassandraPersistentProperty(field, descriptor, owner, (CassandraSimpleTypeHolder) simpleTypeHolder) {
                public boolean isTransient() { return true ;}
            };
        }
        if (rawType.isEnum()) {
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
        } else if (URL.class.isAssignableFrom(rawType) || TimeZone.class.isAssignableFrom(rawType) ||
                    Locale.class.isAssignableFrom(rawType) || Currency.class.isAssignableFrom(rawType) ||
                    Calendar.class.isAssignableFrom(rawType)) {
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
        }
        return super.createPersistentProperty(field, descriptor, owner, simpleTypeHolder);
    }
}
