package org.grails.datastore.mapping.core

import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertyResolver
import org.springframework.core.env.StandardEnvironment
import spock.lang.Specification

/**
 * Created by graemerocher on 01/03/2017.
 */
class DatastoreUtilsSpec extends Specification {

    void "test prepare property source from env"() {
        given:
        StandardEnvironment env = new StandardEnvironment()
        env.propertySources.addFirst(new MapPropertySource("test", ['grails.foo':'bar']))
        env.propertySources.addFirst(new MapPropertySource("test2", ['grails.foo':'baz']))

        PropertyResolver resolver = DatastoreUtils.preparePropertyResolver(env)
        expect:
        env != null
        resolver.getProperty('grails.foo') == 'baz'
    }
}
