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

import java.lang.reflect.Method
import java.lang.reflect.Modifier

import org.codehaus.groovy.runtime.metaclass.ClosureStaticMetaMethod
import org.grails.datastore.gorm.finders.DynamicFinder
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.gorm.query.NamedQueriesBuilder
import org.springframework.datastore.mapping.core.Datastore
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.model.types.ManyToMany
import org.springframework.datastore.mapping.model.types.OneToMany
import org.springframework.datastore.mapping.reflect.ClassPropertyFetcher
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate

/**
 * Enhances a class with GORM behavior
 *
 * @author Graeme Rocher
 */
class GormEnhancer {

    Datastore datastore
    PlatformTransactionManager transactionManager
    List<DynamicFinder> finders

    GormEnhancer(Datastore datastore) {
        this.datastore = datastore
        initialiseFinders(datastore)
    }

    private List initialiseFinders(Datastore datastore) {
        this.finders = DynamicFinder.getAllDynamicFinders(datastore)
    }

    GormEnhancer(Datastore datastore, PlatformTransactionManager transactionManager) {
        this.datastore = datastore
        initialiseFinders(datastore)
        this.transactionManager = transactionManager
    }

    void enhance() {
        for (PersistentEntity e in datastore.mappingContext.persistentEntities) {
            enhance e
        }
    }

    void enhance(PersistentEntity e) {
        def cls = e.javaClass
        def cpf = ClassPropertyFetcher.forClass(cls)
        def staticMethods = getStaticApi(cls)
        def instanceMethods = [getInstanceApi(cls), getValidationApi(cls)]
        def tm = transactionManager

        final namedQueries = cpf.getStaticPropertyValue('namedQueries', Closure)
        if (namedQueries) {
            if (namedQueries instanceof Closure) {
                registerNamedQueries(e, namedQueries)
            }
        }

        ExpandoMetaClass mc = cls.metaClass
        for (currentInstanceMethods in instanceMethods) {
            def apiProvider = currentInstanceMethods
            for (Method method in apiProvider.methods) {
                def methodName = method.name
                def parameterTypes = method.parameterTypes

                if (parameterTypes) {
                    parameterTypes = Arrays.copyOfRange(parameterTypes, 1, parameterTypes.length)
                    // use fake object just so we have the right method signature

                    final tooCall = new InstanceMethodInvokingClosure(apiProvider,methodName, parameterTypes)
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
            if ( (prop instanceof OneToMany) || (prop instanceof ManyToMany)) {
                def associatedEntity = prop.associatedEntity
                mc."addTo${prop.capitilizedName}" = { arg ->
                    def obj
                    if (delegate[prop.name] == null) {
                        delegate[prop.name] = [].asType( prop.type )
                    }
                    if (arg instanceof Map) {
                        obj = associatedEntity.javaClass.newInstance(arg)
                        delegate[prop.name].add(obj)
                    }
                    else if (associatedEntity.javaClass.isInstance(arg)) {
                        obj = arg
                        delegate[prop.name].add(obj)
                    }
                    else {
                        throw new MissingMethodException("addTo${prop.capitilizedName}", e.javaClass, [arg] as Object[])
                    }
                    if (prop.bidirectional && prop.inverseSide) {
                        def otherSide = prop.inverseSide
                        if (otherSide instanceof OneToMany) {
                            String name = otherSide.name
                            if (!obj[name]) {
                                obj[name] = [].asType(otherSide.type)
                            }
                            obj[otherSide.name].add(delegate)
                        }
                        else {
                            obj[otherSide.name] = delegate
                        }
                    }
                    delegate
                }
                mc."removeFrom${prop.capitilizedName}" = {Object arg ->
                    if (associatedEntity.javaClass.isInstance(arg)) {
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

        def staticScope = mc.static
        for (Method m in staticMethods.methods) {
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

        if (tm) {
            staticScope.withTransaction = { Closure callable ->
                if (callable) {
                    def transactionTemplate = new TransactionTemplate(tm)
                    transactionTemplate.execute(callable as TransactionCallback)
                }
            }
            staticScope.withNewTransaction = { Closure callable ->
                if (callable) {
                    def transactionTemplate = new TransactionTemplate(tm, new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW))
                    transactionTemplate.execute(callable as TransactionCallback)
                }
            }
            staticScope.withTransaction = { TransactionDefinition definition, Closure callable ->
                if (callable) {
                    def transactionTemplate = new TransactionTemplate(tm, definition)
                    transactionTemplate.execute(callable as TransactionCallback)
                }
            }
        }

        registerMethodMissing cls
    }

    protected void registerNamedQueries(PersistentEntity entity, namedQueries) {
        def namedQueryBuilder = new NamedQueriesBuilder(entity, finders)
        namedQueryBuilder.evaluate namedQueries
    }

    protected void registerMethodMissing(Class cls) {
        def mc = cls.metaClass
        def dynamicFinders = finders
        mc.static.methodMissing = {String methodName, args ->
            def method = dynamicFinders.find { FinderMethod f -> f.isMethodMatch(methodName) }
            if (method) {
                // register the method invocation for next time
                synchronized(this) {
                    mc.static."$methodName" = {List varArgs ->
                        method.invoke(cls, methodName, varArgs)
                    }
                }
                return method.invoke(cls, methodName, args)
            }
            else {
                throw new MissingMethodException(methodName, delegate, args)
            }
        }
    }

    protected GormStaticApi getStaticApi(Class cls) {
        new GormStaticApi(cls, datastore)
    }

    protected GormInstanceApi getInstanceApi(Class cls) {
        new GormInstanceApi(cls, datastore)
    }

    protected GormValidationApi getValidationApi(Class cls) {
        new GormValidationApi(cls, datastore)
    }
}

class InstanceMethodInvokingClosure extends Closure {
    private String methodName
    private Object apiDelegate
    private Class[] parameterTypes

    InstanceMethodInvokingClosure(Object apiDelegate, String methodName, Class[] parameterTypes) {
        super(apiDelegate, apiDelegate)
        this.apiDelegate = apiDelegate
        this.methodName = methodName
        this.parameterTypes = parameterTypes
    }

    @Override
    Object call(Object[] args) {
        apiDelegate."$methodName"(delegate, *args)
    }

    Object doCall(Object[] args) {
        apiDelegate."$methodName"(delegate, *args)
    }

    @Override
    Class[] getParameterTypes() { parameterTypes }
}

class StaticMethodInvokingClosure extends Closure {

    private String methodName
    private Object apiDelegate
    private Class[] parameterTypes

    StaticMethodInvokingClosure(Object apiDelegate, String methodName, Class[] parameterTypes) {
        super(apiDelegate, apiDelegate)
        this.apiDelegate = apiDelegate
        this.methodName = methodName
        this.parameterTypes = parameterTypes
    }

    @Override
    Object call(Object[] args) {
        apiDelegate."$methodName"(*args)
    }

    Object doCall(Object[] args) {
        apiDelegate."$methodName"(*args)
    }

    @Override
    Class[] getParameterTypes() { parameterTypes }
}
