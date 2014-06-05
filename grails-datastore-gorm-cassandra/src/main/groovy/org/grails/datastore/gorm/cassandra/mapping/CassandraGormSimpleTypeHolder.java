package org.grails.datastore.gorm.cassandra.mapping;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.cassandra.mapping.CassandraSimpleTypeHolder;

public class CassandraGormSimpleTypeHolder extends CassandraSimpleTypeHolder {   
    
    private static final Set<Class<?>> CASSANDRA_GORM_SIMPLE_TYPES = new HashSet<Class<?>>();
    
    static {
        CASSANDRA_GORM_SIMPLE_TYPES.add(URL.class);
    }
    
    @Override
    public boolean isSimpleType(Class<?> type) {
        boolean simple = super.isSimpleType(type);
        if (!simple) {
            for (Class<?> clazz : CASSANDRA_GORM_SIMPLE_TYPES) {
                if (type == clazz || clazz.isAssignableFrom(type)) {
                        simple = true;
                        break;
                }
            }
        } 
        return simple;        
    }
}
