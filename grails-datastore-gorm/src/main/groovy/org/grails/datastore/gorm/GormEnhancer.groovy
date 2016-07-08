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

import grails.gorm.MultiTenant
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.codehaus.groovy.reflection.CachedMethod
import org.codehaus.groovy.runtime.metaclass.ClosureStaticMetaMethod
import org.codehaus.groovy.runtime.metaclass.MethodSelectionException
import org.grails.datastore.gorm.finders.*
import org.grails.datastore.gorm.internal.InstanceMethodInvokingClosure
import org.grails.datastore.gorm.internal.StaticMethodInvokingClosure
import org.grails.datastore.gorm.query.GormQueryOperations
import org.grails.datastore.gorm.query.NamedQueriesBuilder
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.core.DatastoreAware
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettings
import org.grails.datastore.mapping.core.connections.ConnectionSourcesProvider
import org.grails.datastore.mapping.core.connections.ConnectionSourcesSupport
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.config.GormProperties
import org.grails.datastore.mapping.multitenancy.MultiTenancySettings
import org.grails.datastore.mapping.multitenancy.TenantResolver
import org.grails.datastore.mapping.multitenancy.resolvers.FixedTenantResolver
import org.grails.datastore.mapping.multitenancy.resolvers.NoTenantResolver
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.grails.datastore.mapping.reflect.MetaClassUtils
import org.grails.datastore.mapping.reflect.NameUtils
import org.springframework.transaction.PlatformTransactionManager

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

/**
 * Enhances a class with GORM behavior
 *
 * @author Graeme Rocher
 */
@Slf4j
@CompileStatic
class GormEnhancer implements Closeable {

    private static final Map<String, Map<String, GormQueryOperations>> NAMED_QUERIES = new ConcurrentHashMap<>()

    private static final Map<String, Map<String,GormStaticApi>> STATIC_APIS = new ConcurrentHashMap<String, Map<String,GormStaticApi>>().withDefault { String key ->
        return new ConcurrentHashMap<String, GormStaticApi>()
    }
    private static final Map<String, Map<String, GormInstanceApi>> INSTANCE_APIS = new ConcurrentHashMap<String, Map<String, GormInstanceApi>>().withDefault { String key ->
        return new ConcurrentHashMap<String, GormInstanceApi>()
    }
    private static final Map<String, Map<String, GormValidationApi>> VALIDATION_APIS = new ConcurrentHashMap<String, Map<String, GormValidationApi>>().withDefault { String key ->
        return new ConcurrentHashMap<String, GormValidationApi>()
    }
    private static final Map<String, Map<String, Datastore>> DATASTORES = new ConcurrentHashMap<String, Map<String, Datastore>>().withDefault { String key ->
        return new ConcurrentHashMap<String, Datastore>()
    }
    private static final Map<String, TenantResolver> TENANT_RESOLVERS = new ConcurrentHashMap<String, TenantResolver>().withDefault { String key ->
        return new NoTenantResolver()
    }

    final Datastore datastore
    PlatformTransactionManager transactionManager
    List<FinderMethod> finders
    boolean failOnError

    /**
     * Whether to include external entities
     */
    boolean includeExternal = true
    /**
     * Whether to enhance classes dynamically using meta programming as well, only necessary for Java classes
     */
    final boolean dynamicEnhance

    /**
     * The tenant resolver
     */
    final TenantResolver tenantResolver

    GormEnhancer(Datastore datastore) {
        this(datastore, null)
    }

    GormEnhancer(Datastore datastore, PlatformTransactionManager transactionManager, boolean failOnError = false, boolean dynamicEnhance = false) {
        this(datastore, transactionManager, new ConnectionSourceSettings().failOnError(failOnError))
    }

    /**
     * Construct a new GormEnhancer for the given arguments
     *
     * @param datastore The datastore
     * @param transactionManager The transaction manager
     * @param settings The settings
     */
    GormEnhancer(Datastore datastore, PlatformTransactionManager transactionManager, ConnectionSourceSettings settings) {
        this.datastore = datastore
        this.failOnError = settings.isFailOnError()
        this.transactionManager = transactionManager
        this.dynamicEnhance = false
        MultiTenancySettings multiTenancySettings = settings.multiTenancy
        this.tenantResolver = multiTenancySettings.tenantResolverClass?.newInstance() ?: new NoTenantResolver()

        if(datastore != null) {
            registerConstraints(datastore)
        }
        NAMED_QUERIES.clear()


        if(multiTenancySettings.mode == MultiTenancySettings.MultiTenancyMode.SINGLE) {

            if(tenantResolver instanceof DatastoreAware) {
                ((DatastoreAware)tenantResolver).setDatastore(datastore)
            }
            for(entity in datastore.mappingContext.persistentEntities) {
                if(MultiTenant.isAssignableFrom(entity.javaClass)) {
                    registerEntity(entity, tenantResolver)
                }
                else {
                    registerEntity(entity)
                }
            }
        }
        else {
            for(entity in datastore.mappingContext.persistentEntities) {
                registerEntity(entity)
            }
        }
    }

