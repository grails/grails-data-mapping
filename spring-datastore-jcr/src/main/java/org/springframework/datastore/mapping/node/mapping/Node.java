package org.springframework.datastore.mapping.node.mapping;

/**
 * TODO: write javadoc
 *
 * @author Erawat Chamanont
 * @since 1.0
 */
public class Node {

    private String className;

    public Node() {
    }

    public Node(String entityName) {
        this.className = entityName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
