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

import grails.gorm.CriteriaBuilder

import org.grails.datastore.gorm.finders.DynamicFinder
import org.grails.datastore.gorm.finders.FinderMethod
import org.springframework.beans.PropertyAccessorFactory
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.grails.datastore.mapping.core.AbstractDatastore
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.core.SessionCallback
import org.grails.datastore.mapping.core.VoidSessionCallback
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.PersistentProperty
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.query.Query
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.util.Assert
import org.springframework.validation.Errors
import org.grails.datastore.mapping.query.api.Criteria
import grails.gorm.DetachedCriteria

/**
 * Static methods of the GORM API.
 *
 * @author Graeme Rocher
 * @param <D> the entity/domain class
 */
class GormStaticApi<D> extends AbstractGormApi<D> {

    private List<FinderMethod> dynamicFinders
    private PlatformTransactionManager transactionManager

    GormStaticApi(Class<D> persistentClass, Datastore datastore, List<FinderMethod> finders) {
        this(persistentClass, datastore, finders, null)
    }

    GormStaticApi(Class<D> persistentClass, Datastore datastore, List<FinderMethod> finders, PlatformTransactionManager transactionManager) {
        super(persistentClass, datastore)
        this.dynamicFinders = finders
        this.transactionManager = transactionManager
    }

