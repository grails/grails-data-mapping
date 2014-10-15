package org.grails.datastore.gorm.cassandra

import groovy.transform.CompileStatic

import org.codehaus.groovy.runtime.InvokerHelper
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.mapping.cassandra.CassandraDatastore
import org.grails.datastore.mapping.cassandra.engine.CassandraEntityPersister
import org.grails.datastore.mapping.cassandra.utils.OptionsUtil
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.core.SessionCallback
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable

@CompileStatic
class CassandraGormInstanceApi<D> extends GormInstanceApi<D> {

    CassandraGormInstanceApi(Class<D> persistentClass, Datastore datastore) {
        super(persistentClass, datastore)        
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
	
	protected D doSave(D instance, Map params, Session session, boolean isInsert = false) {
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
		if (params?.updateSimpleTypes) {			
            CassandraEntityPersister persister = (CassandraEntityPersister) session.getPersister(persistentClass)            
            persister.updateSimpleTypes(instance)			
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
	 * Update an object's non collection, non map types only to the datastore
	 * @param instance The instance
	 * @return Returns the instance
	 */
	D update(D instance, Map params = [:]) {
		params.updateSimpleTypes = true
		save(instance, params)
	}
}
