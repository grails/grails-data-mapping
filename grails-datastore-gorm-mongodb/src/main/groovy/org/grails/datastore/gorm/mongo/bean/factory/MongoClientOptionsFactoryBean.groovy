/*
 * Copyright 2015 original authors
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
package org.grails.datastore.gorm.mongo.bean.factory

import com.mongodb.MongoClientOptions
import com.mongodb.ReadPreference
import com.mongodb.WriteConcern
import groovy.transform.CompileStatic
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.converter.ConverterRegistry
import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.util.ReflectionUtils

import java.lang.reflect.Method
import java.lang.reflect.Modifier


/**
 * Ability to configure MongoClientOptions from a Map
 *
 * @author Graeme Rocher
 * @since 4.0
 */
@CompileStatic
class MongoClientOptionsFactoryBean implements FactoryBean<MongoClientOptions>, InitializingBean {

    private Map<String, Method> configurationMethods = new HashMap<>()

    Map<String, Object> mongoOptions = [:]

    @Autowired(required = false)
    ConversionService conversionService
    @Autowired(required = false)
    ConverterRegistry converterRegistry
    MongoClientOptions.Builder builder = MongoClientOptions.builder()

    MongoClientOptionsFactoryBean() {

        def methods = ReflectionUtils.getUniqueDeclaredMethods( MongoClientOptions.Builder )
        for(m in methods) {
            def modifiers = m.modifiers
            if(Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers) && m.parameterTypes.size() == 1) {
                ReflectionUtils.makeAccessible(m)
                configurationMethods[m.name] = m
            }
        }
    }


    @Override
    MongoClientOptions getObject() throws Exception {
        return builder.build()
    }

    @Override
    Class<?> getObjectType() { MongoClientOptions }

    @Override
    boolean isSingleton() {
        return true
    }

    @Override
    void afterPropertiesSet() throws Exception {
        if(this.conversionService == null) {
            def defaultConversionService = new DefaultConversionService()
            conversionService = defaultConversionService
            converterRegistry = defaultConversionService
        }
        else if(converterRegistry == null) {
            converterRegistry = (ConverterRegistry) conversionService
        }

        converterRegistry.addConverter(new Converter<String, ReadPreference>() {
            @Override
            ReadPreference convert(String s) {
                switch( s ) {
                    case 'primary': return ReadPreference.primary()
                    case 'primaryPreferred': return ReadPreference.primaryPreferred()
                    case 'secondary': return ReadPreference.secondary()
                    case 'secondaryPreferred': return ReadPreference.secondaryPreferred()
                    case 'nearest': return ReadPreference.nearest()
                }
                return null
            }
        })
        converterRegistry.addConverter(new Converter<String, WriteConcern>() {
            @Override
            WriteConcern convert(String s) {
                switch( s ) {
                    case 'ACKNOWLEDGED': return WriteConcern.ACKNOWLEDGED
                    case 'UNACKNOWLEDGED': return WriteConcern.UNACKNOWLEDGED
                    case 'FSYNCED': return WriteConcern.FSYNCED
                    case 'JOURNALED': return WriteConcern.JOURNALED
                    case 'REPLICA_ACKNOWLEDGED': return WriteConcern.REPLICA_ACKNOWLEDGED
                    case 'NORMAL': return WriteConcern.NORMAL
                    case 'SAFE': return WriteConcern.SAFE
                    case 'MAJORITY': return WriteConcern.MAJORITY
                    case 'FSYNC_SAFE': return WriteConcern.FSYNC_SAFE
                    case 'JOURNAL_SAFE': return WriteConcern.JOURNAL_SAFE
                    case 'REPLICAS_SAFE': return WriteConcern.REPLICAS_SAFE
                }
                return null
            }
        })
        for(entry in mongoOptions.entrySet()) {
            def method = configurationMethods[entry.key]
            def unconvertedValue = entry.value
            def targetType = method.parameterTypes[0]
            if(conversionService.convert(unconvertedValue, targetType)) {
                def value = conversionService.convert(unconvertedValue, targetType)
                if(value) {
                    ReflectionUtils.invokeMethod(method, builder, value)
                }
            }
        }
    }
}
