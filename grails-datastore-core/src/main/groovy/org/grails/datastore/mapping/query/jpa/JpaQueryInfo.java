package org.grails.datastore.mapping.query.jpa;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 9/13/11
 * Time: 4:44 PM
 * To change this template use File | Settings | File Templates.
 */
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
