package org.grails.orm.hibernate.connections

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.config.ConfigurationBuilder
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettings
import org.springframework.core.env.PropertyResolver

/**
 * Builds the GORM for Hibernate configuration
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class HibernateConnectionSourceSettingsBuilder extends ConfigurationBuilder<HibernateConnectionSourceSettings, HibernateConnectionSourceSettings> {
    HibernateConnectionSourceSettingsBuilder(PropertyResolver propertyResolver, String configurationPrefix = "", ConnectionSourceSettings fallBackConfiguration = null) {
        super(propertyResolver, configurationPrefix, fallBackConfiguration)
    }

    @Override
    protected HibernateConnectionSourceSettings createBuilder() {
        return new HibernateConnectionSourceSettings()
    }

    @Override
    protected HibernateConnectionSourceSettings toConfiguration(HibernateConnectionSourceSettings builder) {
        return builder
    }
}