    /**
     * Sets the {@link PlatformTransactionManager} to use
     */
    void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager
    }

    /**
     * @return The FinderMethods for this class
     */
    List<FinderMethod> getGormDynamicFinders() {
        this.dynamicFinders
    }

    /**
     * @return The PersistentEntity for this class
     */
    PersistentEntity getGormPersistentEntity() {
        this.persistentEntity
    }

    /**
     * Method missing handler that deals with the invocation of dynamic finders
     *
     * @param methodName The method name
     * @param args The arguments
     * @return The result of the method call
     */
    def methodMissing(String methodName, args) {
        def method = dynamicFinders.find { FinderMethod f -> f.isMethodMatch(methodName) }
        def cls = persistentClass
        if (!method) {
            throw new MissingMethodException(methodName, cls, args)
        }

        def mc = cls.metaClass

        // register the method invocation for next time
        synchronized(this) {
            mc.static."$methodName" = { List varArgs ->
                method.invoke(cls, methodName, varArgs)
            }
        }
        return method.invoke(cls, methodName, args)
    }

    /**
     *
     * @param callable Callable closure containing detached criteria definition
     * @return The DetachedCriteria instance
     */
    DetachedCriteria<D> where(Closure callable) {
        def criteria = new DetachedCriteria(persistentClass)
        criteria.build callable
        return criteria
    }

    /**
     * Uses detached criteria to build a query and then execute it returning a list
     *
     * @param callable The callable
     * @return A List of entities
     */
    List<D> findAll(Closure callable) {
        def criteria = new DetachedCriteria(persistentClass)
        criteria.build callable
        return criteria.list()
    }

    /**
     * Uses detached criteria to build a query and then execute it returning a list
     *
     * @param callable The callable
     * @return A single entity
     */
    D find(Closure callable) {
        def criteria = new DetachedCriteria(persistentClass)
        criteria.build callable
        return criteria.find()
    }

    /**
     * Saves a list of objects in one go
     * @param objectsToSave The objects to save
     * @return A list of object identifiers
     */
    List<Serializable> saveAll(D... objectsToSave) {
        execute({ Session session ->
           session.persist Arrays.asList(objectsToSave)
        } as SessionCallback)
    }

    /**
     * Deletes a list of objects in one go
     * @param objectsToDelete The objects to delete
     */
    void deleteAll(D... objectsToDelete) {
        execute({ Session session ->
           session.delete objectsToDelete
        } as SessionCallback)
    }

    /**
     * Creates an instance of this class
     * @return The created instance
     */
    D create() {
        D d = persistentClass.newInstance()
        datastore.applicationContext.autowireCapableBeanFactory.autowireBeanProperties(
              d, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false)
        d
    }

    /**
     * Retrieves and object from the datastore. eg. Book.get(1)
     */
    D get(Serializable id) {
        execute({ Session session ->
           session.retrieve(persistentClass, id)
        } as SessionCallback)
    }

    /**
     * Retrieves and object from the datastore. eg. Book.read(1)
     *
     * Since the datastore abstraction doesn't support dirty checking yet this
     * just delegates to {@link #get(Serializable)}
     */
    D read(Serializable id) {
        execute ({ Session session ->
           session.retrieve(persistentClass, id)
        } as SessionCallback)
    }

    /**
     * Retrieves and object from the datastore as a proxy. eg. Book.load(1)
     */
    D load(Serializable id) {
        execute ({ Session session ->
           session.proxy(persistentClass, id)
        } as SessionCallback)
    }

    /**
     * Retrieves and object from the datastore as a proxy. eg. Book.proxy(1)
     */
    D proxy(Serializable id) {
        load(id)
    }

    /**
     * Retrieve all the objects for the given identifiers
     * @param ids The identifiers to operate against
     * @return A list of identifiers
     */
    List<D> getAll(Serializable... ids) {
        execute ({ Session session ->
           session.retrieveAll(persistentClass, ids.flatten())
        } as SessionCallback)
    }

    /**
     * @return Synonym for {@link #list()}
     */
    List<D> getAll() {
        list()
    }

    /**
     * Creates a criteria builder instance
     */
    Criteria createCriteria() {
        new CriteriaBuilder(persistentClass, datastore.currentSession)
    }

    /**
     * Creates a criteria builder instance
     */
    def withCriteria(Closure callable) {
        return createCriteria().call(callable)
    }

    /**
     * Creates a criteria builder instance
     */
    def withCriteria(Map builderArgs, Closure callable) {
        def criteriaBuilder = createCriteria()
        def builderBean = PropertyAccessorFactory.forBeanPropertyAccess(criteriaBuilder)
        for (entry in builderArgs) {
            if (builderBean.isWritableProperty(entry.key)) {
                builderBean.setPropertyValue(entry.key, entry.value)
            }
        }

        return criteriaBuilder.list(callable)
    }

    /**
     * Locks an instance for an update
     * @param id The identifier
     * @return The instance
     */
    D lock(Serializable id) {
        execute ({ Session session ->
                session.lock(persistentClass, id)
        } as SessionCallback)
    }

    /**
     * Merges an instance with the current session
     * @param d The object to merge
     * @return The instance
     */
    D merge(D d) {
        execute ({ Session session ->
            session.persist(d)
            return d
        } as SessionCallback)
    }

    /**
     * Counts the number of persisted entities
     * @return The number of persisted entities
     */
    Integer count() {
        execute ({ Session session ->

            def q = session.createQuery(persistentClass)
            q.projections().count()
            def result = q.singleResult()
            if (!(result instanceof Number)) {
                result = result.toString()
            }
            try {
                return result as Integer
            }
            catch (NumberFormatException e) {
                return 0
            }
        } as SessionCallback)
    }

    /**
     * Same as {@link #count()} but allows property-style syntax (Foo.count)
     */
    Integer getCount() {
        count()
    }

    /**
     * Checks whether an entity exists
     */
    boolean exists(Serializable id) {
        get(id) != null
    }

    /**
     * Lists objects in the datastore. eg. Book.list(max:10)
     *
     * @param params Any parameters such as offset, max etc.
     * @return A list of results
     */
    List<D> list(Map params) {
        execute ({ Session session ->
            Query q = session.createQuery(persistentClass)
            DynamicFinder.populateArgumentsForCriteria(persistentClass, q, params)
            q.list()
        } as SessionCallback)
    }

    /**
     * List all entities
     *
     * @return The list of all entities
     */
    List<D> list() {
        execute ({ Session session ->
                session.createQuery(persistentClass).list()
        } as SessionCallback)
    }

    /**
     * The same as {@link #list()}
     *
     * @return The list of all entities
     */
    List<D> findAll() {
        list()
    }

    /**
     * Finds an object by example
     *
     * @param example The example
     * @return A list of matching results
     */
    List<D> findAll(D example) {
        findAll(example, Collections.emptyMap())
    }

    /**
     * Finds an object by example using the given arguments for pagination
     *
     * @param example The example
     * @param args The arguments
     *
     * @return A list of matching results
     */
    List<D> findAll(D example, Map args) {
        if (!persistentEntity.isInstance(example)) {
            return Collections.emptyList()
        }

        def queryMap = createQueryMapForExample(persistentEntity, example)
        return findAllWhere(queryMap, args)
    }

    private Map createQueryMapForExample(PersistentEntity persistentEntity, D example) {
        def props = persistentEntity.persistentProperties.findAll { PersistentProperty prop ->
            !(prop instanceof Association)
        }

        def queryMap = [:]
        for (PersistentProperty prop in props) {
            def val = example[prop.name]
            if (val != null) {
                queryMap[prop.name] = val
            }
        }
        return queryMap
    }

    /**
     * Finds all results matching all of the given conditions. Eg. Book.findAllWhere(author:"Stephen King", title:"The Stand")
     *
     * @param queryMap The map of conditions
     * @return A list of results
     */
    List<D> findAllWhere(Map queryMap) {
        findAllWhere(queryMap, Collections.emptyMap())
    }

    /**
     * Finds all results matching all of the given conditions. Eg. Book.findAllWhere(author:"Stephen King", title:"The Stand")
     *
     * @param queryMap The map of conditions
     * @param args The Query arguments
     *
     * @return A list of results
     */
    List<D> findAllWhere(Map queryMap, Map args) {
        execute ({
            Query q = session.createQuery(persistentClass)
            q.allEq(queryMap)
            DynamicFinder.populateArgumentsForCriteria persistentClass, q, args
            q.list()
        } as SessionCallback<List>)
    }

    /**
     * Finds an object by example
     *
     * @param example The example
     * @return A list of matching results
     */
    D find(D example) {
        find(example, Collections.emptyMap())
    }

    /**
     * Finds an object by example using the given arguments for pagination
     *
     * @param example The example
     * @param args The arguments
     *
     * @return A list of matching results
     */
    D find(D example, Map args) {
        if (persistentEntity.isInstance(example)) {
            def queryMap = createQueryMapForExample(persistentEntity, example)
            return findWhere(queryMap, args)
        }
        return null
    }

    /**
     * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand")
     *
     * @param queryMap The map of conditions
     * @return A single result
     */
    D findWhere(Map queryMap) {
        findWhere(queryMap, Collections.emptyMap())
    }

    /**
     * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand")
     *
     * @param queryMap The map of conditions
     * @param args The Query arguments
     *
     * @return A single result
     */
    D findWhere(Map queryMap, Map args) {
        execute({
            Query q = session.createQuery(persistentClass)
            if (queryMap) {
                q.allEq(queryMap)
            }
            DynamicFinder.populateArgumentsForCriteria persistentClass, q, args
            q.singleResult()
        } as SessionCallback)
    }

   /**
    * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand").  If
    * a matching persistent entity is not found a new entity is created and returned.
    *
    * @param queryMap The map of conditions
    * @return A single result
     */
    D findOrCreateWhere(Map queryMap) {
        internalFindOrCreate(queryMap, false)
    }

   /**
    * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand").  If
    * a matching persistent entity is not found a new entity is created, saved and returned.
    *
    * @param queryMap The map of conditions
    * @return A single result
     */
    D findOrSaveWhere(Map queryMap) {
        internalFindOrCreate(queryMap, true)
    }

    private D internalFindOrCreate(Map queryMap, boolean shouldSave) {
        D result = findWhere(queryMap)
        if (!result) {
            def persistentMetaClass = GroovySystem.metaClassRegistry.getMetaClass(persistentClass)
            result = persistentMetaClass.invokeConstructor(queryMap)
            if (shouldSave) {
                result.save()
            }
        }
        result
    }

    /**
     * Execute a closure whose first argument is a reference to the current session.
     *
     * @param callable the closure
     * @return The result of the closure
     */
    def withSession(Closure callable) {
        execute ({
            callable.call session
        } as SessionCallback)
    }

    /**
     * Same as withSession, but present for the case where withSession is overridden to use the Hibernate session
     *
     * @param callable the closure
     * @return The result of the closure
     */
    def withDatastoreSession(Closure callable) {
        execute ({ Session session ->
            callable.call session
        } as SessionCallback)
    }

    /**
     * Executes the closure within the context of a transaction, creating one if none is present or joining
     * an existing transaction if one is already present.
     *
     * @param callable The closure to call
     * @return The result of the closure execution
     */
    def withTransaction(Closure callable) {
        Assert.notNull transactionManager, "No transactionManager bean configured"

        if (!callable) {
            return
        }

        new TransactionTemplate(transactionManager).execute(callable as TransactionCallback)
    }

    /**
     * Executes the closure within the context of a new transaction
     *
     * @param callable The closure to call
     * @return The result of the closure execution
     */
    def withNewTransaction(Closure callable) {
        Assert.notNull transactionManager, "No transactionManager bean configured"

        if (!callable) {
            return
        }

        def transactionTemplate = new TransactionTemplate(transactionManager,
                new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW))
        transactionTemplate.execute(callable as TransactionCallback)
    }

    /**
     * Executes the closure within the context of a transaction for the given {@link TransactionDefinition}
     *
     * @param callable The closure to call
     * @return The result of the closure execution
     */
    def withTransaction(TransactionDefinition definition, Closure callable) {
        Assert.notNull transactionManager, "No transactionManager bean configured"

        if (!callable) {
            return
        }

        new TransactionTemplate(transactionManager, definition).execute(callable as TransactionCallback)
    }

    /**
     * Creates and binds a new session for the scope of the given closure
     */
    void withNewSession(Closure callable) {
        def session = datastore.connect()
        try {
            DatastoreUtils.bindNewSession session
            callable?.call(session)
        }
        finally {
            DatastoreUtils.unbindSession session
        }
    }

    /**
     * Get the thread-local map used to store Errors when validating.
     * @return the map
     */
    Map<D, Errors> getValidationErrorsMap() {
        AbstractDatastore.getValidationErrorsMap()
    }

    /**
     * Get the thread-local map used to store whether to skip validation.
     * @return the map
     */
    Map<D, Boolean> getValidationSkipMap() {
        AbstractDatastore.getValidationErrorsMap()
    }

    // TODO: In the first version no support will exist for String-based queries
    List<D> executeQuery(String query) {
        unsupported("executeQuery")
    }

    List<D> executeQuery(String query, Map args) {
        unsupported("executeQuery")
    }

    List<D> executeQuery(String query, Map params, Map args) {
        unsupported("executeQuery")
    }

    List<D> executeQuery(String query, Collection params) {
        unsupported("executeQuery")
    }

    List<D> executeQuery(String query, Collection params, Map args) {
        unsupported("executeQuery")
    }

    Integer executeUpdate(String query) {
        unsupported("executeUpdate")
    }

    Integer executeUpdate(String query, Map args) {
        unsupported("executeUpdate")
    }

    Integer executeUpdate(String query, Map params, Map args) {
        unsupported("executeUpdate")
    }

    Integer executeUpdate(String query, Collection params) {
        unsupported("executeUpdate")
    }

    Integer executeUpdate(String query, Collection params, Map args) {
        unsupported("executeUpdate")
    }

    D find(String query) {
        unsupported("find")
    }

    D find(String query, Map args) {
        unsupported("find")
    }

    D find(String query, Map params, Map args) {
        unsupported("find")
    }

    D find(String query, Collection params) {
        unsupported("find")
    }

    D find(String query, Collection params, Map args) {
        unsupported("find")
    }

    List<D> findAll(String query) {
        unsupported("findAll")
    }

    List<D> findAll(String query, Map args) {
        unsupported("findAll")
    }

    List<D> findAll(String query, Map params, Map args) {
        unsupported("findAll")
    }

    List<D> findAll(String query, Collection params) {
        unsupported("find")
    }

    List<D> findAll(String query, Collection params, Map args) {
        unsupported("findAll")
    }

    void unsupported(method) {
        throw new UnsupportedOperationException("String-based queries like [$method] are currently not supported in this implementation of GORM. Use criteria instead.")
    }
}
