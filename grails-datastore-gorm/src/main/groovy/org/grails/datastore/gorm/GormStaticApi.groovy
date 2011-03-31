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
import org.springframework.beans.BeanWrapperImpl
import org.springframework.datastore.mapping.core.Datastore
import org.springframework.datastore.mapping.core.Session
import org.springframework.datastore.mapping.model.PersistentProperty
import org.springframework.datastore.mapping.model.types.Association
import org.springframework.datastore.mapping.query.Query
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.DefaultTransactionDefinition
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionTemplate

/**
 *  Static methods of the GORM API
 *
 * @author Graeme Rocher
 */
class GormStaticApi extends AbstractGormApi {

    private List<FinderMethod> dynamicFinders
    private PlatformTransactionManager transactionManager

    GormStaticApi(Class persistentClass, Datastore datastore) {
        this(persistentClass,datastore, DynamicFinder.getAllDynamicFinders(datastore))
    }

    GormStaticApi(Class persistentClass, Datastore datastore, List<FinderMethod> finders) {
        this(persistentClass,datastore,finders,null)
    }

    GormStaticApi(Class persistentClass, Datastore datastore, List<FinderMethod> finders, PlatformTransactionManager transactionManager) {
        super(persistentClass,datastore)
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
     * Method missing handler that deals with the invocation of dynamic finders
     *
     * @param methodName The method name
     * @param args The arguments
     * @return The result of the method call
     */
    def methodMissing(String methodName, args) {
        def method = dynamicFinders.find { FinderMethod f -> f.isMethodMatch(methodName) }
        def cls = persistentClass
        def mc = cls.metaClass
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
            throw new MissingMethodException(methodName, cls, args)
        }
    }

    /**
     * Saves a list of objects in one go
     * @param objectsToSave The objects to save
     * @return A list of object identifiers
     */
    List saveAll(Object...objectsToSave) {
        Session currentSession = datastore.currentSession

        currentSession.persist Arrays.asList(objectsToSave)
    }

    /**
     * Deletes a list of objects in one go
     * @param objectsToDelete The objects to delete
     */
    void deleteAll(Object...objectsToDelete) {
        Session currentSession = datastore.currentSession

        currentSession.delete objectsToDelete
    }

    /**
     * Creates an instance of this class
     * @return The created instance
     */
    def create() {
        persistentClass.newInstance()
    }

    /**
     * Retrieves and object from the datastore. eg. Book.get(1)
     */
    def get(Serializable id) {
        datastore.currentSession.retrieve(persistentClass,id)
    }

    /**
     * Retrieves and object from the datastore. eg. Book.read(1)
     *
     * Since the datastore abstraction doesn't support dirty checking yet this
     * just delegates to {@link #get(Serializable)}
     */
    def read(Serializable id) {
        datastore.currentSession.retrieve(persistentClass,id)
    }

    /**
     * Retrieves and object from the datastore as a proxy. eg. Book.load(1)
     */
    def load(Serializable id) {
        datastore.currentSession.proxy(persistentClass,id)
    }

    /**
     * Retrieves and object from the datastore as a proxy. eg. Book.proxy(1)
     */
    def proxy(Serializable id) {
        load(id)
    }

    /**
     * Retrieve all the objects for the given identifiers
     * @param ids The identifiers to operate against
     * @return A list of identifiers
     */
    List getAll(Serializable... ids) {
        datastore.currentSession.retrieveAll(persistentClass, ids.flatten())
    }

    /**
     * @return Synonym for {@link #list()}
     */
    List getAll() {
        list()
    }

    /**
     * Creates a criteria builder instance
     */
    def createCriteria() {
        return new CriteriaBuilder(persistentClass, datastore)
    }

    /**
     * Creates a criteria builder instance
     */
    def withCriteria(Closure callable) {
        return createCriteria().list(callable)
    }

