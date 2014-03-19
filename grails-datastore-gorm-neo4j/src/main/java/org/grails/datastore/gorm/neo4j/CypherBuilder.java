package org.grails.datastore.gorm.neo4j;

import java.util.*;

/**
 * Created by stefan on 19.03.14.
 */
public class CypherBuilder {

    public final static String TYPE = "type";
    public final static String END = "end";
    public final static String START = "start";


    private String forLabel;
    private Set<String> matches = new HashSet<String>();
    private String conditions;
    private String orderAndLimits;
    private List<String> returnColumns = new ArrayList<String>();
    private Map<String, Object> params = new HashMap<String, Object>();

    public CypherBuilder(String forLabel) {
        this.forLabel = forLabel;
    }

    public void addMatch(String match) {
        matches.add(match);
    }

    public int getNextMatchNumber() {
        return matches.size();
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public void setOrderAndLimits(String orderAndLimits) {
        this.orderAndLimits = orderAndLimits;
    }

    public void putParam(String key, Object value) {
        params.put(key, value);
    }

    public int getNextParamNumber() {
        return params.size();
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void addReturnColumn(String returnColumn) {
        returnColumns.add(returnColumn);
    }

    public String build() {
        StringBuilder cypher = new StringBuilder();
        cypher.append("MATCH (n:").append(forLabel).append(")");

        for (String m : matches) {
            cypher.append(", ").append(m);
        }

        if ((conditions!=null) && (!conditions.isEmpty())) {
            cypher.append(" WHERE ").append(conditions);
        }
        cypher.append(" ");

        if (returnColumns.isEmpty()) {
            cypher
                    .append("WITH id(n) as id, labels(n) as labels, n as data\n")
                    .append("OPTIONAL MATCH (n)-[r]-()\n")
                    .append("WITH id, labels, data, type(r) as t, collect(id(endnode(r))) as endNodeIds, collect(id(startnode(r))) as startNodeIds\n")
                    .append("RETURN id, labels, data, collect( {")
                    .append(TYPE).append(": t, ")
                    .append(END).append(": endNodeIds, ")
                    .append(START).append(": startNodeIds}) as relationships");

        } else {
            cypher.append("RETURN ");
            Iterator<String> iter = returnColumns.iterator();   // same as Collection.join(String separator)
            if (iter.hasNext()) {
                cypher.append(iter.next());
                while (iter.hasNext()) {
                    cypher.append(", ").append(iter.next());
                }
            }
        }
        if (orderAndLimits!=null) {
            cypher.append(" ");
            cypher.append(orderAndLimits);
        }

        return cypher.toString();
    }
}
