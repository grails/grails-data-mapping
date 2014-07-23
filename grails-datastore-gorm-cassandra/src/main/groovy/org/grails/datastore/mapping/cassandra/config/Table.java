package org.grails.datastore.mapping.cassandra.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.grails.datastore.mapping.config.Entity;
import org.grails.datastore.mapping.query.Query.Order;

public class Table extends Entity {
    private String keyspace;
    private List<Column> primaryKeys = new ArrayList<Column>();
    private List<String> primaryKeyNames = new ArrayList<String>();
    private Map<String, String> sort;
    private Order orderBy;
    
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
        
    public void setSort(Map<String, String> sort) {
        this.sort = sort;
    }
    
    public Map<String, String> getSort() {
        return sort;
    }

    public Order getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(Order orderBy) {
        this.orderBy = orderBy;
    }   
    
}
