/* Copyright (C) 2014 SpringSource
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

package grails.test.mixin.mongodb

import grails.test.mixin.support.GrailsUnitTestMixin
import grails.test.mixin.support.SkipMethod
import grails.test.runtime.TestPluginRegistrar
import grails.test.runtime.TestPluginUsage
import grails.test.runtime.gorm.MongoDbTestPlugin
import groovy.transform.CompileStatic

import com.mongodb.Mongo

/**
 * A test mixin that sets up MongoDB
 *
 * @author Graeme Rocher
 *
 */
@CompileStatic
class MongoDbTestMixin extends GrailsUnitTestMixin implements TestPluginRegistrar {
    private static final Set<String> REQUIRED_FEATURES = (["mongoDbGorm"] as Set<String>).asImmutable()
    
    public MongoDbTestMixin(Set<String> features) {
        super((REQUIRED_FEATURES + features) as Set<String>)
    }
    
    public MongoDbTestMixin() {
        super(REQUIRED_FEATURES)
    }
    
    @SkipMethod
    public Iterable<TestPluginUsage> getTestPluginUsages() {
        return TestPluginUsage.createForActivating(MongoDbTestPlugin)
    }
    
    /**
     * Sets up a GORM for MongoDB domain for the given domain classes
     *
     * @param persistentClasses
     */
    void mongoDomain(Collection<Class> persistentClasses) {
        getRuntime().publishEvent("mongoDomain", [domains: persistentClasses])
    }

    /**
     * Sets up a GORM for MongoDB domain for the given Mongo instance and domain classes
     *
     * @param persistentClasses
     */
    void mongoDomain(Mongo mongo, Collection<Class> persistentClasses) {
        getRuntime().publishEvent("mongoDomain", [domains: persistentClasses, mongo: mongo])
    }

    /**
     * Sets up a GORM for MongoDB domain for the given configuration and domain classes
     *
     * @param persistentClasses
     */
    void mongoDomain(Map config, Collection<Class> persistentClasses) {
        getRuntime().publishEvent("mongoDomain", [domains: persistentClasses, config: config])
    }
}

