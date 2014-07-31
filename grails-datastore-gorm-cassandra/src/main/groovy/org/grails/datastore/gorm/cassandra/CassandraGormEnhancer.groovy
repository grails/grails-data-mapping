package org.grails.datastore.gorm.cassandra

import org.codehaus.groovy.runtime.InvokerHelper
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.core.Datastore
import org.springframework.transaction.PlatformTransactionManager

/**
 * Extends the default {@link GormEnhancer} adding supporting for passing arguments and Cassandra specific methods
 *
 */
class CassandraGormEnhancer extends GormEnhancer {

    CassandraGormEnhancer(Datastore datastore) {
       this(datastore, null)
    }
    
    CassandraGormEnhancer(Datastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager)
    }
    
    @Override
    protected <D> GormStaticApi<D> getStaticApi(Class<D> cls) {
        return new CassandraGormStaticApi<D>(cls, datastore, getFinders(), transactionManager)
    }
    
    @Override
    protected <D> GormInstanceApi<D> getInstanceApi(Class<D> cls) {
        final api = new CassandraGormInstanceApi<D>(cls, datastore)
        api.failOnError = failOnError
        return api
    }

}

class CassandraGormInstanceApi<D> extends GormInstanceApi<D> {

    CassandraGormInstanceApi(Class<D> persistentClass, Datastore datastore) {
        super(persistentClass, datastore)        
    } 
}

class CassandraGormStaticApi<D> extends GormStaticApi<D> {

    CassandraGormStaticApi(Class<D> persistentClass, Datastore datastore, List<FinderMethod> finders) {
        this(persistentClass, datastore, finders, null)
    }
            
    CassandraGormStaticApi(Class<D> persistentClass, Datastore datastore, List<FinderMethod> finders, PlatformTransactionManager transactionManager) {
        super(persistentClass, datastore, finders, transactionManager) 
        def finder =  gormDynamicFinders.find { FinderMethod f -> 
            "org.grails.datastore.gorm.finders.FindByFinder".equals(f.class.name)            
        }
        finder.registerNewMethodExpression(InList.class)
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
