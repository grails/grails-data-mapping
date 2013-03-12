/* Copyright (C) 2010 SpringSource
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
package org.grails.datastore.gorm

import org.grails.datastore.gorm.internal.InstanceMethodInvokingClosure
import org.grails.datastore.gorm.internal.StaticMethodInvokingClosure

import java.lang.reflect.Method
import java.lang.reflect.Modifier

import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.codehaus.groovy.runtime.metaclass.ClosureStaticMetaMethod
import org.grails.datastore.gorm.finders.CountByFinder
import org.grails.datastore.gorm.finders.FindAllByBooleanFinder
import org.grails.datastore.gorm.finders.FindAllByFinder
import org.grails.datastore.gorm.finders.FindByBooleanFinder
import org.grails.datastore.gorm.finders.FindByFinder
import org.grails.datastore.gorm.finders.FindOrCreateByFinder
import org.grails.datastore.gorm.finders.FindOrSaveByFinder
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.gorm.finders.ListOrderByFinder
import org.grails.datastore.gorm.query.NamedQueriesBuilder
import org.grails.datastore.gorm.validation.constraints.UniqueConstraintFactory
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Basic
import org.grails.datastore.mapping.model.types.EmbeddedCollection
import org.grails.datastore.mapping.model.types.ManyToMany
import org.grails.datastore.mapping.model.types.OneToMany
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.springframework.transaction.PlatformTransactionManager

/**
 * Enhances a class with GORM behavior
 *
 * @author Graeme Rocher
 */
class GormEnhancer {

    Datastore datastore
    PlatformTransactionManager transactionManager
    List<FinderMethod> finders
    boolean failOnError

    GormEnhancer(Datastore datastore) {
        this(datastore, null)
    }

    GormEnhancer(Datastore datastore, PlatformTransactionManager transactionManager) {
        this.datastore = datastore
        this.transactionManager = transactionManager
        initialiseFinders()
        registerConstraints(datastore)
    }

    protected void registerConstraints(Datastore datastore) {
        ConstrainedProperty.registerNewConstraint("unique", new UniqueConstraintFactory(datastore))
    }

    /**
     * Enhances all persistent entities.
     *
     * @param onlyExtendedMethods If only to add additional methods provides by subclasses of the GORM APIs
     */
    void enhance(boolean onlyExtendedMethods = false) {
        for (PersistentEntity e in datastore.mappingContext.persistentEntities) {
            enhance e
        }
    }

