/*
 * Copyright 2014 the original author or authors.
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

import grails.gorm.DetachedCriteria
import org.grails.datastore.gorm.async.GormAsyncStaticApi
import org.grails.datastore.mapping.query.api.BuildableCriteria
import org.grails.datastore.mapping.query.api.Criteria
import org.springframework.transaction.TransactionDefinition
import org.springframework.validation.Errors

/**
 * 
 * @author Jeff Brown
 * @since 4.0
 */
trait GormEntity<D> {
    
    private static GormInstanceApi internalInstanceApi
    private static GormStaticApi<D> internalStaticApi

    static void initInternalApi(GormInstanceApi api) {
        internalInstanceApi = api
    }

    static GormInstanceApi currentGormInstanceApi() {
        if(internalInstanceApi == null) {
            throw new IllegalStateException("Method on class [${this.getName()}] was used outside of a Grails application. If running in the context of a test using the mocking API or bootstrap Grails correctly.")
        }
        internalInstanceApi
    }

    static void initInternalStaticApi(GormStaticApi<D> api) {
        internalStaticApi = api
    }

    static GormStaticApi<D> currentGormStaticApi() {
        if(internalStaticApi == null) {
            throw new IllegalStateException("Method on class [${this.getName()}] was used outside of a Grails application. If running in the context of a test using the mocking API or bootstrap Grails correctly.")
        }
        internalStaticApi
    }

    /**
     * Proxy aware instanceOf implementation.
     */
    boolean instanceOf(Class cls) {
        currentGormInstanceApi().instanceOf this, cls
    }

    /**
     * Upgrades an existing persistence instance to a write lock
     * @return The instance
     */
    D lock() {
        currentGormInstanceApi().lock this
    }

    /**
     * Locks the instance for updates for the scope of the passed closure
     *
     * @param callable The closure
     * @return The result of the closure
     */
    def mutex(Closure callable) {
        currentGormInstanceApi().mutex this, callable
    }

    /**
     * Refreshes the state of the current instance
     * @return The instance
     */
    D refresh() {
        currentGormInstanceApi().refresh this
    }

    /**
     * Saves an object the datastore
     * @return Returns the instance
     */
    D save() {
        currentGormInstanceApi().save this
    }

    /**
     * Forces an insert of an object to the datastore
     * @return Returns the instance
     */
    D insert() {
        currentGormInstanceApi().insert this
    }

    /**
     * Forces an insert of an object to the datastore
     * @return Returns the instance
     */
    D insert(Map params) {
        currentGormInstanceApi().insert this, params
    }

    /**
     * Saves an object the datastore
     * @return Returns the instance
     */
    D merge() {
        currentGormInstanceApi().merge this
    }

    /**
     * Saves an object the datastore
     * @return Returns the instance
     */
    D merge(Map params) {
        currentGormInstanceApi().merge this, params
    }

    /**
     * Save method that takes a boolean which indicates whether to perform validation or not
     *
     * @param validate Whether to perform validation
     *
     * @return The instance or null if validation fails
     */
    D save(boolean validate) {
        currentGormInstanceApi().save this, validate
    }

    /**
     * Saves an object with the given parameters
     * @param instance The instance
     * @param params The parameters
     * @return The instance
     */
    D save(Map params) {
        currentGormInstanceApi().save this, params
    }

    /**
     * Returns the objects identifier
     */
    Serializable ident() {
        currentGormInstanceApi().ident this
    }

    /**
     * Attaches an instance to an existing session. Requries a session-based model
     * @return
     */
    D attach() {
        currentGormInstanceApi().attach this
    }

    /**
     * No concept of session-based model so defaults to true
     */
    boolean isAttached() {
        currentGormInstanceApi().isAttached this
    }

    /**
     * Discards any pending changes. Requires a session-based model.
     */
    void discard() {
        currentGormInstanceApi().discard this
    }

    /**
     * Deletes an instance from the datastore
     */
    void delete() {
        currentGormInstanceApi().delete this
    }

    /**
     * Deletes an instance from the datastore
     */
    void delete(Map params) {
        currentGormInstanceApi().delete this, params
    }

    /**
     * Checks whether a field is dirty
     *
     * @param instance The instance
     * @param fieldName The name of the field
     *
     * @return true if the field is dirty
     */
    boolean isDirty(String fieldName) {
        currentGormInstanceApi().isDirty this, fieldName
    }

    /**
     * Checks whether an entity is dirty
     *
     * @param instance The instance
     * @return true if it is dirty
     */
    boolean isDirty() {
        currentGormInstanceApi().isDirty this
    }

