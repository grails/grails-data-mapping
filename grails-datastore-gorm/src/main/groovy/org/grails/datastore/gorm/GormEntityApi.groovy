package org.grails.datastore.gorm

/**
 * API for instance methods defined by a GORM entity
 *
 * @author Graeme Rocher
 * @since 5.0.5
 *
 * @param <D> The entity type
 */
interface GormEntityApi<D> {
    /**
     * Proxy aware instanceOf implementation.
     */
    boolean instanceOf(Class cls)
    /**
     * Upgrades an existing persistence instance to a write lock
     * @return The instance
     */
    D lock()
    /**
     * Locks the instance for updates for the scope of the passed closure
     *
     * @param callable The closure
     * @return The result of the closure
     */
    def mutex(Closure callable)
    /**
     * Refreshes the state of the current instance
     * @return The instance
     */
    D refresh()
    /**
     * Saves an object the datastore
     * @return Returns the instance
     */
    D save()
    /**
     * Forces an insert of an object to the datastore
     * @return Returns the instance
     */
    D insert()
    /**
     * Forces an insert of an object to the datastore
     * @return Returns the instance
     */
    D insert(Map params)
    /**
     * Saves an object the datastore
     * @return Returns the instance
     */
    D merge()
    /**
     * Saves an object the datastore
     * @return Returns the instance
     */
    D merge(Map params)
    /**
     * Save method that takes a boolean which indicates whether to perform validation or not
     *
     * @param validate Whether to perform validation
     *
     * @return The instance or null if validation fails
     */
    D save(boolean validate)
    /**
     * Saves an object with the given parameters
     * @param instance The instance
     * @param params The parameters
     * @return The instance
     */
    D save(Map params)
    /**
     * Returns the objects identifier
     */
    Serializable ident()
    /**
     * Attaches an instance to an existing session. Requries a session-based model
     * @return
     */
    D attach()
    /**
     * No concept of session-based model so defaults to true
     */
    boolean isAttached()
    /**
     * Discards any pending changes. Requires a session-based model.
     */
    void discard()
    /**
     * Deletes an instance from the datastore
     */
    void delete()
    /**
     * Deletes an instance from the datastore
     */
    void delete(Map params)
    /**
     * Checks whether a field is dirty
     *
     * @param instance The instance
     * @param fieldName The name of the field
     *
     * @return true if the field is dirty
     */
    boolean isDirty(String fieldName)
    /**
     * Checks whether an entity is dirty
     *
     * @param instance The instance
     * @return true if it is dirty
     */
    boolean isDirty()
}