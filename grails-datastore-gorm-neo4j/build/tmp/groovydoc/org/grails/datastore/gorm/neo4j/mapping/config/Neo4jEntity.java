package org.grails.datastore.gorm.neo4j.mapping.config;

import org.grails.datastore.mapping.config.Entity;

/**
 * Created by stefan on 10.04.14.
 */
public class Neo4jEntity extends Entity {

    private String label;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
