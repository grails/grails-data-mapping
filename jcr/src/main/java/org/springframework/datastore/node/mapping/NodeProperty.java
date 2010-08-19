package org.springframework.datastore.node.mapping;

import org.springframework.datastore.mapping.types.Fetch;

/**
 * TODO: write javadoc
 *
 * @author Erawat Chamanont
 * @since 1.0
 */
public class NodeProperty {

    private String attributeName;
    private boolean index = false;
    private Fetch fetchStrategy = Fetch.LAZY;

    public NodeProperty(){}

    public NodeProperty(String attributeName) {
       this.attributeName = attributeName;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    /**
     * @return Whether this property is index
     */
    public boolean isIndex() {
        return index;
    }

    /**
     * Whether this property is index
     * @param index Sets whether to index the property or not
     */
    public void setIndex(boolean index) {
        this.index = index;
    }

    public Fetch getFetchStrategy() {
        return this.fetchStrategy;
    }

    public void setFetchStrategy(Fetch fetchStrategy) {
        this.fetchStrategy = fetchStrategy;
    }
}
