package org.grails.datastore.gorm

import org.springframework.datastore.core.Datastore
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import org.springframework.datastore.mapping.PersistentEntity

/**
 * Abstract GORM API provider
 *
 * @author Graeme Rocher
 * @since 1.0
 */
abstract class AbstractGormApi {

  protected Class persistentClass
  protected PersistentEntity persistentEntity
  protected Datastore datastore

  AbstractGormApi(Class persistentClass, Datastore datastore) {
    this.persistentClass = persistentClass;
    this.datastore = datastore
    this.persistentEntity = datastore.getMappingContext().getPersistentEntity(persistentClass.name)
  }

  static final EXCLUDES = ['setProperty', 'getProperty', 'getMetaClass', 'setMetaClass','invokeMethod','getMethodNames', 'wait', 'equals', 'toString', 'hashCode', 'getClass', 'notify', 'notifyAll']

  List<String> getMethodNames() {
     getClass().methods.findAll { Method m ->
       def mods = m.getModifiers()
       !m.isSynthetic() && !Modifier.isStatic(mods)&& !Modifier.isPrivate(mods) && !AbstractGormApi.EXCLUDES.contains(m.name)
     }*.name
  }

}
