package org.grails.datastore.gorm

import org.springframework.datastore.mapping.core.Datastore
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import org.springframework.datastore.mapping.model.PersistentEntity

/**
 * Abstract GORM API provider
 *
 * @author Graeme Rocher
 * @since 1.0
 */
abstract class AbstractGormApi {

  static final EXCLUDES = ['setProperty', 'getProperty', 'getMetaClass', 'setMetaClass','invokeMethod','getMethods', 'wait', 'equals', 'toString', 'hashCode', 'getClass', 'notify', 'notifyAll']
	
  protected Class persistentClass
  protected PersistentEntity persistentEntity
  protected Datastore datastore
  private List<Method> methods

  AbstractGormApi(Class persistentClass, Datastore datastore) {
    this.persistentClass = persistentClass;
    this.datastore = datastore
    this.persistentEntity = datastore.getMappingContext().getPersistentEntity(persistentClass.name)
	this.methods =  getClass().methods.findAll { Method m ->
       def mods = m.getModifiers()
       !m.isSynthetic() && !Modifier.isStatic(mods)&& !Modifier.isPrivate(mods) && !AbstractGormApi.EXCLUDES.contains(m.name)
     }
  }

  List<Method> getMethods() {  	methods  }

}
