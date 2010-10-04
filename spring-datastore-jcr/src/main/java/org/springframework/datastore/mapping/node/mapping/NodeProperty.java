package org.springframework.datastore.mapping.node.mapping;

import javax.persistence.FetchType;


/**
 * TODO: write javadoc
 *
 * @author Erawat Chamanont
 * @since 1.0
 */
public class NodeProperty {

    private String name;
    private boolean index = false;
    private FetchType fetchStrategy = FetchType.LAZY;

    public NodeProperty(){}

    public NodeProperty(String name) {
       this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public FetchType getFetchStrategy() {
        return fetchStrategy;
    }

    public void setFetchStrategy(FetchType fetchStrategy) {
        this.fetchStrategy = fetchStrategy;
    }
}
