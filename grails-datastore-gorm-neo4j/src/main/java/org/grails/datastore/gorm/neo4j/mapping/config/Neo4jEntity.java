package org.grails.datastore.gorm.neo4j.mapping.config;

import org.grails.datastore.mapping.config.Entity;

/**
 * Created by stefan on 10.04.14.
 * @author Stefan Armbruster
 */
public class Neo4jEntity extends Entity {

    private Object labels;

    public Object getLabels() {
        return labels;
    }

    public void setLabels(Object labels) {
        this.labels = labels;
    }
}