    /**
     * Creates a criteria builder instance
     */
    def withCriteria( Map builderArgs, Closure callable) {
        def criteriaBuilder = createCriteria()
        def builderBean = new BeanWrapperImpl(criteriaBuilder)
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
    def lock(Serializable id) {
        datastore.currentSession.lock(persistentClass, id)
    }

    /**
     * Merges an instance with the current session
     * @param o The object to merge
     * @return The instance
     */
    def merge(Object o) {
        datastore.currentSession.persist(o)
        return o
    }

    /**
     * Counts the number of persisted entities
     * @return The number of persisted entities
     */
    Integer count() {
        def q = datastore.currentSession.createQuery(persistentClass)
        q.projections().count()
        def result = q.singleResult()
        if (!(result instanceof Number)) result = result.toString()
        try {
            result as Integer
        } catch (NumberFormatException e) {
            return 0
        }
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
    List list(Map params) {
        Query q = datastore.currentSession.createQuery(persistentClass)
        DynamicFinder.populateArgumentsForCriteria(persistentClass, q, params)
        q.list()
    }

    /**
     * List all entities
     *
     * @return The list of all entities
     */
    List list() {
        datastore
            .currentSession
            .createQuery(persistentClass)
            .list()
    }

    /**
     * The same as {@link #list()}
     *
     * @return The list of all entities
     */
    List findAll() {
        list()
    }

    /**
     * Finds an object by example
     *
     * @param example The example
     * @return A list of matching results
     */
    List findAll(Object example) {
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
    List findAll(Object example, Map args) {
        if (persistentEntity.isInstance(example)) {
            def queryMap = createQueryMapForExample(persistentEntity, example)
            return findAllWhere(queryMap, args)
        }
        return Collections.emptyList()
    }

    private Map createQueryMapForExample(org.springframework.datastore.mapping.model.PersistentEntity persistentEntity, example) {
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
    List findAllWhere(Map queryMap) {
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
    List findAllWhere(Map queryMap, Map args) {
        Query q = datastore.currentSession.createQuery(persistentClass)
        q.allEq(queryMap)
        DynamicFinder.populateArgumentsForCriteria persistentClass, q, args
        q.list()
    }

    /**
     * Finds an object by example
     *
     * @param example The example
     * @return A list of matching results
     */
    def find(Object example) {
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
    def find(Object example, Map args) {
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
    def findWhere(Map queryMap) {
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
    def findWhere(Map queryMap, Map args) {
        Query q = datastore.currentSession.createQuery(persistentClass)
        if (queryMap) {
            q.allEq(queryMap)
        }
        DynamicFinder.populateArgumentsForCriteria persistentClass, q, args
        q.singleResult()
    }

   /**
    * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand").  If
    * a matching persistent entity is not found a new entity is created and returned.
    *
    * @param queryMap The map of conditions
    * @return A single result
     */
    def findOrCreateWhere(Map queryMap) {
        def result = persistentClass.newInstance()
        result.properties = queryMap
        result
    }

    /**
     * Execute a closure whose first argument is a reference to the current session
     * @param callable
     *
     * @return The result of the closure
     */
    def withSession(Closure callable) {
        callable.call(datastore.currentSession)
    }

    /**
     * Executes the closure within the context of a transaction, creating one if non is present or joining
     * an existing transaction if one is already present
     *
     * @param callable The closure to call
     * @return The result of the closure execution
     */
    def withTransaction(Closure callable) {
        if(transactionManager == null) {
            throw new IllegalStateException("No transactionManager bean configured")
        }

        if (callable) {
            def transactionTemplate = new TransactionTemplate(transactionManager)
            transactionTemplate.execute(callable as TransactionCallback)
        }
    }

    /**
     * Executes the closure within the context of a new transaction
     *
     * @param callable The closure to call
     * @return The result of the closure execution
     */
    def withNewTransaction(Closure callable) {
        if(transactionManager == null) {
            throw new IllegalStateException("No transactionManager bean configured")
        }
        if (callable) {
            def transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW))
            transactionTemplate.execute(callable as TransactionCallback)
        }
    }

    /**
     * Executes the closure within the context of a transaction for the given {@link TransactionDefinition}
     *
     * @param callable The closure to call
     * @return The result of the closure execution
     */
    def withTransaction(TransactionDefinition definition, Closure callable) {
       if(transactionManager == null) {
            throw new IllegalStateException("No transactionManager bean configured")
       }

       if (callable) {
            def transactionTemplate = new TransactionTemplate(transactionManager, definition)
            transactionTemplate.execute(callable as TransactionCallback)
       }
    }

    /**
     * Creates and binds a new session for the scope of the given closure
     */
    def withNewSession(Closure callable) {

        def session = datastore.connect()
        try {
            callable?.call(session)
        }
        finally {
            session.disconnect()
        }
    }

    // TODO: In the first version no support will exist for String-based queries
    def executeQuery(String query) {
        unsupported("executeQuery")
    }

    def executeQuery(String query, Map args) {
        unsupported("executeQuery")
    }

    def executeQuery(String query, Map params, Map args) {
        unsupported("executeQuery")
    }

    def executeQuery(String query, Collection params) {
        unsupported("executeQuery")
    }

    def executeQuery(String query, Collection params, Map args) {
        unsupported("executeQuery")
    }

    def executeUpdate(String query) {
        unsupported("executeUpdate")
    }

    def executeUpdate(String query, Map args) {
        unsupported("executeUpdate")
    }

    def executeUpdate(String query, Map params, Map args) {
        unsupported("executeUpdate")
    }

    def executeUpdate(String query, Collection params) {
        unsupported("executeUpdate")
    }

    def executeUpdate(String query, Collection params, Map args) {
        unsupported("executeUpdate")
    }

    def find(String query) {
        unsupported("find")
    }

    def find(String query, Map args) {
        unsupported("find")
    }

    def find(String query, Map params, Map args) {
        unsupported("find")
    }

    def find(String query, Collection params) {
        unsupported("find")
    }

    def find(String query, Collection params, Map args) {
        unsupported("find")
    }

    List findAll(String query) {
        unsupported("findAll")
    }

    List findAll(String query, Map args) {
        unsupported("findAll")
    }

    List findAll(String query, Map params, Map args) {
        unsupported("findAll")
    }

    List findAll(String query, Collection params) {
        unsupported("find")
    }

    List findAll(String query, Collection params, Map args) {
        unsupported("findAll")
    }

    def unsupported(method) {
        throw new UnsupportedOperationException("String-based queries like [$method] are currently not supported in this implementation of GORM. Use criteria instead.")
    }
}