    /**
     * Registers a new entity with the GORM enhancer
     *
     * @param entity The entity
     */
    void registerEntity(PersistentEntity entity, TenantResolver tenantResolver = new FixedTenantResolver()) {
        Datastore datastore = this.datastore
        if (appliesToDatastore(datastore, entity)) {
            def cls = entity.javaClass
            TENANT_RESOLVERS.put(cls.name, tenantResolver)

            List<String> qualifiers = allQualifiers(this.datastore, entity)
            if(!qualifiers.contains(ConnectionSource.DEFAULT)) {
                def firstQualifier = qualifiers.first()
                def staticApi = getStaticApi(cls, firstQualifier)
                def name = entity.name
                STATIC_APIS.get(ConnectionSource.DEFAULT).put(name, staticApi)
                def instanceApi = getInstanceApi(cls, firstQualifier)
                INSTANCE_APIS.get(ConnectionSource.DEFAULT).put(name, instanceApi)
                def validationApi = getValidationApi(cls, firstQualifier)
                VALIDATION_APIS.get(ConnectionSource.DEFAULT).put(name, validationApi)
                DATASTORES.get(ConnectionSource.DEFAULT).put(name, this.datastore)

            }
            for (qualifier in qualifiers) {
                def staticApi = getStaticApi(cls, qualifier)
                def name = entity.name
                STATIC_APIS.get(qualifier).put(name, staticApi)
                def instanceApi = getInstanceApi(cls, qualifier)
                INSTANCE_APIS.get(qualifier).put(name, instanceApi)
                def validationApi = getValidationApi(cls, qualifier)
                VALIDATION_APIS.get(qualifier).put(name, validationApi)
                DATASTORES.get(qualifier).put(name, this.datastore)
            }
        }
    }

    List<String> allQualifiers(Datastore datastore, PersistentEntity entity) {
        List<String> qualifiers = new ArrayList<>()
        qualifiers.addAll ConnectionSourcesSupport.getConnectionSourceNames(entity)
        if((MultiTenant.isAssignableFrom(entity.javaClass) || qualifiers.contains(ConnectionSource.ALL)) && (datastore instanceof ConnectionSourcesProvider)) {
            qualifiers.clear()
            qualifiers.add(ConnectionSource.DEFAULT)

            Iterable<ConnectionSource> allConnectionSources = ((ConnectionSourcesProvider) datastore).getConnectionSources().allConnectionSources
            Collection<String> allConnectionSourceNames = allConnectionSources.findAll() { ConnectionSource connectionSource -> connectionSource.name != ConnectionSource.DEFAULT }
                                                                              .collect() { ((ConnectionSource)it).name }
            qualifiers.addAll allConnectionSourceNames
        }
        return qualifiers
    }

    protected boolean appliesToDatastore(Datastore datastore, PersistentEntity entity) {
        !entity.isExternal()
    }

    /**
     * Finds a named query for the given entity
     *
     * @param entity The entity name
     * @param queryName The query name
     *
     * @return The named query or null if it doesn't exist
     */
    static GormQueryOperations findNamedQuery(Class entity, String queryName) {
        def className = entity.getName()
        def namedQueries = NAMED_QUERIES.get(className)
        if(namedQueries == null) {
            def cpf = ClassPropertyFetcher.forClass(entity)
            def closure = cpf.getStaticPropertyValue(GormProperties.NAMED_QUERIES, Closure.class)
            if(closure != null) {

                def evaluator = new NamedQueriesBuilder(findEntity(entity), findStaticApi(entity).gormDynamicFinders)
                namedQueries = evaluator.evaluate(closure)
                NAMED_QUERIES.put(className, namedQueries)
                return namedQueries.get(queryName)
            }
            else {
                NAMED_QUERIES.put(className, new LinkedHashMap<String, GormQueryOperations>());
            }

        }
        else {
            return namedQueries.get(queryName)
        }
        return null
    }

    /**
     * Find the {@link TenantResolver} for the given entity
     *
     * @param entity The entity
     * @return The {@link TenantResolver}
     */
    static TenantResolver findTenantResolver(Class entity) {
        String className = NameUtils.getClassName(entity)
        return TENANT_RESOLVERS.get(className)
    }

