package grails.gorm.rx.mongodb.config

import com.mongodb.ReadPreference
import com.mongodb.ServerAddress
import com.mongodb.async.client.MongoClientSettings
import org.grails.datastore.rx.mongodb.config.MongoClientSettingsBuilder
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertyResolver
import org.springframework.core.env.StandardEnvironment
import spock.lang.Specification

class MongoClientSettingsBuilderSpec extends Specification {

    void "test mongo client settings builder"() {
        when:"using a property resolver"
        StandardEnvironment resolver = new StandardEnvironment()
        Map myMap = ['grails.mongodb.options.readPreference': 'secondary',
                     'grails.mongodb.host': 'mycompany',
                     'grails.mongodb.port': '1234',
                     'grails.mongodb.username': 'foo',
                     'grails.mongodb.password': 'bar',
                     'grails.mongodb.options.clusterSettings.maxWaitQueueSize': '10']
        resolver.propertySources.addFirst(new MapPropertySource("test", myMap))

        def builder = new MongoClientSettingsBuilder(resolver)
        def clientSettings = builder.build()

        then:"The settings are correct"
        clientSettings.clusterSettings.hosts.contains(new ServerAddress("mycompany", 1234))
        clientSettings.readPreference == ReadPreference.secondary()
        clientSettings.credentialList.size() == 1
        clientSettings.clusterSettings.maxWaitQueueSize == 10
    }
}
