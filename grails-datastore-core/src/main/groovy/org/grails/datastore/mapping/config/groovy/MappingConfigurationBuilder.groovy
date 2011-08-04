/* Copyright (C) 2011 SpringSource
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
package org.grails.datastore.mapping.config.groovy

import org.springframework.beans.MutablePropertyValues
import org.grails.datastore.mapping.reflect.NameUtils
import org.springframework.validation.DataBinder

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class MappingConfigurationBuilder {

    public static final String VERSION_KEY = 'VERSION_KEY'

    Object target
    Map properties = [:]
    Class propertyClass

    MappingConfigurationBuilder(target, Class propertyClass) {
        this.target = target
        this.propertyClass = propertyClass
        propertyClass.metaClass.propertyMissing = { String name, val -> }
    }

    def invokeMethod(String name, args) {
        if (args.size() == 0) {
            return
        }

        if ('version'.equals(name) && args.length == 1 && args[0] instanceof Boolean) {
            properties[VERSION_KEY] = args[0]
            return
        }

        def setterName = NameUtils.getSetterName(name)
        if (target.respondsTo(setterName)) {
            target[name] = args.size() == 1 ? args[0] : args
        }
        else {
            if (args[0] instanceof Map) {
                def instance = properties[name] ?: propertyClass.newInstance()
                def binder = new DataBinder(instance)
                binder.bind(new MutablePropertyValues(args[0]))
                properties[name] = instance
            }
        }
    }

    void evaluate(Closure callable) {
        if (!callable) {
            return
        }

        def originalDelegate = callable.delegate
        try {
            callable.delegate = this
            callable.resolveStrategy = Closure.DELEGATE_FIRST
            callable.call()
        } finally {
            callable.delegate = originalDelegate
        }
    }
}