    /**
     * Obtains a list of property names that are dirty
     *
     * @param instance The instance
     * @return A list of property names that are dirty
     */
    List getDirtyPropertyNames() {
        currentGormInstanceApi().getDirtyPropertyNames this
    }

    /**
     * Gets the original persisted value of a field.
     *
     * @param fieldName The field name
     * @return The original persisted value
     */
    Object getPersistentValue(String fieldName) {
        currentGormInstanceApi().getPersistentValue this, fieldName
    }

    /**
     *
     * @param callable Callable closure containing detached criteria definition
     * @return The DetachedCriteria instance
     */
    static DetachedCriteria<D> where(Closure callable) {
        currentGormStaticApi().where callable
    }

    /**
     *
     * @param callable Callable closure containing detached criteria definition
     * @return The DetachedCriteria instance that is lazily initialized
     */
    static DetachedCriteria<D> whereLazy(Closure callable) {
        currentGormStaticApi().whereLazy callable
    }

    /**
     *
     * @param callable Callable closure containing detached criteria definition
     * @return The DetachedCriteria instance
     */
    static DetachedCriteria<D> whereAny(Closure callable) {
        currentGormStaticApi().whereAny callable
    }

    /**
     * Uses detached criteria to build a query and then execute it returning a list
     *
     * @param callable The callable
     * @return A List of entities
     */
    static List<D> findAll(Closure callable) {
        currentGormStaticApi().findAll callable
    }

    /**
     * Uses detached criteria to build a query and then execute it returning a list
     *
     * @param args pagination parameters
     * @param callable The callable
     * @return A List of entities
     */
    static List<D> findAll(Map args, Closure callable) {
        currentGormStaticApi().findAll args, callable
    }

    /**
     * Uses detached criteria to build a query and then execute it returning a list
     *
     * @param callable The callable
     * @return A single entity
     */
    static D find(Closure callable) {
        currentGormStaticApi().find callable
    }


    /**
     * Saves a list of objects in one go
     * @param objectsToSave The objects to save
     * @return A list of object identifiers
     */
    static List<Serializable> saveAll(Object... objectsToSave) {
        currentGormStaticApi().saveAll objectsToSave
    }

    /**
     * Saves a list of objects in one go
     * @param objectToSave Collection of objects to save
     * @return A list of object identifiers
     */
    static List<Serializable> saveAll(Iterable<?> objectsToSave) {
        currentGormStaticApi().saveAll objectsToSave
    }

    /**
     * Deletes a list of objects in one go
     * @param objectsToDelete The objects to delete
     */
    static void deleteAll(Object... objectsToDelete) {
        currentGormStaticApi().deleteAll objectsToDelete
    }

    /**
     * Deletes a list of objects in one go
     * @param objectsToDelete Collection of objects to delete
     */
    static void deleteAll(Iterable objectToDelete) {
        currentGormStaticApi().deleteAll objectToDelete
    }

    /**
     * Creates an instance of this class
     * @return The created instance
     */
    static D create() {
        currentGormStaticApi().create()
    }

    /**
     * Retrieves and object from the datastore. eg. Book.get(1)
     */
    static D get(Serializable id) {
        currentGormStaticApi().get id
    }

    /**
     * Retrieves and object from the datastore. eg. Book.read(1)
     *
     * Since the datastore abstraction doesn't support dirty checking yet this
     * just delegates to {@link #get(Serializable)}
     */
    static D read(Serializable id) {
        currentGormStaticApi().read id
    }

    /**
     * Retrieves and object from the datastore as a proxy. eg. Book.load(1)
     */
    static D load(Serializable id) {
        currentGormStaticApi().load id
    }

    /**
     * Retrieves and object from the datastore as a proxy. eg. Book.proxy(1)
     */
    static D proxy(Serializable id) {
        currentGormStaticApi().proxy id
    }

    /**
     * Retrieve all the objects for the given identifiers
     * @param ids The identifiers to operate against
     * @return A list of identifiers
     */
    static List<D> getAll(Iterable<Serializable> ids) {
        currentGormStaticApi().getAll ids
    }

    /**
     * Retrieve all the objects for the given identifiers
     * @param ids The identifiers to operate against
     * @return A list of identifiers
     */
    static List<D> getAll(Serializable... ids) {
        currentGormStaticApi().getAll ids
    }

    /**
     * @return The async version of the GORM static API
     */
    static GormAsyncStaticApi<D> getAsync() {
        currentGormStaticApi().getAsync()
    }

