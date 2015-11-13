package org.grails.orm.hibernate.cfg

import org.grails.datastore.mapping.model.DatastoreConfigurationException

/**
 * Builder delegate that handles multiple-column definitions for a
 * single domain property, e.g.
 * <pre>
 *   amount type: MonetaryAmountUserType, {
 *       column name: "value"
 *       column name: "currency_code", sqlType: "text"
 *   }
 * </pre>
 */
class PropertyDefinitionDelegate {
    PropertyConfig config

    PropertyDefinitionDelegate(PropertyConfig config) {
        this.config = config
    }

    def column(Map args) {
        // Check that this column has a name
        if (!args["name"]) {
            throw new DatastoreConfigurationException("Column definition must have a name!")
        }

        // Create a new column configuration based on the mapping for this column.
        def column = new ColumnConfig()
        column.name = args["name"]
        column.sqlType = args["sqlType"]
        column.enumType = args["enumType"] ?: column.enumType
        column.index = args["index"]
        column.unique = args["unique"] ?: false
        column.length = args["length"] ?: -1
        column.precision = args["precision"] ?: -1
        column.scale = args["scale"] ?: -1

        // Append the new column configuration to the property config.
        config.columns << column
    }
}
