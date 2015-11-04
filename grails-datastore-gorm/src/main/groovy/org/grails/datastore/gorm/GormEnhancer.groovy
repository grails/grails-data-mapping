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

import groovy.transform.CompileStatic
import groovy.util.logging.Commons
import org.codehaus.groovy.reflection.CachedMethod
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.metaclass.ClosureStaticMetaMethod
import org.codehaus.groovy.runtime.metaclass.MethodSelectionException
import org.grails.datastore.gorm.finders.*
import org.grails.datastore.gorm.internal.InstanceMethodInvokingClosure
import org.grails.datastore.gorm.internal.StaticMethodInvokingClosure
import org.grails.datastore.gorm.query.NamedQueriesBuilder
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.grails.datastore.mapping.reflect.MetaClassUtils
import org.springframework.transaction.PlatformTransactionManager

import java.lang.reflect.Method
import java.lang.reflect.Modifier
/**
 * Enhances a class with GORM behavior
 *
 * @author Graeme Rocher
 */
@Commons
class GormEnhancer implements Closeable {

    Datastore datastore
    PlatformTransactionManager transactionManager
    List<FinderMethod> finders
    boolean failOnError
    boolean includeExternal = true

    GormEnhancer(Datastore datastore) {
        this(datastore, null)
    }

    GormEnhancer(Datastore datastore, PlatformTransactionManager transactionManager) {
        this.datastore = datastore
        this.transactionManager = transactionManager
        if(datastore != null) {
            registerConstraints(datastore)
        }
    }

    @Override
    @CompileStatic
    void close() throws IOException {
        removeConstraints()
        def registry = GroovySystem.metaClassRegistry
        for(entity in datastore.mappingContext.persistentEntities) {
            def cls = entity.javaClass
            try {
                registry.removeMetaClass(cls)
                InvokerHelper.invokeStaticMethod(cls, "resetInternalApi", null)
                InvokerHelper.invokeStaticMethod(cls, "resetInternalValidationApi", null)
            } catch (Exception ex) {
                log.warn("There was an error shutting down GORM for entity [$cls.name]: ${ex.message}", ex)
            }
        }
    }

    protected void removeConstraints() {
        try {
            Thread.currentThread().contextClassLoader.loadClass("org.codehaus.groovy.grails.validation.ConstrainedProperty").removeConstraint('unique')
        } catch (Throwable e) {
            log.warn("Error removing applied constraints on shutdown. ${e.message}", e)
        }
    }

    protected void registerConstraints(Datastore datastore) {
        try {
            Thread.currentThread().contextClassLoader.loadClass("org.grails.datastore.gorm.support.ConstraintRegistrar").newInstance(datastore)
        } catch (Throwable e) {
            log.error("Unable to register GORM constraints: $e.message", e)
        }
    }

    @CompileStatic
    List<FinderMethod> getFinders() {
        if (finders == null) {
            finders = Collections.unmodifiableList(createDynamicFinders())
        }
        finders
    }

    /**
     * Enhances all persistent entities.
     *
     * @param onlyExtendedMethods If only to add additional methods provides by subclasses of the GORM APIs
     */
    @CompileStatic
    void enhance(boolean onlyExtendedMethods = false) {
        for (PersistentEntity e in datastore.mappingContext.persistentEntities) {
            if(e.external && !includeExternal) continue
            enhance e, onlyExtendedMethods
        }
    }
    
    /**
     * Enhance and individual entity
     *
     * @param e The entity
     * @param onlyExtendedMethods If only to add additional methods provides by subclasses of the GORM APIs
     */
    @CompileStatic
    void enhance(PersistentEntity e, boolean onlyExtendedMethods = false) {
        def cls = e.javaClass
        try {
            InvokerHelper.invokeStaticMethod(cls, "initInternalApi", getInstanceApi(cls))
        } catch (Exception ex) {
        }
        try {
            InvokerHelper.invokeStaticMethod(cls, "initInternalValidationApi", getValidationApi(cls))
        } catch (Exception ex) {
        }
        try {
            InvokerHelper.invokeStaticMethod(cls, "initInternalStaticApi", getStaticApi(cls))
        } catch (Exception ex) {
        }

        addNamedQueryMethods(e)

        addInstanceMethods(e, onlyExtendedMethods)

        addStaticMethods(e, onlyExtendedMethods)
    }

    @CompileStatic
    protected void addStaticMethods(PersistentEntity e, boolean onlyExtendedMethods) {
        def cls = e.javaClass
        ExpandoMetaClass mc = MetaClassUtils.getExpandoMetaClass(cls)
        def staticApiProvider = getStaticApi(cls)
        for (Method m in (onlyExtendedMethods ? staticApiProvider.extendedMethods : staticApiProvider.methods)) {
            def method = m
            if (method != null) {
                def methodName = method.name
                def parameterTypes = method.parameterTypes
                if (parameterTypes != null) {
                    boolean realMethodExists = doesRealMethodExist(mc, methodName, parameterTypes, true)
                    if(!realMethodExists) {
                        registerStaticMethod(mc, methodName, parameterTypes, staticApiProvider)
                    }
                }
            }
        }
    }