    /**
     * @return Synonym for {@link #list()}
     */
    static List<D> getAll() {
        list()
    }

    /**
     * Creates a criteria builder instance
     */
    static BuildableCriteria createCriteria() {
        currentGormStaticApi().createCriteria()
    }

    /**
     * Creates a criteria builder instance
     */
    static withCriteria(@DelegatesTo(Criteria) Closure callable) {
        currentGormStaticApi().withCriteria callable
    }

    /**
     * Creates a criteria builder instance
     */
    static withCriteria(Map builderArgs, @DelegatesTo(Criteria) Closure callable) {
        currentGormStaticApi().withCriteria builderArgs, callable
    }

    /**
     * Locks an instance for an update
     * @param id The identifier
     * @return The instance
     */
    static D lock(Serializable id) {
        currentGormStaticApi().lock id
    }

    /**
     * Merges an instance with the current session
     * @param d The object to merge
     * @return The instance
     */
    static D merge(D d) {
        currentGormStaticApi().merge d
    }

    /**
     * Counts the number of persisted entities
     * @return The number of persisted entities
     */
    static Integer count() {
        currentGormStaticApi().count()
    }

    /**
     * Same as {@link #count()} but allows property-style syntax (Foo.count)
     */
    static Integer getCount() {
        currentGormStaticApi().getCount()
    }

    /**
     * Checks whether an entity exists
     */
    static boolean exists(Serializable id) {
        currentGormStaticApi().exists id
    }

    /**
     * Lists objects in the datastore. eg. Book.list(max:10)
     *
     * @param params Any parameters such as offset, max etc.
     * @return A list of results
     */
    static List<D> list(Map params) {
        currentGormStaticApi().list params
    }

    /**
     * List all entities
     *
     * @return The list of all entities
     */
    static List<D> list() {
        currentGormStaticApi().list()
    }

    /**
     * The same as {@link #list()}
     *
     * @return The list of all entities
     */
    static List<D> findAll(Map params = Collections.emptyMap()) {
        currentGormStaticApi().findAll params
    }

    /**
     * Finds an object by example
     *
     * @param example The example
     * @return A list of matching results
     */
    static List<D> findAll(D example) {
        currentGormStaticApi().findAll example
    }

    /**
     * Finds an object by example using the given arguments for pagination
     *
     * @param example The example
     * @param args The arguments
     *
     * @return A list of matching results
     */
    static List<D> findAll(D example, Map args) {
        currentGormStaticApi().findAll example, args
    }

    /**
     * Finds the first object using the natural sort order
     *
     * @return the first object in the datastore, null if none exist
     */
    static D first() {
        currentGormStaticApi().first()
    }

    /**
     * Finds the first object sorted by propertyName
     *
     * @param propertyName the name of the property to sort by
     *
     * @return the first object in the datastore sorted by propertyName, null if none exist
     */
    static D first(String propertyName) {
        currentGormStaticApi().first propertyName
    }

    /**
     * Finds the first object.  If queryParams includes 'sort', that will
     * dictate the sort order, otherwise natural sort order will be used.
     * queryParams may include any of the same parameters that might be passed
     * to the list(Map) method.  This method will ignore 'order' and 'max' as
     * those are always 'asc' and 1, respectively.
     *
     * @return the first object in the datastore, null if none exist
     */
    static D first(Map queryParams) {
        currentGormStaticApi().first queryParams
    }

    /**
     * Finds the last object using the natural sort order
     *
     * @return the last object in the datastore, null if none exist
     */
    static D last() {
        currentGormStaticApi().last()
    }

    /**
     * Finds the last object sorted by propertyName
     *
     * @param propertyName the name of the property to sort by
     *
     * @return the last object in the datastore sorted by propertyName, null if none exist
     */
    static D last(String propertyName) {
        currentGormStaticApi().last propertyName
    }

    /**
     * Finds the last object.  If queryParams includes 'sort', that will
     * dictate the sort order, otherwise natural sort order will be used.
     * queryParams may include any of the same parameters that might be passed
     * to the list(Map) method.  This method will ignore 'order' and 'max' as
     * those are always 'asc' and 1, respectively.
     *
     * @return the last object in the datastore, null if none exist
     */
    static D last(Map queryParams) {
        currentGormStaticApi().last queryParams
    }

    /**
     * Finds all results matching all of the given conditions. Eg. Book.findAllWhere(author:"Stephen King", title:"The Stand")
     *
     * @param queryMap The map of conditions
     * @return A list of results
     */
    static List<D> findAllWhere(Map queryMap) {
        currentGormStaticApi().findAllWhere queryMap
    }

