package org.grails.datastore.mapping.config

import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettings
import org.springframework.core.env.PropertyResolver
import spock.lang.Specification

import javax.persistence.FlushModeType

/**
 * Created by graemerocher on 29/06/16.
 */
class ConfigurationBuilderSpec extends Specification {

    void "Test configuration builder"() {

        given:"A configuration"
        def config = DatastoreUtils.createPropertyResolver(
                (Settings.SETTING_AUTO_FLUSH): "true",
                (Settings.SETTING_DEFAULT_MAPPING): {
                }
        )

        when:"The configuration is built"
        def builder = new TestConfigurationBuilder(config)
        ConnectionSourceSettings connectionSourceSettings = builder.build()

        then:"The result is correct"
        connectionSourceSettings.autoFlush
        connectionSourceSettings.getDefault().mapping != null

    }


    void "Test configuration builder with fallback config"() {

        given:"A configuration"
        def config = DatastoreUtils.createPropertyResolver(
                (Settings.SETTING_AUTO_FLUSH): "true",
                (Settings.SETTING_DEFAULT_MAPPING): {
                }
        )

        when:"The configuration is built"
        def fallback = new ConnectionSourceSettings().flushMode(FlushModeType.COMMIT).defaults(new ConnectionSourceSettings.DefaultSettings().constraints({->}))
        def builder = new TestConfigurationBuilder(config, fallback)
        ConnectionSourceSettings connectionSourceSettings = builder.build()

        then:"The result is correct"
        connectionSourceSettings.autoFlush
        connectionSourceSettings.flushMode == FlushModeType.COMMIT
        connectionSourceSettings.getDefault().mapping != null
        connectionSourceSettings.getDefault().constraints != null

    }

    static class TestConfigurationBuilder extends ConfigurationBuilder<ConnectionSourceSettings, ConnectionSourceSettings> {


        TestConfigurationBuilder(PropertyResolver propertyResolver) {
            super(propertyResolver, Settings.PREFIX)
        }

        TestConfigurationBuilder(PropertyResolver propertyResolver, ConnectionSourceSettings fallback) {
            super(propertyResolver, Settings.PREFIX, fallback)
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
}
