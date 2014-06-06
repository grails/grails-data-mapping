package org.grails.datastore.gorm.neo4j;

import org.grails.datastore.mapping.model.types.Association;

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
    private List<Object> params = new ArrayList<Object>();

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

    public int addParam(Object value) {
        params.add(value);
        return params.size();
    }

    /**
     *
     * @param position first element is 1
     * @param value
     */
    public void replaceParamAt(int position, Object value) {
        params.set(position-1, value);
    }

//    public int getNextParamNumber() {
//        return params.size();
//    }

    public List<Object> getParams() {
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

        if (returnColumns.isEmpty()) {
            cypher.append(" RETURN n.__id__ as id, labels(n) as labels, n as data \n");
            if (orderAndLimits!=null) {
                cypher.append(orderAndLimits).append(" \n");
            }
        } else {
            cypher.append(" RETURN ");
            Iterator<String> iter = returnColumns.iterator();   // same as Collection.join(String separator)
            if (iter.hasNext()) {
                cypher.append(iter.next());
                while (iter.hasNext()) {
                    cypher.append(", ").append(iter.next());
                }
            }
            if (orderAndLimits!=null) {
                cypher.append(" ");
                cypher.append(orderAndLimits);
            }
        }

        return cypher.toString();
    }

    public static String findRelationshipEndpointIdsFor(Association association) {
        String relType = RelationshipUtils.relationshipTypeUsedFor(association);
        boolean reversed = RelationshipUtils.useReversedMappingFor(association);

        StringBuilder sb = new StringBuilder();
        GraphPersistentEntity graphPersistentEntity = (GraphPersistentEntity) (association.getOwner());
        String label = graphPersistentEntity.getLabel();
        sb.append("MATCH (me:").append(label).append(" {__id__:{1}})");
        if (reversed) {
            sb.append("<");
        }
        sb.append("-[:").append(relType).append("]-");
        if (!reversed) {
            sb.append(">");
        }
        sb.append("(other) RETURN other.__id__ as id");
        return sb.toString();
    }

}
