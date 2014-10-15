package org.grails.datastore.gorm.cassandra

import groovy.transform.CompileStatic

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

@CompileStatic
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
    
	def updateProperty(Serializable id, String propertyName, Object item, Map params = [:]) {
		execute ({ Session session ->
			getCassandraEntityPersister(session).updateProperty(id, propertyName, item, OptionsUtil.convertToWriteOptions(params))
			if (params.flush) {
				session.flush()
			}
		 } as SessionCallback)
	}
	
	def updateProperties(Serializable id, Map properties, Map params = [:]) {
		execute ({ Session session ->
			getCassandraEntityPersister(session).updateProperties(id, properties, OptionsUtil.convertToWriteOptions(params))
			if (params.flush) {
				session.flush()
			}
		 } as SessionCallback)
	}

	
	def append(Serializable id, String propertyName, Object item, Map params = [:]) {	
		execute ({ Session session ->						
			getCassandraEntityPersister(session).append(id, propertyName, item, OptionsUtil.convertToWriteOptions(params))	
			if (params.flush) {
				session.flush()
			}		
		 } as SessionCallback)
	}	
	
	def prepend(Serializable id, String propertyName, Object item, Map params = [:]) {
		execute ({ Session session ->			
			getCassandraEntityPersister(session).prepend(id, propertyName, item, OptionsUtil.convertToWriteOptions(params))
			if (params.flush) {
				session.flush()
			}
		 } as SessionCallback)
	}
	
	def replaceAt(Serializable id, String propertyName, Object item, int index, Map params = [:]) {
		execute ({ Session session ->
			getCassandraEntityPersister(session).replaceAt(id, propertyName, item, index, OptionsUtil.convertToWriteOptions(params))
			if (params.flush) {
				session.flush()
			}
		 } as SessionCallback)
	}
	
	def deleteFrom(Serializable id, String propertyName, Object item, Map params = [:]) {
		execute ({ Session session ->
			getCassandraEntityPersister(session).deleteFrom(id, propertyName, item, OptionsUtil.convertToWriteOptions(params))
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
                 InvokerHelper.invokeMethod(result, "save", null)
             }
         }
         result
     }
    
}