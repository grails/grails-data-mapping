package org.grails.datastore.gorm.cassandra

import org.codehaus.groovy.grails.commons.GrailsMetaClassUtils
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.model.PersistentEntity
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
	 void enhance(PersistentEntity e, boolean onlyExtendedMethods = false) {
		super.enhance(e, onlyExtendedMethods)
		addCollectionMethods(e)
	}
	 
	protected void addCollectionMethods(PersistentEntity e) {
		Class cls = e.javaClass
		ExpandoMetaClass mc = GrailsMetaClassUtils.getExpandoMetaClass(cls)
		final proxyFactory = datastore.mappingContext.proxyFactory
		for (p in e.persistentProperties) {
			def prop = p			
			def isCollectionOrMap = (Map.class.isAssignableFrom(prop.type) || Collection.class.isAssignableFrom(prop.type))			
			if (isCollectionOrMap) {												
				mc.static."appendTo${prop.capitilizedName}" = { Serializable id, Object obj, Map params = [:] ->						
					final targetObject = delegate		
					targetObject.append(id, prop.name, obj, params)														
					targetObject
				}
				mc.static."prependTo${prop.capitilizedName}" = { Serializable id, Object obj, Map params = [:] ->
					if (List.class.isAssignableFrom(prop.type)) {
						final targetObject = delegate
						targetObject.prepend(id, prop.name, obj, params)
						targetObject
					} else {
						throw new MissingMethodException("prependTo${prop.capitilizedName}", cls, [id, obj, params] as Object[])
					}
				}
				mc.static."replaceAtIn${prop.capitilizedName}" = { Serializable id, Object obj, int index, Map params = [:] ->
					if (List.class.isAssignableFrom(prop.type)) {
    					final targetObject = delegate
    					targetObject.replaceAt(id, prop.name, obj, index, params)
    					targetObject
					} else {
						throw new MissingMethodException("replaceAtIn${prop.capitilizedName}", cls, [id, obj, index, params] as Object[])
					}
				}
				mc.static."deleteFrom${prop.capitilizedName}" = { Serializable id, Object obj, Map params = [:] ->
					if (Collection.class.isAssignableFrom(prop.type)) {
    					final targetObject = delegate
    					targetObject.deleteFrom(id, prop.name, obj, params)
    					targetObject
					} else {
						throw new MissingMethodException("prependTo${prop.capitilizedName}", cls, [id, obj, params] as Object[])
					}				
				}
			}			
		}
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
