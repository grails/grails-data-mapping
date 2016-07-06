package org.grails.orm.hibernate.jdbc.connections

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.config.ConfigurationBuilder
import org.grails.orm.hibernate.cfg.Settings
import org.springframework.core.env.PropertyResolver

/**
 * Settings builder for {@link DataSourceSettings}
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class DataSourceSettingsBuilder extends ConfigurationBuilder<DataSourceSettings, DataSourceSettings> {
    DataSourceSettingsBuilder(PropertyResolver propertyResolver, String configurationPrefix = Settings.SETTING_DATASOURCE, Object fallBackSettings = null) {
        super(propertyResolver, configurationPrefix, fallBackSettings)
    }

    @Override
    protected DataSourceSettings createBuilder() {
        return new DataSourceSettings()
    }

    @Override
    protected DataSourceSettings toConfiguration(DataSourceSettings builder) {
        return builder
    }
}
