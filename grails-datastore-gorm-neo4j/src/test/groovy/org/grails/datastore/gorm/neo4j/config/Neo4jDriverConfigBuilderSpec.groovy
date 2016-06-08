package org.grails.datastore.gorm.neo4j.config

import org.neo4j.driver.v1.Config
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import spock.lang.Specification

/**
 * Created by graemerocher on 08/06/16.
 */
class Neo4jDriverConfigBuilderSpec extends Specification {

    void "test mongo client settings builder"() {
        when:"using a property resolver"
        StandardEnvironment resolver = new StandardEnvironment()
        Map myMap = ['grails.neo4j.options.maxSessions': '10',
                     'grails.neo4j.options.encryptionLevel': 'NONE']
        resolver.propertySources.addFirst(new MapPropertySource("test", myMap))

        def builder = new Neo4jDriverConfigBuilder(resolver)
        Config clientSettings = builder.build()

        then:"The settings are correct"
        clientSettings.connectionPoolSize() == 10
        clientSettings.encryptionLevel() == Config.EncryptionLevel.NONE

    }
}