    /**
     * Enhance and individual entity
     *
     * @param e The entity
     * @param onlyExtendedMethods If only to add additional methods provides by subclasses of the GORM APIs
     */
    void enhance(PersistentEntity e, boolean onlyExtendedMethods = false) {
        def cls = e.javaClass
        def cpf = ClassPropertyFetcher.forClass(cls)
        def staticMethods = getStaticApi(cls)
        staticMethods.transactionManager = transactionManager
        def instanceMethods = [getInstanceApi(cls), getValidationApi(cls)]
        def tm = transactionManager

        List<Closure> namedQueries = cpf.getStaticPropertyValuesFromInheritanceHierarchy('namedQueries', Closure)
        for (int i = namedQueries.size(); i > 0; i--) {
            Closure closure = namedQueries.get(i - 1);
            registerNamedQueries(e, closure)
        }

        ExpandoMetaClass mc = org.codehaus.groovy.grails.commons.GrailsMetaClassUtils.getExpandoMetaClass(cls)
        for (currentInstanceMethods in instanceMethods) {
            def apiProvider = currentInstanceMethods
            if (GormInstanceApi.isInstance(apiProvider)) {
                mc.static.currentGormInstanceApi = {-> apiProvider }
            }
            else {
                mc.static.currentGormValidationApi = {-> apiProvider }
            }

            for (Method method in (onlyExtendedMethods ? apiProvider.extendedMethods : apiProvider.methods)) {
                def methodName = method.name
                Class[] parameterTypes = method.parameterTypes

                if (parameterTypes) {
                    parameterTypes = parameterTypes.length == 1 ? [] : parameterTypes[1..-1]

                    // use fake object just so we have the right method signature

                    final tooCall = new InstanceMethodInvokingClosure(apiProvider, methodName, parameterTypes)
                    def pt = parameterTypes
                    // Hack to workaround http://jira.codehaus.org/browse/GROOVY-4720
                    final closureMethod = new ClosureStaticMetaMethod(methodName, cls, tooCall, pt) {
                        @Override
                        int getModifiers() { Modifier.PUBLIC }
                    }
                    mc.registerInstanceMethod(closureMethod)
                }
            }
        }

        for (p in e.associations) {
            def prop = p
            def isBasic = prop instanceof Basic
            if ((prop instanceof OneToMany) || (prop instanceof ManyToMany) || isBasic || (prop instanceof EmbeddedCollection)) {
                def associatedEntity = prop.associatedEntity
                def javaClass = associatedEntity?.javaClass
                if(javaClass || isBasic) {
                    mc."addTo${prop.capitilizedName}" = { arg ->
                        def obj
                        if (delegate[prop.name] == null) {
                            delegate[prop.name] = [].asType(prop.type)
                        }
                        if (arg instanceof Map) {
                            obj = javaClass.newInstance(arg)
                            delegate[prop.name].add(obj)
                        }
                        else if (isBasic) {
                            delegate[prop.name].add(arg)
                            return delegate
                        }
                        else if (javaClass.isInstance(arg)) {
                            obj = arg
                            delegate[prop.name].add(obj)
                        }
                        else {
                            throw new MissingMethodException("addTo${prop.capitilizedName}", e.javaClass, [arg] as Object[])
                        }
                        if (prop.bidirectional && prop.inverseSide) {
                            def otherSide = prop.inverseSide
                            String name = otherSide.name
                            if (otherSide instanceof OneToMany || otherSide instanceof ManyToMany) {
                                if (obj[name] == null) {
                                    obj[name] = [].asType(otherSide.type)
                                }
                                obj[name].add(delegate)
                            }
                            else {
                                obj[name] = delegate
                            }
                        }
                        delegate
                    }
                    mc."removeFrom${prop.capitilizedName}" = { arg ->
                        if (javaClass.isInstance(arg)) {
                            delegate[prop.name]?.remove(arg)
                            if (prop.bidirectional) {
                                def otherSide = prop.inverseSide
                                if (otherSide instanceof ManyToMany) {
                                    String name = otherSide.name
                                    arg[name]?.remove(delegate)
                                }
                                else {
                                    arg[otherSide.name] = null
                                }
                            }
                        }
                        else {
                            throw new MissingMethodException("removeFrom${prop.capitilizedName}", e.javaClass, [arg] as Object[])
                        }
                        delegate
                    }

                }
            }
        }

        def staticScope = mc.static
        staticScope.currentGormStaticApi = {-> staticMethods }
        for (Method m in (onlyExtendedMethods ? staticMethods.extendedMethods : staticMethods.methods)) {
            def method = m
            if (method != null) {
                def methodName = method.name
                def parameterTypes = method.parameterTypes
                if (parameterTypes != null) {
                    def callable = new StaticMethodInvokingClosure(staticMethods, methodName, parameterTypes)
                    staticScope."$methodName" = callable
                }
            }
        }
    }

    protected void registerNamedQueries(PersistentEntity entity, namedQueries) {
        new NamedQueriesBuilder(entity, finders).evaluate namedQueries
    }

    protected <D> GormStaticApi<D> getStaticApi(Class<D> cls) {
        new GormStaticApi<D>(cls, datastore, finders)
    }

    protected <D> GormInstanceApi<D> getInstanceApi(Class<D> cls) {
        def instanceApi = new GormInstanceApi<D>(cls, datastore)
        instanceApi.failOnError = failOnError
        return instanceApi
    }

    protected <D> GormValidationApi<D> getValidationApi(Class<D> cls) {
        new GormValidationApi(cls, datastore)
    }

    protected List<FinderMethod> getAllDynamicFinders() {
        [new FindOrCreateByFinder(datastore),
         new FindOrSaveByFinder(datastore),
         new FindByFinder(datastore),
         new FindAllByFinder(datastore),
         new FindAllByBooleanFinder(datastore),
         new FindByBooleanFinder(datastore),
         new CountByFinder(datastore),
         new ListOrderByFinder(datastore)]
    }

    private List initialiseFinders() {
        this.finders = Collections.unmodifiableList(getAllDynamicFinders())
    }
}