    /**
     * Finds all results matching all of the given conditions. Eg. Book.findAllWhere(author:"Stephen King", title:"The Stand")
     *
     * @param queryMap The map of conditions
     * @param args The Query arguments
     *
     * @return A list of results
     */
    static List<D> findAllWhere(Map queryMap, Map args) {
        currentGormStaticApi().findAllWhere queryMap, args
    }

    /**
     * Finds an object by example
     *
     * @param example The example
     * @return A list of matching results
     */
    static D find(D example) {
        currentGormStaticApi().find example
    }

    /**
     * Finds an object by example using the given arguments for pagination
     *
     * @param example The example
     * @param args The arguments
     *
     * @return A list of matching results
     */
    static D find(D example, Map args) {
        currentGormStaticApi().find example, args
    }

    /**
     * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand")
     *
     * @param queryMap The map of conditions
     * @return A single result
     */
    static D findWhere(Map queryMap) {
        currentGormStaticApi().findWhere queryMap
    }

    /**
     * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand")
     *
     * @param queryMap The map of conditions
     * @param args The Query arguments
     *
     * @return A single result
     */
    static D findWhere(Map queryMap, Map args) {
        currentGormStaticApi().findWhere queryMap, args
    }

    /**
     * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand").  If
     * a matching persistent entity is not found a new entity is created and returned.
     *
     * @param queryMap The map of conditions
     * @return A single result
     */
    static D findOrCreateWhere(Map queryMap) {
        currentGormStaticApi().findOrCreateWhere queryMap
    }

    /**
     * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand").  If
     * a matching persistent entity is not found a new entity is created, saved and returned.
     *
     * @param queryMap The map of conditions
     * @return A single result
     */
    static D findOrSaveWhere(Map queryMap) {
        currentGormStaticApi().findOrSaveWhere queryMap
    }

    /**
     * Execute a closure whose first argument is a reference to the current session.
     *
     * @param callable the closure
     * @return The result of the closure
     */
    static withSession(Closure callable) {
        currentGormStaticApi().withSession callable
    }

    /**
     * Same as withSession, but present for the case where withSession is overridden to use the Hibernate session
     *
     * @param callable the closure
     * @return The result of the closure
     */
    static withDatastoreSession(Closure callable) {
        currentGormStaticApi().withDatastoreSession callable
    }

    /**
     * Executes the closure within the context of a transaction, creating one if none is present or joining
     * an existing transaction if one is already present.
     *
     * @param callable The closure to call
     * @return The result of the closure execution
     * @see #withTransaction(Map, Closure)
     * @see #withNewTransaction(Closure)
     * @see #withNewTransaction(Map, Closure)
     */
    static withTransaction(Closure callable) {
        currentGormStaticApi().withTransaction callable
    }

    /**
     * Executes the closure within the context of a new transaction
     *
     * @param callable The closure to call
     * @return The result of the closure execution
     * @see #withTransaction(Closure)
     * @see #withTransaction(Map, Closure)
     * @see #withNewTransaction(Map, Closure)
     */
    static withNewTransaction(Closure callable) {
        currentGormStaticApi().withNewTransaction callable
    }

    /**
     * Executes the closure within the context of a transaction which is
     * configured with the properties contained in transactionProperties.
     * transactionProperties may contain any properties supported by
     * {@link org.springframework.transaction.support.DefaultTransactionDefinition}.
     *
     * <blockquote>
     * <pre>
     * SomeEntity.withTransaction([propagationBehavior: TransactionDefinition.PROPAGATION_REQUIRES_NEW,
     *                             isolationLevel: TransactionDefinition.ISOLATION_REPEATABLE_READ]) {
     *     // ...
     * }
     * </pre>
     * </blockquote>
     *
     * @param transactionProperties properties to configure the transaction properties
     * @param callable The closure to call
     * @return The result of the closure execution
     * @see org.springframework.transaction.support.DefaultTransactionDefinition
     * @see #withNewTransaction(Closure)
     * @see #withNewTransaction(Map, Closure)
     * @see #withTransaction(Closure)
     */
    static withTransaction(Map transactionProperties, Closure callable) {
        currentGormStaticApi().withTransaction transactionProperties, callable
    }

