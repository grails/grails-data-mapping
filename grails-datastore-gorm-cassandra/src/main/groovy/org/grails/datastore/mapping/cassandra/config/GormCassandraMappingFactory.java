package org.grails.datastore.mapping.cassandra.config;

import groovy.lang.Closure;

import java.beans.PropertyDescriptor;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.grails.datastore.mapping.config.AbstractGormMappingFactory;
import org.grails.datastore.mapping.config.Property;
import org.grails.datastore.mapping.config.groovy.MappingConfigurationBuilder;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.IdentityMapping;
import org.grails.datastore.mapping.model.IllegalMappingException;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PropertyMapping;
import org.grails.datastore.mapping.model.types.Identity;
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("rawtypes")
public class GormCassandraMappingFactory extends AbstractGormMappingFactory<Table, Column> {

    private static final String TABLE_PROPERTIES = "tableProperties";
	private static Logger log = LoggerFactory.getLogger(GormCassandraMappingFactory.class);
    private String keyspace;

    public GormCassandraMappingFactory(String keyspace) {
        this.keyspace = keyspace;
    }

    @SuppressWarnings("unchecked")
	@Override
    public Table createMappedForm(PersistentEntity entity) {
        Table table = super.createMappedForm(entity);
        CassandraPersistentEntity cassandraPersistentEntity = (CassandraPersistentEntity) entity;
        //read tableOptions
        ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(entity.getJavaClass());        
        final Closure value = cpf.getStaticPropertyValue(TABLE_PROPERTIES, Closure.class);
        if (value != null) {
        	MapConfigurationBuilder builder = new MapConfigurationBuilder();
        	try {
        		builder.evaluate(value);
        	} catch (Exception e) {
        		throw new IllegalMappingException(String.format("Error reading %s : %s",TABLE_PROPERTIES, e.toString()));
        	}
        	table.setTableProperties(builder.getProperties());
        }
        
        if (table.getKeyspace() == null) {
            table.setKeyspace(keyspace);
        }

        //additional static mapping block handling        
        Map<String, Column> properties = entityToPropertyMap.get(entity);        
        Object version = properties.get(MappingConfigurationBuilder.VERSION_KEY);
        if (version instanceof Boolean) {
        	cassandraPersistentEntity.setVersion((Boolean) version);
        }
                
        Column idProperty  = properties.get(IDENTITY_PROPERTY);
        Iterator<Entry<String, Column>> propertyIterator = properties.entrySet().iterator();
        
        while (propertyIterator.hasNext()) {
            Entry<String, Column> entry = propertyIterator.next();                  
            if (entry.getValue() instanceof Column) {
                String name = entry.getKey();
                Column column = entry.getValue();
                if (idProperty != null && idProperty.getName() != null && idProperty.getName().equals(name)) {
                    //remove extra column created if id property in constraints block, as it conflicts with the column created in mapping block. 
                	//constraints will be handled elsewhere in GORM
                    propertyIterator.remove();
                    continue;
                }
                
                if (column.getName() == null) {
                    column.setName(name);
                }                
                table.addColumn(column);
            }
        }
               
        return table;
    }

    @Override
    protected Class<Table> getEntityMappedFormType() {
        return Table.class;
    }

    @Override
    protected Class<Column> getPropertyMappedFormType() {
        return Column.class;
    }

    @Override
    protected IdentityMapping getIdentityMappedForm(final ClassMapping classMapping, final Column property) {
        if (property != null) {
            final String name = property.getName();
            if (name != null) {                
                return new IdentityMapping() {
                    public String[] getIdentifierName() {
                        return new String[] { name };
                    }

                    public ClassMapping getClassMapping() {
                        return classMapping;
                    }

                    public Property getMappedForm() {
                        return property;
                    }
                };
            }
        }
        return super.getIdentityMappedForm(classMapping, property);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Identity<Column> createIdentity(PersistentEntity owner, MappingContext context, PropertyDescriptor pd) {
        final Table table = (Table) owner.getMapping().getMappedForm();
        if (table.hasCompositePrimaryKeys()) {
            return new Identity<Column>(owner, context, table.getPrimaryKeyNames()[0], pd.getPropertyType()) {
                PropertyMapping<Column> propertyMapping = createPropertyMapping(this, owner);

                public PropertyMapping<Column> getMapping() {
                    return propertyMapping;
                }
            };
        }
        return super.createIdentity(owner, context, pd);
    }
    
    @Override
    public boolean isSimpleType(Class propType) {     
        return isCassandraNativeType(propType) ||  super.isSimpleType(propType);
    }
    
    public static boolean isCassandraNativeType(Class clazz) {
        return UUID.class.getName().equals(clazz.getName());
    }
}
