package org.grails.datastore.gorm.cassandra

import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.finders.DynamicFinder
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
		DynamicFinder.registerNewMethodExpression(InList.class)
    }
    
	@Override
	 void enhance(PersistentEntity e, boolean onlyExtendedMethods = false) {
		super.enhance(e, onlyExtendedMethods)
		addCollectionMethods(e)
	}
	 
	protected void addCollectionMethods(PersistentEntity e) {
		Class cls = e.javaClass
		MetaClass mc = cls.metaClass
		final proxyFactory = datastore.mappingContext.proxyFactory
		for (p in e.persistentProperties) {
			def prop = p			
			def isCollectionOrMap = (Map.class.isAssignableFrom(prop.type) || Collection.class.isAssignableFrom(prop.type))			
			if (isCollectionOrMap) {
				mc."appendTo${prop.capitilizedName}" = { Object item, Map params = [:] ->
					final targetObject = delegate
					if (Map.class.isAssignableFrom(prop.type)) {
						if (targetObject[prop.name] == null) {
							targetObject[prop.name] = [:]
						}
						targetObject[prop.name].putAll(item)
					} else {
    					if (targetObject[prop.name] == null) {
    						targetObject[prop.name] = [].asType(prop.type)
    					}
						targetObject[prop.name].add(item)
					}
					targetObject.append(prop.name, item, params)
					targetObject
				}
				mc.static."appendTo${prop.capitilizedName}" = { Object id, Object item, Map params = [:] ->						
					final targetObject = delegate		
					targetObject.append(id, prop.name, item, params)														
					targetObject
				}		
				mc."prependTo${prop.capitilizedName}" = { Object item, Map params = [:] ->
					if (List.class.isAssignableFrom(prop.type)) {
						final targetObject = delegate
						if (targetObject[prop.name] == null) {
							targetObject[prop.name] = []
						}
						targetObject[prop.name].add(0, item)
						targetObject.prepend(prop.name, item, params)
						targetObject
					} else {
						throw new MissingMethodException("prependTo${prop.capitilizedName}", cls, [item, params] as Object[])
					}
				}
				mc.static."prependTo${prop.capitilizedName}" = { Object id, Object item, Map params = [:] ->
					if (List.class.isAssignableFrom(prop.type)) {
						final targetObject = delegate
						targetObject.prepend(id, prop.name, item, params)
						targetObject
					} else {
						throw new MissingMethodException("prependTo${prop.capitilizedName}", cls, [id, item, params] as Object[])
					}
				}
				mc."replaceAtIn${prop.capitilizedName}" = { int index, Object item, Map params = [:] ->
					if (List.class.isAssignableFrom(prop.type)) {
						final targetObject = delegate						
						targetObject[prop.name]?.set(index, item)
						targetObject.replaceAt(prop.name, index, item, params)
						targetObject
					} else {
						throw new MissingMethodException("replaceAtIn${prop.capitilizedName}", cls, [index, item, params] as Object[])
					}
				}
				mc.static."replaceAtIn${prop.capitilizedName}" = { Object id, int index, Object item, Map params = [:] ->
					if (List.class.isAssignableFrom(prop.type)) {
    					final targetObject = delegate
    					targetObject.replaceAt(id, prop.name, index, item, params)
    					targetObject
					} else {
						throw new MissingMethodException("replaceAtIn${prop.capitilizedName}", cls, [id, index, item, params] as Object[])
					}
				}
				mc."deleteFrom${prop.capitilizedName}" = { Object item, Map params = [:] ->					
					final targetObject = delegate
					targetObject[prop.name]?.remove((Object)item)
					targetObject.deleteFrom(prop.name, item, false, params)
					targetObject					
				}
				mc.static."deleteFrom${prop.capitilizedName}" = { Object id, Object item, Map params = [:] ->					
					final targetObject = delegate
					targetObject.deleteFrom(id, prop.name, item, false, params)
					targetObject								
				}
				mc."deleteAtFrom${prop.capitilizedName}" = { int index, Map params = [:] ->
					if (List.class.isAssignableFrom(prop.type)) {
    					final targetObject = delegate
    					targetObject[prop.name]?.remove(index)
    					targetObject.deleteFrom(prop.name, index, true, params)
    					targetObject
					} else {
						throw new MissingMethodException("deleteAtFrom${prop.capitilizedName}", cls, [index, params] as Object[])
					}
				}
				mc.static."deleteAtFrom${prop.capitilizedName}" = { Object id, int index, Map params = [:] ->
					if (List.class.isAssignableFrom(prop.type)) {
    					final targetObject = delegate
    					targetObject.deleteFrom(id, prop.name, index, true, params)
    					targetObject
					} else {
						throw new MissingMethodException("deleteAtFrom${prop.capitilizedName}", cls, [id, index, params] as Object[])
					}
				}
			}			
		}
	}
	

}