    /**
     * Executes the closure within the context of a new transaction which is
     * configured with the properties contained in transactionProperties.
     * transactionProperties may contain any properties supported by
     * {@link org.springframework.transaction.support.DefaultTransactionDefinition}.  Note that if transactionProperties
     * includes entries for propagationBehavior or propagationName, those values
     * will be ignored.  This method always sets the propagation level to
     * TransactionDefinition.REQUIRES_NEW.
     *
     * <blockquote>
     * <pre>
     * SomeEntity.withNewTransaction([isolationLevel: TransactionDefinition.ISOLATION_REPEATABLE_READ]) {
     *     // ...
     * }
     * </pre>
     * </blockquote>
     *
     * @param transactionProperties properties to configure the transaction properties
     * @param callable The closure to call
     * @return The result of the closure execution
     * @see org.springframework.transaction.support.DefaultTransactionDefinition
     * @see #withNewTransaction(Closure)
     * @see #withTransaction(Closure)
     * @see #withTransaction(Map, Closure)
     */
    static withNewTransaction(Map transactionProperties, Closure callable) {
        currentGormStaticApi().withNewTransaction transactionProperties, callable
    }

    /**
     * Executes the closure within the context of a transaction for the given {@link org.springframework.transaction.TransactionDefinition}
     *
     * @param callable The closure to call
     * @return The result of the closure execution
     */
    static withTransaction(TransactionDefinition definition, Closure callable) {
        currentGormStaticApi().withTransaction definition, callable
    }

    /**
     * Creates and binds a new session for the scope of the given closure
     */
    static withNewSession(Closure callable) {
        currentGormStaticApi().withNewSession callable
    }

    /**
     * Creates and binds a new session for the scope of the given closure
     */
    static withStatelessSession(Closure callable) {
        currentGormStaticApi().withStatelessSession callable
    }

    /**
     * Get the thread-local map used to store Errors when validating.
     * @return the map
     */
    static Map<D, Errors> getValidationErrorsMap() {
        currentGormStaticApi().getValidationErrorsMap()
    }

    /**
     * Get the thread-local map used to store whether to skip validation.
     * @return the map
     */
    static Map<D, Boolean> getValidationSkipMap() {
        currentGormStaticApi().getValidationSkipMap()
    }

    static List<D> executeQuery(String query) {
        currentGormStaticApi().executeQuery query
    }

    static List<D> executeQuery(String query, Map args) {
        currentGormStaticApi().executeQuery query, args
    }

    static List<D> executeQuery(String query, Map params, Map args) {
        currentGormStaticApi().executeQuery query, params, args
    }

    static List<D> executeQuery(String query, Collection params) {
        currentGormStaticApi().executeQuery query, params
    }

    static List<D> executeQuery(String query, Object...params) {
        currentGormStaticApi().executeQuery query, params
    }

    static List<D> executeQuery(String query, Collection params, Map args) {
        currentGormStaticApi().executeQuery query, params, args
    }

    static Integer executeUpdate(String query) {
        currentGormStaticApi().executeUpdate query
    }

    static Integer executeUpdate(String query, Map args) {
        currentGormStaticApi().executeUpdate query, args
    }

    static Integer executeUpdate(String query, Map params, Map args) {
        currentGormStaticApi().executeUpdate query, params, args
    }

    static Integer executeUpdate(String query, Collection params) {
        currentGormStaticApi().executeUpdate query, params
    }

    static Integer executeUpdate(String query, Object...params) {
        currentGormStaticApi().executeUpdate query, params
    }

    static Integer executeUpdate(String query, Collection params, Map args) {
        currentGormStaticApi().executeUpdate query, params, args
    }

    static D find(String query) {
        currentGormStaticApi().find query
    }

    static D find(String query, Map args) {
        currentGormStaticApi().find query, args
    }

    static D find(String query, Map params, Map args) {
        currentGormStaticApi().find query, params, args
    }

    static D find(String query, Collection params) {
        currentGormStaticApi().find query, params
    }

    static D find(String query, Object[] params) {
        currentGormStaticApi().find query, params
    }

    static D find(String query, Collection params, Map args) {
        currentGormStaticApi().find query, params, args
    }

    static List<D> findAll(String query) {
        currentGormStaticApi().findAll query
    }

    static List<D> findAll(String query, Map params) {
        currentGormStaticApi().findAll query, params
    }

    static List<D> findAll(String query, Map params, Map args) {
        currentGormStaticApi().findAll query, params, args
    }

    static List<D> findAll(String query, Collection params) {
        currentGormStaticApi().findAll query, params
    }

    static List<D> findAll(String query, Object[] params) {
        currentGormStaticApi().findAll query, params
    }

    static List<D> findAll(String query, Collection params, Map args) {
        currentGormStaticApi().findAll query, params, args
    }
}