    /**
     * Find the tenant id for the given entity
     *
     * @param entity
     * @return
     */
    static String findTenantId(Class entity) {
        if(MultiTenant.isAssignableFrom(entity)) {
            def tenantId = CurrentTenant.get()
            if(tenantId != null) {
                log.debug "Found tenant id [$tenantId] bound to thread local"
                return tenantId
            }
            else {
                TenantResolver tenantResolver = findTenantResolver(entity)
                def tenantIdentifier = tenantResolver.resolveTenantIdentifier(entity)
                log.debug "Resolved tenant id [$tenantIdentifier] from resolver [${tenantResolver.getClass().simpleName}] for class [$entity]"
                return tenantIdentifier
            }
        }
        else {
            log.debug "Returning default tenant id for non-multitenant class [$entity]"
            return ConnectionSource.DEFAULT
        }
    }


    @CompileDynamic
    static <D> GormStaticApi<D> findStaticApi(Class<D> entity, String qualifier = findTenantId(entity)) {
        String className = NameUtils.getClassName(entity)
        def staticApi = STATIC_APIS.get(qualifier)?.get(className)
        if(staticApi == null) {
            throw stateException(entity)
        }
        return staticApi
    }

    private static IllegalStateException stateException(Class entity) {
        new IllegalStateException("Either class [$entity.name] is not a domain class or GORM has not been initialized correctly or has already been shutdown. If you are unit testing your entities using the mocking APIs")
    }

    static GormInstanceApi findInstanceApi(Class entity, String qualifier = findTenantId(entity)) {
        def instanceApi = INSTANCE_APIS.get(qualifier)?.get(NameUtils.getClassName(entity))
        if(instanceApi == null) {
            throw stateException(entity)
        }
        return instanceApi
    }

    static  GormValidationApi findValidationApi(Class entity, String qualifier = findTenantId(entity)) {
        def instanceApi = VALIDATION_APIS.get(qualifier)?.get(NameUtils.getClassName(entity))
        if(instanceApi == null) {
            throw stateException(entity)
        }
        return instanceApi
    }

    static Datastore findDatastore(Class entity, String qualifier = findTenantId(entity)) {
        def datastore = DATASTORES.get(qualifier)?.get(entity.name)
        if(datastore == null) {
            throw stateException(entity)
        }
        return datastore
    }

    static PersistentEntity findEntity(Class entity) {
        findDatastore(entity).getMappingContext().getPersistentEntity(entity.name)
    }

    @Override
    @CompileStatic
    void close() throws IOException {
        removeConstraints()
        def registry = GroovySystem.metaClassRegistry
        for(entity in datastore.mappingContext.persistentEntities) {

            List<String> qualifiers = allQualifiers(datastore, entity)
            def cls = entity.javaClass
            def className = cls.name
            TENANT_RESOLVERS.remove(className)
            for(q in qualifiers) {
                NAMED_QUERIES.remove(className)
                STATIC_APIS.get(q)?.remove(className)
                INSTANCE_APIS.get(q)?.remove(className)
                VALIDATION_APIS.get(q)?.remove(className)
                DATASTORES.get(q)?.remove(datastore)
            }
            registry.removeMetaClass(cls)
        }
    }

    @CompileDynamic
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
        if(dynamicEnhance) {
            for (PersistentEntity e in datastore.mappingContext.persistentEntities) {
                if(e.external && !includeExternal) continue
                enhance e, onlyExtendedMethods
            }
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
        registerEntity(e)

        if(!(GroovyObject.isAssignableFrom(e.javaClass) ) || dynamicEnhance) {
            addInstanceMethods(e, onlyExtendedMethods)

            addStaticMethods(e, onlyExtendedMethods)
        }
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

    @CompileDynamic
    protected void registerStaticMethod(ExpandoMetaClass mc, String methodName, Class<?>[] parameterTypes, GormStaticApi staticApiProvider) {
        def callable = new StaticMethodInvokingClosure(staticApiProvider, methodName, parameterTypes)
        mc.static."$methodName" = callable
    }



    @CompileDynamic
    protected <D> List<AbstractGormApi<D>> getInstanceMethodApiProviders(Class<D> cls) {
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
    protected <D> GormStaticApi<D> getStaticApi(Class<D> cls, String qualifier = ConnectionSource.DEFAULT) {
        new GormStaticApi<D>(cls, datastore, getFinders(), transactionManager)
    }

    @CompileStatic
    protected <D> GormInstanceApi<D> getInstanceApi(Class<D> cls, String qualifier = ConnectionSource.DEFAULT) {
        def instanceApi = new GormInstanceApi<D>(cls, datastore)
        instanceApi.failOnError = failOnError
        return instanceApi
    }

    @CompileStatic
    protected <D> GormValidationApi<D> getValidationApi(Class<D> cls, String qualifier = ConnectionSource.DEFAULT) {
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
         new ListOrderByFinder(datastore)] as List<FinderMethod>
    }
}
