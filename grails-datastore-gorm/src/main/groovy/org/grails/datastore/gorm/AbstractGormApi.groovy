package org.grails.datastore.gorm

import java.lang.reflect.Method
import java.lang.reflect.Modifier

import org.springframework.datastore.mapping.core.Datastore
import org.springframework.datastore.mapping.model.PersistentEntity

/**
 * Abstract GORM API provider
 *
 * @author Graeme Rocher
 * @since 1.0
 */
abstract class AbstractGormApi {

    static final EXCLUDES = [
        'setProperty',
        'getProperty',
        'getMetaClass',
        'setMetaClass',
        'invokeMethod',
        'getMethods',
        'getExtendedMethods',
        'wait',
        'equals',
        'toString',
        'hashCode',
        'getClass',
        'notify',
        'notifyAll',
        'setTransactionManager'
    ]

    protected Class persistentClass
    protected PersistentEntity persistentEntity
    protected Datastore datastore
    private List<Method> methods = []
    private List<Method> extendedMethods = []

    AbstractGormApi(Class persistentClass, Datastore datastore) {
        this.persistentClass = persistentClass;
        this.datastore = datastore
        this.persistentEntity = datastore.getMappingContext().getPersistentEntity(persistentClass.name)

        final clazz = getClass()
        while(clazz != Object) {
            final methodsToAdd = clazz.declaredMethods.findAll { Method m ->
                def mods = m.getModifiers()
                !m.isSynthetic() && !Modifier.isStatic(mods) && Modifier.isPublic(mods) &&
                        !AbstractGormApi.EXCLUDES.contains(m.name)
            }
            methods.addAll ( methodsToAdd )
            if(clazz != GormStaticApi && clazz != GormInstanceApi && clazz != GormValidationApi && clazz != AbstractGormApi) {
                extendedMethods.addAll( methodsToAdd )
            }
            clazz = clazz.getSuperclass()
        }
    }

    List<Method> getMethods() { methods }

    List<Method> getExtendedMethods() { extendedMethods }
}
