package org.grails.datastore.mapping.core.connections

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.config.ConfigurationBuilder
import org.grails.datastore.mapping.config.Settings
import org.springframework.core.env.PropertyResolver

/**
 * Builder for the default settings
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class ConnectionSourceSettingsBuilder extends ConfigurationBuilder<ConnectionSourceSettings, ConnectionSourceSettings> {
    ConnectionSourceSettingsBuilder(PropertyResolver propertyResolver, String configurationPrefix = Settings.PREFIX) {
        super(propertyResolver, configurationPrefix)
    }

    @Override
    protected ConnectionSourceSettings createBuilder() {
        return new ConnectionSourceSettings()
    }

    @Override
    protected ConnectionSourceSettings toConfiguration(ConnectionSourceSettings builder) {
        return builder
    }
}
