package org.grails.datastore.mapping.config

import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettings
import org.springframework.core.env.PropertyResolver
import spock.lang.Specification

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


    static class TestConfigurationBuilder extends ConfigurationBuilder<ConnectionSourceSettings, ConnectionSourceSettings> {


        TestConfigurationBuilder(PropertyResolver propertyResolver) {
            super(propertyResolver, Settings.PREFIX)
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
