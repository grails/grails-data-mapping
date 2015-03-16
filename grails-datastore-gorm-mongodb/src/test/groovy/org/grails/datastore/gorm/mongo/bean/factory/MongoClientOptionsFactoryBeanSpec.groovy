package org.grails.datastore.gorm.mongo.bean.factory

import spock.lang.Specification

/*
 * Copyright 2014 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author graemerocher
 */
class MongoClientOptionsFactoryBeanSpec extends Specification {

    void "Test that MongoClientOptionsFactory bean can create a valid MongoClientOptions"() {
        when:"A factory is created"
            def factory = new MongoClientOptionsFactoryBean()
            factory.mongoOptions = [maxConnectionIdleTime:20, readPreference:"secondary"]
            factory.afterPropertiesSet()
            def clientOptions = factory.getObject()
        then:
            clientOptions.maxConnectionIdleTime == 20
            clientOptions.readPreference.name == 'secondary'
    }
}
