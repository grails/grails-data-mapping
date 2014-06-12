package org.grails.datastore.mapping.query.jpa;

import java.util.List;

@SuppressWarnings("rawtypes")
public class JpaQueryInfo {

    String query;
    List parameters;

    public JpaQueryInfo(String query, List parameters) {
        this.query = query;
        this.parameters = parameters;
    }

    public String getQuery() {
        return query;
    }

    public List getParameters() {
        return parameters;
    }
}
