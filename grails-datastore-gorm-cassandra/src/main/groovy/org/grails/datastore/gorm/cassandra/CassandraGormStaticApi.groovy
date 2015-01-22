package org.grails.datastore.gorm.cassandra

import org.codehaus.groovy.runtime.InvokerHelper
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.finders.FindByFinder
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.cassandra.CassandraDatastore
import org.grails.datastore.mapping.cassandra.engine.CassandraEntityPersister
import org.grails.datastore.mapping.cassandra.utils.OptionsUtil
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.core.Session
import org.grails.datastore.mapping.core.SessionCallback
import org.springframework.core.convert.ConversionService
import org.springframework.data.cassandra.core.CassandraTemplate
import org.springframework.transaction.PlatformTransactionManager

class CassandraGormStaticApi<D> extends GormStaticApi<D> {
	
	org.springframework.data.cassandra.mapping.CassandraPersistentEntity<?> springCassandraPersistentEntity
	CassandraTemplate cassandraTemplate 
	ConversionService conversionService
	
    CassandraGormStaticApi(Class<D> persistentClass, Datastore datastore, List<FinderMethod> finders) {
        this(persistentClass, datastore, finders, null)
    }
            
    CassandraGormStaticApi(Class<D> persistentClass, Datastore datastore, List<FinderMethod> finders, PlatformTransactionManager transactionManager) {
        super(persistentClass, datastore, finders, transactionManager) 
		cassandraTemplate = ((CassandraDatastore) datastore).cassandraTemplate		
		springCassandraPersistentEntity = cassandraTemplate.getCassandraMappingContext().getExistingPersistentEntity(persistentClass)
        FindByFinder finder = (FindByFinder) gormDynamicFinders.find { FinderMethod f -> 
            "org.grails.datastore.gorm.finders.FindByFinder".equals(f.class.name)            
        }
        finder.registerNewMethodExpression(InList.class)
    }
    
	private CassandraEntityPersister getCassandraEntityPersister(Session session) {
		return (CassandraEntityPersister) session.getPersister(persistentClass)		
	}
	
    CassandraTemplate getCassandraTemplate() {
        cassandraTemplate
    }
    
	/**
	 * Update a property on an instance with the specified item 
	 * @param id the id of the instance to update
	 * @param propertyName the name of the property to update
	 * @param item the new value of the property
	 * @param params The parameters	
	 */
	def updateProperty(Serializable id, String propertyName, Object item, Map params = [:]) {
		execute ({ Session session ->
			getCassandraEntityPersister(session).updateProperty(id, propertyName, item, OptionsUtil.convertToWriteOptions(params))
			if (params.flush) {
				session.flush()
			}
		 } as SessionCallback)
	}
	
	/**
	 * Update multiple properties on an instance with the specified properties
	 * @param id the id of the instance to update
	 * @param properties a map of property name/value pairs to update
	 * @param params The parameters
	 */
	def updateProperties(Serializable id, Map properties, Map params = [:]) {
		execute ({ Session session ->
			getCassandraEntityPersister(session).updateProperties(id, properties, OptionsUtil.convertToWriteOptions(params))
			if (params.flush) {
				session.flush()
			}
		 } as SessionCallback)
	}

	/**
	 * Add the specified element to the instance's embedded list, set or map in the datastore
	 * @param id the id of the instance to update
	 * @param propertyName the name of the embedded list, set or map
	 * @param element the element to add
	 * @param params The parameters
	 */
	def append(Serializable id, String propertyName, Object element, Map params = [:]) {	
		execute ({ Session session ->						
			getCassandraEntityPersister(session).append(id, propertyName, element, OptionsUtil.convertToWriteOptions(params))	
			if (params.flush) {
				session.flush()
			}		
		 } as SessionCallback)
	}	
	
	/**
	 * Prepend the specified element to the instance's embedded list in the datastore
	 * @param id the id of the instance to update
	 * @param propertyName the name of the embedded list
	 * @param element the element to prepend
	 * @param params The parameters
	 */
	def prepend(Serializable id, String propertyName, Object element, Map params = [:]) {
		execute ({ Session session ->			
			getCassandraEntityPersister(session).prepend(id, propertyName, element, OptionsUtil.convertToWriteOptions(params))
			if (params.flush) {
				session.flush()
			}
		 } as SessionCallback)
	}
	
	/**
	 * Replace the specified element at the specified index in the instance's embedded list in the datastore
	 * @param id the id of the instance to update
	 * @param propertyName the name of the embedded list
	 * @param index the index of the element to replace
	 * @param element the element to be stored at the specified index
	 * @param params The parameters
	 */
	def replaceAt(Serializable id, String propertyName, int index, Object element, Map params = [:]) {
		execute ({ Session session ->
			getCassandraEntityPersister(session).replaceAt(id, propertyName, index, element, OptionsUtil.convertToWriteOptions(params))
			if (params.flush) {
				session.flush()
			}
		 } as SessionCallback)
	}
	
	/**
	 * Remove the specified element, or the element at the specified index, from the instance's embedded list, set or map in the datastore
	 * @param id the id of the instance to update
	 * @param propertyName the name of the embedded list, set or map
	 * @param item the element or index of the element to remove in the case of a list, the element in the case of a set or map
	 * @param isIndex whether the specified item is an element or the index of the element to remove, only true if removing from a list using index, false otherwise
	 * @param params The parameters
	 */
	def deleteFrom(Serializable id, String propertyName, Object element, boolean index, Map params = [:]) {
		execute ({ Session session ->
			getCassandraEntityPersister(session).deleteFrom(id, propertyName, element, index, OptionsUtil.convertToWriteOptions(params))
			if (params.flush) {
				session.flush()
			}
		 } as SessionCallback)
	}
	
    /**
     * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand").  If
     * a matching persistent entity is not found a new entity is created and returned.
     *
     * @param queryMap The map of conditions
     * @param args The Query arguments
     * @return A single result
      */
     D findOrCreateWhere(Map queryMap, Map args) {
         internalFindOrCreate(queryMap, args, false)
     }
 
    /**
     * Finds a single result matching all of the given conditions. Eg. Book.findWhere(author:"Stephen King", title:"The Stand").  If
     * a matching persistent entity is not found a new entity is created, saved and returned.
     * 
     * @param queryMap The map of conditions
     * @param args The Query arguments
     * @return A single result
      */
     D findOrSaveWhere(Map queryMap, Map args) {
         internalFindOrCreate(queryMap, args, true)
     }
     
     private D internalFindOrCreate(Map queryMap, Map args, boolean shouldSave) {
         D result = findWhere(queryMap, args)
         if (!result) {
             def persistentMetaClass = GroovySystem.metaClassRegistry.getMetaClass(persistentClass)
             result = (D)persistentMetaClass.invokeConstructor(queryMap)
             if (shouldSave) {
                 InvokerHelper.invokeMethod(result, "save", args)
             }
         }
         result
     }
    
}