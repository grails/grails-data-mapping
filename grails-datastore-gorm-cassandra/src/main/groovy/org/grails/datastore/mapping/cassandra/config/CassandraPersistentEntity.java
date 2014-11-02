package org.grails.datastore.mapping.cassandra.config;

import org.grails.datastore.mapping.config.Entity;
import org.grails.datastore.mapping.model.AbstractClassMapping;
import org.grails.datastore.mapping.model.AbstractPersistentEntity;
import org.grails.datastore.mapping.model.ClassMapping;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;

public class CassandraPersistentEntity extends AbstractPersistentEntity<Table> {

    private Entity mappedForm;
    private CassandraClassMapping classMapping;
    private Boolean version = true; 
    public CassandraPersistentEntity(Class<?> javaClass, CassandraMappingContext context) {
        super(javaClass, context);
        //need to create the mappedForm first so all the entity properties are read first, 
        //which then ensures the classMapping.identifierMapping is created correctly for 
        //multiple primary key entities
        this.mappedForm = context.getMappingFactory().createMappedForm(CassandraPersistentEntity.this);
        this.classMapping = new CassandraClassMapping(this, context);
    }      
    
    @SuppressWarnings("unchecked")
    @Override
    public ClassMapping<Table> getMapping() {
        return classMapping;
    }

    public class CassandraClassMapping extends AbstractClassMapping<Table> {

        public CassandraClassMapping(PersistentEntity entity, MappingContext context) {
            super(entity, context);
        }

        @Override
        public Table getMappedForm() {
            return (Table) mappedForm;
        }
    }
    
    
    public void setVersion(Boolean version) {
		this.version = version;
	}
    
    @Override
    public boolean isVersioned() {
    	return version;
    }
}
