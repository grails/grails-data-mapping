package org.grails.datastore.gorm.neo4j.simplegraph;

/**
 * Created by stefan on 03.03.14.
 */
public class Relationship {

    private long startNodeId;
    private long endNodeId;
    private String type;

    public Relationship(long startNodeId, long endNodeId, String type) {
        this.startNodeId = startNodeId;
        this.endNodeId = endNodeId;
        this.type = type;
    }

    public long getStartNodeId() {
        return startNodeId;
    }

    public long getEndNodeId() {
        return endNodeId;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Relationship that = (Relationship) o;

        if (endNodeId != that.endNodeId) return false;
        if (startNodeId != that.startNodeId) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (startNodeId ^ (startNodeId >>> 32));
        result = 31 * result + (int) (endNodeId ^ (endNodeId >>> 32));
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    public long getOtherId(Long id) {
        if (startNodeId==id) {
            return endNodeId;
        } else if (endNodeId==id) {
            return startNodeId;
        } else {
            throw new IllegalArgumentException(String.format("relationship (%d)-[:%s]->(%d) does not match id %d", startNodeId, type, endNodeId, id));
        }
    }
}