    protected void registerStaticMethod(ExpandoMetaClass mc, String methodName, Class<?>[] parameterTypes, GormStaticApi staticApiProvider) {
        def callable = new StaticMethodInvokingClosure(staticApiProvider, methodName, parameterTypes)
        mc.static."$methodName" = callable
    }



    protected void addNamedQueryMethods(PersistentEntity e) {
        def cpf = ClassPropertyFetcher.forClass(e.javaClass)
        List<Closure> namedQueries = cpf.getStaticPropertyValuesFromInheritanceHierarchy('namedQueries', Closure)
        for (int i = namedQueries.size(); i > 0; i--) {
            Closure closure = namedQueries.get(i - 1)
            registerNamedQueries(e, closure)
        }
    }

    @CompileStatic
    protected <D> List<AbstractGormApi<D>> getInstanceMethodApiProviders(Class cls) {
        [getInstanceApi(cls), getValidationApi(cls)]
    }

    @CompileStatic
    protected void addInstanceMethods(PersistentEntity e, boolean onlyExtendedMethods) {
        Class cls = e.javaClass
        ExpandoMetaClass mc = MetaClassUtils.getExpandoMetaClass(cls)
        for (AbstractGormApi apiProvider in getInstanceMethodApiProviders(cls)) {

            for (Method method in (onlyExtendedMethods ? apiProvider.extendedMethods : apiProvider.methods)) {
                def methodName = method.name
                Class[] parameterTypes = method.parameterTypes

                if (parameterTypes) {
                    parameterTypes = parameterTypes.length == 1 ? []: parameterTypes[1..-1]

                    boolean realMethodExists = doesRealMethodExist(mc, methodName, parameterTypes, false)

                    if(!realMethodExists) {
                        registerInstanceMethod(cls, mc, apiProvider, methodName, parameterTypes)
                    }
                }
            }
        }
    }

    protected registerInstanceMethod(Class cls, ExpandoMetaClass mc, AbstractGormApi apiProvider, String methodName, Class[] parameterTypes) {
        // use fake object just so we have the right method signature
        final tooCall = new InstanceMethodInvokingClosure(apiProvider, cls, methodName, parameterTypes)
        def pt = parameterTypes
        // Hack to workaround http://jira.codehaus.org/browse/GROOVY-4720
        final closureMethod = new ClosureStaticMetaMethod(methodName, cls, tooCall, pt) {
                    @Override
                    int getModifiers() { Modifier.PUBLIC }
                }
        mc.registerInstanceMethod(closureMethod)
    }

    @CompileStatic
    protected static boolean doesRealMethodExist(final MetaClass mc, final String methodName, final Class[] parameterTypes, boolean staticScope) {
        boolean realMethodExists = false
        try {
            MetaMethod existingMethod = mc.pickMethod(methodName, parameterTypes)
            if(existingMethod && existingMethod.isStatic()==staticScope && isRealMethod(existingMethod) && parameterTypes.length==existingMethod.parameterTypes.length)  {
                realMethodExists = true
            }
        } catch (MethodSelectionException mse) {
            // the metamethod already exists with multiple signatures, must check if the exact method exists
            realMethodExists = mc.methods.contains { MetaMethod existingMethod ->
                existingMethod.name == methodName && existingMethod.isStatic()==staticScope && isRealMethod(existingMethod) && ((!parameterTypes && !existingMethod.parameterTypes) || parameterTypes==existingMethod.parameterTypes)
            }
        }
        return realMethodExists
    }

    @CompileStatic
    protected static boolean isRealMethod(MetaMethod existingMethod) {
        existingMethod instanceof CachedMethod
    }


    @CompileStatic
    protected void registerNamedQueries(PersistentEntity entity, Closure namedQueries) {
        new NamedQueriesBuilder(entity, getFinders()).evaluate(namedQueries)
    }

    @CompileStatic
    protected <D> GormStaticApi<D> getStaticApi(Class<D> cls) {
        new GormStaticApi<D>(cls, datastore, getFinders(), transactionManager)
    }

    @CompileStatic
    protected <D> GormInstanceApi<D> getInstanceApi(Class<D> cls) {
        def instanceApi = new GormInstanceApi<D>(cls, datastore)
        instanceApi.failOnError = failOnError
        return instanceApi
    }

    @CompileStatic
    protected <D> GormValidationApi<D> getValidationApi(Class<D> cls) {
        new GormValidationApi(cls, datastore)
    }

    @CompileStatic
    protected List<FinderMethod> createDynamicFinders() {
        [new FindOrCreateByFinder(datastore),
         new FindOrSaveByFinder(datastore),
         new FindByFinder(datastore),
         new FindAllByFinder(datastore),
         new FindAllByBooleanFinder(datastore),
         new FindByBooleanFinder(datastore),
         new CountByFinder(datastore),
         new ListOrderByFinder(datastore)]
    }
}
