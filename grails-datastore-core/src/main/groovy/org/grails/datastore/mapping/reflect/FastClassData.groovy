/* Copyright (C) 2015 original authors
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
package org.grails.datastore.mapping.reflect

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.engine.internal.MappingUtils
import org.grails.datastore.mapping.model.ClassMapping
import org.grails.datastore.mapping.model.IdentityMapping
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.springframework.cglib.reflect.FastClass
import org.springframework.cglib.reflect.FastMethod

/**
 * Cached FastClass data
 *
 * @author Graeme Rocher
 * @since 4.1
 */
@CompileStatic
class FastClassData {

    private static final Class[] ZERO_CLASS_ARRAY = [] as Class[]

    final FastClass fastClass
    final FastMethod idReader
    final Map<String, FastMethod> fastGetters = [:]
    final Map<String, FastMethod> fastSetters = [:]
    final PersistentEntity entity

    FastClassData(PersistentEntity entity) {
        this.entity = entity
        if(!entity.isInitialized()) {
            entity.initialize()
        }
        this.fastClass = FastClass.create(entity.javaClass)
        if(entity.identity != null) {
            def identifierName = getIdentifierName(entity.mapping)
            this.idReader = fastClass.getMethod(NameUtils.getGetterName(identifierName), ZERO_CLASS_ARRAY)
            fastGetters[identifierName] = idReader
            fastSetters[identifierName] = fastClass.getMethod(NameUtils.getSetterName(identifierName), [entity.identity.type] as Class[])

        }
        else {
            idReader = null
        }


        def version = entity.version
        if(version != null) {
            def versionName = version.name
            def getterName = MappingUtils.getGetterName(versionName)
            def setterName = MappingUtils.getSetterName(versionName)
            fastGetters[versionName] = fastClass.getMethod(getterName, ZERO_CLASS_ARRAY)
            fastSetters[versionName] = fastClass.getMethod(setterName, [version.type] as Class[])
        }

        for(prop in entity.persistentProperties) {
            def name = prop.name

            def getterName = MappingUtils.getGetterName(name)
            def setterName = MappingUtils.getSetterName(name)
            fastGetters[name] = fastClass.getMethod(getterName, ZERO_CLASS_ARRAY)
            fastSetters[name] = fastClass.getMethod(setterName, [prop.type] as Class[])
        }
    }

    Object getIdentifier(Object o) {
        idReader?.invoke(o)
    }

    Class getPropertyType(String name) {
        fastGetters[name]?.returnType
    }

    protected String getIdentifierName(ClassMapping cm) {
        final IdentityMapping identifier = cm.getIdentifier();
        if (identifier != null && identifier.getIdentifierName() != null) {
            return identifier.getIdentifierName()[0];
        }
        return null
    }
}
