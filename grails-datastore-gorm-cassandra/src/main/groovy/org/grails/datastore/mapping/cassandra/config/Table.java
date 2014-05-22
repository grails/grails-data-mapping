package org.grails.datastore.mapping.cassandra.config;

import java.util.ArrayList;
import java.util.List;

import org.grails.datastore.mapping.config.Entity;

public class Table extends Entity {
    private String keyspace;
    private List<Column> primaryKeys = new ArrayList<Column>();
    private List<String> primaryKeyNames = new ArrayList<String>();
    
    public Table() {
     
    }
    
    public Table(String keyspace ) {
        this.keyspace = keyspace;
    }

    public String getKeyspace() {
        return keyspace;
    }

    public void setKeyspace(String keyspace) {
        this.keyspace = keyspace;
    }
    
    public void addPrimaryKey(Column column) {
        primaryKeys.add(column);
        primaryKeyNames.add(column.getName());
    }
    
    public List<Column> getPrimaryKeys() {
        return primaryKeys;
    }
    
    public String[] getPrimaryKeyNames() {
        return primaryKeyNames.toArray(new String[0]);
    }
    public boolean hasCompositePrimaryKeys() {
        return primaryKeys.size() > 1;
    }
    
    public boolean isPrimaryKey(String name) {
        return primaryKeyNames.contains(name);
    }
        
}
