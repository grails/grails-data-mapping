package org.grails.datastore.gorm.schemaless

import groovy.transform.CompileStatic

/**
 * A trait that adds support for defining dynamic attributes for databases that support it
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
trait DynamicAttributes {

    private transient Map<String, Object> dynamicAttributes = [:]

    void putAt(String name, value) {
        dynamicAttributes.put name, value
    }

    def getAt(String name) {
        dynamicAttributes.get(name)
    }

    /**
     * Obtain the dynamic attributes
     *
     * @return The dynamic attributes
     */
    Map<String, Object> attributes() {
        return this.dynamicAttributes
    }

    /**
     * Obtain the dynamic attributes combined with the provided attributes
     *
     * @param newAttributes The new attributes
     * @return The dynamic attributes
     */
    Map<String, Object> attributes(Map<String, Object> newAttributes) {
        if(newAttributes != null) {
            this.dynamicAttributes.putAll(newAttributes)
        }
        return dynamicAttributes
    }
}