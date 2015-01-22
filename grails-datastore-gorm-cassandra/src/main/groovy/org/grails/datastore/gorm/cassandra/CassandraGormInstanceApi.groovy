package org.grails.datastore.gorm.cassandra

import org.codehaus.groovy.runtime.InvokerHelper
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.mapping.cassandra.CassandraDatastore
import org.grails.datastore.mapping.cassandra.CassandraSession
import org.grails.datastore.mapping.cassandra.engine.CassandraEntityPersister
import org.grails.datastore.mapping.cassandra.utils.OptionsUtil
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.core.SessionCallback
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable

class CassandraGormInstanceApi<D> extends GormInstanceApi<D> {

    CassandraGormInstanceApi(Class<D> persistentClass, Datastore datastore) {
        super(persistentClass, datastore)        
    } 
	
	private CassandraEntityPersister getCassandraEntityPersister(Session session) {
		return (CassandraEntityPersister) session.getPersister(persistentClass)
	}
	
	/**
	 * Saves an object with the given parameters
	 * @param instance The instance
	 * @param params The parameters
	 * @return The instance
	 */
	D save(D instance, Map params) {		
		execute({ Session session ->									
			((CassandraDatastore)session.datastore).setWriteOptions(instance, OptionsUtil.convertToWriteOptions(params))			
			doSave instance, params, session						
		} as SessionCallback)
	}
	
	protected D doSave(D instance, Map params, Session s, boolean isInsert = false) {
		CassandraSession session = (CassandraSession) s
		boolean hasErrors = false
		boolean validate = params?.containsKey("validate") ? params.validate : true
		MetaMethod validateMethod = instance.respondsTo('validate', InvokerHelper.EMPTY_ARGS).find { MetaMethod mm -> mm.parameterTypes.length == 0 && !mm.vargsMethod}
		if (validateMethod && validate) {
			session.datastore.setSkipValidation(instance, false)
			hasErrors = !validateMethod.invoke(instance, InvokerHelper.EMPTY_ARGS)
		}
		else {
			session.datastore.setSkipValidation(instance, true)
			InvokerHelper.invokeMethod(instance, "clearErrors", null)
		}

		if (hasErrors) {
			boolean failOnErrorEnabled = params?.containsKey("failOnError") ? params.failOnError : failOnError
			if (failOnErrorEnabled) {
				throw validationException.newInstance("Validation error occurred during call to save()", InvokerHelper.getProperty(instance, "errors"))
			}
			return null
		}
		if (params?.update) {			
			session.update(instance)
		} else if (params?.updateSingleTypes) {			                   
            getCassandraEntityPersister(session).updateSingleTypes(instance)			
		} else {
    		if (isInsert) {
    			session.insert(instance)
    		}
    		else {
    			if(instance instanceof DirtyCheckable) {
    				// since this is an explicit call to save() we mark the instance as dirty to ensure it happens
    				instance.markDirty()
    			}
    			session.persist(instance)
    		}
		}
		if (params?.flush) {
			session.flush()
		}
		return instance
	}
	
	/**
	 * Update an instance without loading into the session
	 * @param instance The instance
	 * @param params The parameters
	 * @return Returns the instance
	 */
	D update(D instance, Map params = [:]) {
		params.update = true
		save(instance, params)
	}
	
	/**
	 * Update an instance's non collection, non map types only without loading into the session
	 * @param instance The instance
	 * @param params The parameters
	 * @return Returns the instance
	 */
	D updateSingleTypes(D instance, Map params = [:]) {
		params.updateSingleTypes = true
		save(instance, params)
	}	
	
	/**
	 * Add the specified element to the instance's embedded list, set or map and generate an update for the datastore
	 * @param instance the instance containing the collection or map
	 * @param propertyName the name of the embedded list, set or map
	 * @param element the element to add
	 * @param params The parameters
	 */
	def append(D instance, String propertyName, Object element, Map params = [:]) {
		execute ({ Session session ->			
			getCassandraEntityPersister(session).append(instance, propertyName, element, OptionsUtil.convertToWriteOptions(params))
			if (params.flush) {
				session.flush()
			}
		 } as SessionCallback)
	}
	
	/**
	 * Prepend the specified element to the instance's embedded list and generate an update for the datastore
	 * @param instance the instance containing the list 
	 * @param propertyName the name of the embedded list
	 * @param element the element to prepend
	 * @param params The parameters
	 */
	def prepend(D instance, String propertyName, Object element, Map params = [:]) {
		execute ({ Session session ->
			getCassandraEntityPersister(session).prepend(instance, propertyName, element, OptionsUtil.convertToWriteOptions(params))
			if (params.flush) {
				session.flush()
			}
		 } as SessionCallback)
	}
	
	/**
	 * Replace the specified element at the specified index in the instance's embedded list and generate an update for the datastore
	 * @param instance the instance containing the list
	 * @param propertyName the name of the embedded list
	 * @param index the index of the element to replace
	 * @param element the element to be stored at the specified index
	 * @param params The parameters
	 */
	def replaceAt(D instance, String propertyName, int index, Object element, Map params = [:]) {
		execute ({ Session session ->
			getCassandraEntityPersister(session).replaceAt(instance, propertyName, index, element, OptionsUtil.convertToWriteOptions(params))
			if (params.flush) {
				session.flush()
			}
		 } as SessionCallback)
	}
	
	/**
	 * Remove the specified element, or the element at the specified index, from the instance's embedded list, set or map and generate an update for the datastore
	 * @param instance the instance containing the collection or map
	 * @param propertyName the name of the embedded list, set or map
	 * @param item the element or index of the element to remove in the case of a list, the element in the case of a set or map
	 * @param isIndex whether the specified item is an element or the index of the element to remove, only true if removing from a list using index, false otherwise 
	 * @param params The parameters
	 */
	def deleteFrom(D instance, String propertyName, Object item, boolean isIndex, Map params = [:]) {
		if (isIndex && !(item instanceof Integer || item instanceof int)) {
			throw new IllegalArgumentException("Deleting item by index, item must be an integer")
		}
		execute ({ Session session ->
			getCassandraEntityPersister(session).deleteFrom(instance, propertyName, item, isIndex, OptionsUtil.convertToWriteOptions(params))
			if (params.flush) {
				session.flush()
			}
		 } as SessionCallback)
	}
}
