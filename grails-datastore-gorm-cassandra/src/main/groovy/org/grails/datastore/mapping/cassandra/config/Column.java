package org.grails.datastore.mapping.cassandra.config;

import java.util.Map;

import org.grails.datastore.mapping.config.Property;

/**
 * Provides configuration options for mapping Cassandra columns
 *
 */
public class Column extends Property{
    private String type;
    private Map primaryKey;
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(Map primaryKey) {
        this.primaryKey = primaryKey;
    }
    
    public boolean isPrimaryKey() {
        return primaryKey != null && !primaryKey.isEmpty();
    }
}
