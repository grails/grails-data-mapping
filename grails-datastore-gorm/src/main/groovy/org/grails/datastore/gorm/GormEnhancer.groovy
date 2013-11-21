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

import grails.util.GrailsNameUtils

import java.lang.reflect.Method
import java.lang.reflect.Modifier

import org.codehaus.groovy.grails.commons.GrailsMetaClassUtils
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.codehaus.groovy.reflection.CachedMethod
import org.codehaus.groovy.runtime.metaclass.ClosureStaticMetaMethod
import org.codehaus.groovy.runtime.metaclass.MethodSelectionException
import org.grails.datastore.gorm.finders.CountByFinder
import org.grails.datastore.gorm.finders.FindAllByBooleanFinder
import org.grails.datastore.gorm.finders.FindAllByFinder
import org.grails.datastore.gorm.finders.FindByBooleanFinder
import org.grails.datastore.gorm.finders.FindByFinder
import org.grails.datastore.gorm.finders.FindOrCreateByFinder
import org.grails.datastore.gorm.finders.FindOrSaveByFinder
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.gorm.finders.ListOrderByFinder
import org.grails.datastore.gorm.internal.InstanceMethodInvokingClosure
import org.grails.datastore.gorm.internal.StaticMethodInvokingClosure
import org.grails.datastore.gorm.query.NamedQueriesBuilder
import org.grails.datastore.gorm.validation.constraints.UniqueConstraintFactory
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.Basic
import org.grails.datastore.mapping.model.types.EmbeddedCollection
import org.grails.datastore.mapping.model.types.ManyToMany
import org.grails.datastore.mapping.model.types.OneToMany
import org.grails.datastore.mapping.model.types.ToOne
import org.grails.datastore.mapping.proxy.ProxyFactory
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.springframework.transaction.PlatformTransactionManager
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable
import org.grails.datastore.mapping.model.types.Association

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
        if(datastore != null) {
            registerConstraints(datastore)
        }
    }

    protected void registerConstraints(Datastore datastore) {
        ConstrainedProperty.registerNewConstraint("unique", new UniqueConstraintFactory(datastore))
    }

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
    void enhance(boolean onlyExtendedMethods = false) {
        for (PersistentEntity e in datastore.mappingContext.persistentEntities) {
            enhance e, onlyExtendedMethods
        }
    }
    
    /**
     * Enhance and individual entity
     *
     * @param e The entity
     * @param onlyExtendedMethods If only to add additional methods provides by subclasses of the GORM APIs
     */
    void enhance(PersistentEntity e, boolean onlyExtendedMethods = false) {
        addNamedQueryMethods(e)

        addInstanceMethods(e, onlyExtendedMethods)

        addAssociationMethods(e)

        addStaticMethods(e, onlyExtendedMethods)
    }
    
    protected void addStaticMethods(PersistentEntity e, boolean onlyExtendedMethods) {
        def cls = e.javaClass
        ExpandoMetaClass mc = GrailsMetaClassUtils.getExpandoMetaClass(cls)
        def staticApiProvider = getStaticApi(cls)
        registerApiInstance(cls, staticApiProvider.getClass(), staticApiProvider, true)
        for (Method m in (onlyExtendedMethods ? staticApiProvider.extendedMethods : staticApiProvider.methods)) {
            def method = m
            if (method != null) {
                def methodName = method.name
                def parameterTypes = method.parameterTypes
                if (parameterTypes != null) {
                    boolean realMethodExists = doesRealMethodExist(mc, methodName, parameterTypes, true)
                    if(!realMethodExists) {
                        def callable = new StaticMethodInvokingClosure(staticApiProvider, methodName, parameterTypes)
                        mc.static."$methodName" = callable
                    }
                }
            }
        }
    }
    
    protected void addAssociationMethods(PersistentEntity e) {
        Class cls = e.javaClass
        ExpandoMetaClass mc = GrailsMetaClassUtils.getExpandoMetaClass(cls)
        final proxyFactory = datastore.mappingContext.proxyFactory
        for (p in e.associations) {
            def prop = p
            def isBasic = prop instanceof Basic
            if(prop instanceof ToOne) {
                registerAssociationIdentifierGetter(proxyFactory, mc, (ToOne)prop)
            }
            else if ((prop instanceof OneToMany) || (prop instanceof ManyToMany) || isBasic || (prop instanceof EmbeddedCollection)) {
                def associatedEntity = prop.associatedEntity
                def javaClass = associatedEntity?.javaClass
                if(javaClass || isBasic) {
                    mc."addTo${prop.capitilizedName}" = { arg ->
                        def obj
                        final targetObject = delegate
                        if (targetObject[prop.name] == null) {
                            targetObject[prop.name] = [].asType(prop.type)
                        }
                        if (arg instanceof Map) {
                            obj = javaClass.newInstance(arg)
                            addObjectToCollection(prop, targetObject, obj)
                        }
                        else if (isBasic) {
                            addObjectToCollection(prop, targetObject, arg)
                            return targetObject
                        }
                        else if (javaClass.isInstance(arg)) {
                            obj = arg
                            addObjectToCollection(prop, targetObject, obj)
                        }
                        else {
                            throw new MissingMethodException("addTo${prop.capitilizedName}", cls, [arg] as Object[])
                        }
                        if (prop.bidirectional && prop.inverseSide) {
                            def otherSide = prop.inverseSide
                            String name = otherSide.name
                            if (otherSide instanceof OneToMany || otherSide instanceof ManyToMany) {
                                if (obj[name] == null) {
                                    obj[name] = [].asType(otherSide.type)
                                }
                                addObjectToCollection(otherSide, obj, targetObject)
                            }
                            else {
                                obj[name] = targetObject
                            }
                        }
                        targetObject
                    }
                    mc."removeFrom${prop.capitilizedName}" = { arg ->
                        if (javaClass.isInstance(arg)) {
                            final targetObject = delegate
                            targetObject[prop.name]?.remove(arg)
                            if (targetObject instanceof DirtyCheckable) {
                                ((DirtyCheckable)targetObject).markDirty(prop.name)
                            }
                            if (prop.bidirectional) {
                                def otherSide = prop.inverseSide
                                if (otherSide instanceof ManyToMany) {
                                    String name = otherSide.name
                                    arg[name]?.remove(targetObject)
                                }
                                else {
                                    arg[otherSide.name] = null
                                }
                            }
                        }
                        else {
                            throw new MissingMethodException("removeFrom${prop.capitilizedName}", cls, [arg] as Object[])
                        }
                        delegate
                    }
                }
            }
        }
    }

    protected void addObjectToCollection(Association prop, targetObject, obj) {
        targetObject[prop.name].add(obj)
        if (targetObject instanceof DirtyCheckable) {
            targetObject.markDirty(prop.name)
        }
    }

    protected void addNamedQueryMethods(PersistentEntity e) {
        def cpf = ClassPropertyFetcher.forClass(e.javaClass)
        List<Closure> namedQueries = cpf.getStaticPropertyValuesFromInheritanceHierarchy('namedQueries', Closure)
        for (int i = namedQueries.size(); i > 0; i--) {
            Closure closure = namedQueries.get(i - 1)
            registerNamedQueries(e, closure)
        }
    }

    protected <D> List<AbstractGormApi<D>> getInstanceMethodApiProviders(Class cls) {
        [getInstanceApi(cls), getValidationApi(cls)]
    }
    
    protected void addInstanceMethods(PersistentEntity e, boolean onlyExtendedMethods) {
        Class cls = e.javaClass
        ExpandoMetaClass mc = GrailsMetaClassUtils.getExpandoMetaClass(cls)
        for (AbstractGormApi apiProvider in getInstanceMethodApiProviders(cls)) {
            registerApiInstance(cls, apiProvider.getClass(), apiProvider, false)

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
    
    void registerApiInstance (Class cls, Class apiProviderType, AbstractGormApi apiProvider, boolean staticApi) {
        Class apiProviderBaseClass = apiProviderType
        while(apiProviderBaseClass.getSuperclass() != AbstractGormApi) {
            apiProviderBaseClass = apiProviderBaseClass.getSuperclass()
        }
        String lookupMethodName = "current" + apiProviderBaseClass.getSimpleName()
        String setterName = "set" + (staticApi ? "Static" : "Instance") + apiProviderBaseClass.getSimpleName()
        ExpandoMetaClass mc = GrailsMetaClassUtils.getExpandoMetaClass(cls)
        MetaMethod setterMethod = mc.pickMethod(setterName, [apiProviderType] as Class[])
        if(mc.pickMethod(lookupMethodName, [] as Class[]) && setterMethod && setterMethod.isStatic()) {
            setterMethod.invoke(cls, [apiProvider] as Object[])
        } else {
            mc.static."$lookupMethodName" = {-> apiProvider }
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
    }
        
    protected static boolean isRealMethod(MetaMethod existingMethod) {
        existingMethod instanceof CachedMethod
    }

    protected void registerAssociationIdentifierGetter(ProxyFactory proxyFactory, MetaClass metaClass, ToOne association) {
        final propName = association.name
        final getterName = GrailsNameUtils.getGetterName(propName)
        metaClass."${getterName}Id" = {->
            final associationInstance = delegate.getProperty(propName)
            if (associationInstance != null) {
                if (proxyFactory.isProxy(associationInstance)) {
                    return proxyFactory.getIdentifier(associationInstance)
                }
                else {
                    return datastore.currentSession.getObjectIdentifier(associationInstance)
                }
            }
        }
    }

    protected void registerNamedQueries(PersistentEntity entity, Closure namedQueries) {
        new NamedQueriesBuilder(entity, getFinders()).evaluate(namedQueries)
    }

    protected <D> GormStaticApi<D> getStaticApi(Class<D> cls) {
        new GormStaticApi<D>(cls, datastore, getFinders(), transactionManager)
    }

    protected <D> GormInstanceApi<D> getInstanceApi(Class<D> cls) {
        def instanceApi = new GormInstanceApi<D>(cls, datastore)
        instanceApi.failOnError = failOnError
        return instanceApi
    }

    protected <D> GormValidationApi<D> getValidationApi(Class<D> cls) {
        new GormValidationApi(cls, datastore)
    }

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
