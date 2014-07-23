package org.grails.datastore.mapping.cassandra.config;

import java.beans.PropertyDescriptor;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.grails.datastore.mapping.config.AbstractGormMappingFactory;
import org.grails.datastore.mapping.config.Property;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.IdentityMapping;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.PropertyMapping;
import org.grails.datastore.mapping.model.types.Identity;
import org.grails.datastore.mapping.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("rawtypes")
public class GormCassandraMappingFactory extends AbstractGormMappingFactory<Table, Column> {

    private static Logger log = LoggerFactory.getLogger(GormCassandraMappingFactory.class);
    private String keyspace;

    public GormCassandraMappingFactory(String keyspace) {
        this.keyspace = keyspace;
    }

    @Override
    public Table createMappedForm(PersistentEntity entity) {
        Table table = super.createMappedForm(entity);

        if (table.getKeyspace() == null) {
            table.setKeyspace(keyspace);
        }

        Map<String, Column> properties = entityToPropertyMap.get(entity);
        Column idProperty  = properties.get(IDENTITY_PROPERTY);
        Iterator<Entry<String, Column>> iterator = properties.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Entry<String, Column> entry = iterator.next();                  
            if (entry.getValue() instanceof Column) {
                String name = entry.getKey();
                Column column = entry.getValue();
                if (idProperty != null && idProperty.getName() != null && idProperty.getName().equals(name)) {
                    //remove extra column created if id property in constraints block, will be handled elsewhere,
                    //as it conflicts with the column created in mapping block.                    
                    iterator.remove();
                    continue;
                }
                
                if (column.getName() == null) {
                    column.setName(name);
                }
                if (column.isPrimaryKey()) {
                    table.addPrimaryKey(column);
                }
            }
        }
        
        Map<String, String> sort = table.getSort();
        if (sort != null && sort.size() > 0) {
            Entry<String, String> entry = sort.entrySet().iterator().next();
            String property = entry.getKey();
            if (properties.containsKey(property)) {
                String direction = entry.getValue();
                Query.Order orderBy = "desc".equals(direction) ? Query.Order.desc(property) : Query.Order.asc(property);
                table.setOrderBy(orderBy);                
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
                final PersistentProperty idProperty = classMapping.getEntity().getPropertyByName(name);
                return new IdentityMapping() {
                    public String[] getIdentifierName() {
                        return new String[] { name };
                    }

                    public ClassMapping getClassMapping() {
                        return classMapping;
                    }

                    public Property getMappedForm() {
                        return idProperty.getMapping().getMappedForm();
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
            return new Identity<Column>(owner, context, table.getPrimaryKeyNames()[0], Map.class) {
                PropertyMapping<Column> propertyMapping = createPropertyMapping(this, owner);

                public PropertyMapping<Column> getMapping() {
                    return propertyMapping;
                }
            };
        }
        return super.createIdentity(owner, context, pd);
    }
}
