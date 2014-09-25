package org.grails.datastore.mapping.cassandra.config;

import java.util.Map;

import org.grails.datastore.mapping.config.Property;
import org.springframework.cassandra.core.Ordering;

/**
 * Provides configuration options for mapping Cassandra columns
 *
 */
public class Column extends Property{
    private String type;
    private Map primaryKeyAttributes;
    private Ordering order;
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map getPrimaryKeyAttributes() {
        return primaryKeyAttributes;
    }

    public void setPrimaryKey(Map primaryKey) {
        this.primaryKeyAttributes = primaryKey;
    }
    
    public boolean isPrimaryKey() {
        return primaryKeyAttributes != null && !primaryKeyAttributes.isEmpty();
    }
    
    public Ordering getOrder() {
		return order;
	}

	public void setOrder(String order) {		
		if ("desc".equalsIgnoreCase(order)) {
			this.order = Ordering.DESCENDING;
		} else {
			this.order = Ordering.ASCENDING;
		}
	}
}
