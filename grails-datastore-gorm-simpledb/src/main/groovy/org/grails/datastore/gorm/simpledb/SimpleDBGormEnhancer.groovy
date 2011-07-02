package org.grails.datastore.gorm.simpledb

import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormInstanceApi
import org.grails.datastore.gorm.GormStaticApi
import org.springframework.datastore.mapping.core.Datastore
import org.springframework.datastore.mapping.core.Session
import org.springframework.datastore.mapping.core.SessionCallback
import org.springframework.transaction.PlatformTransactionManager

import org.springframework.datastore.mapping.engine.EntityPersister
import org.springframework.datastore.mapping.simpledb.engine.NativeSimpleDBItem

/**
 * GORM enhancer for SimpleDB.
 *
 * @author Roman Stepanenko based on Graeme Rocher code for MongoDb and Redis
 * @since 0.1
 */
class SimpleDBGormEnhancer extends GormEnhancer {

    SimpleDBGormEnhancer(Datastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager)
    }

    SimpleDBGormEnhancer(Datastore datastore) {
        this(datastore, null)
    }

    protected <D> GormStaticApi<D> getStaticApi(Class<D> cls) {
        return new SimpleDBGormStaticApi<D>(cls, datastore, finders)
    }

    protected <D> GormInstanceApi<D> getInstanceApi(Class<D> cls) {
        return new SimpleDBGormInstanceApi<D>(cls, datastore)
    }
}

class SimpleDBGormInstanceApi<D> extends GormInstanceApi<D> {

    SimpleDBGormInstanceApi(Class<D> persistentClass, Datastore datastore) {
        super(persistentClass, datastore)
    }

    /**
     * Allows subscript access to schemaless attributes.
     *
     * @param instance The instance
     * @param name The name of the field
     */
    void putAt(D instance, String name, value) {
        if (instance.hasProperty(name)) {
            instance.setProperty(name, value)
        }
        else {
            getDbo(instance)?.put name, value
        }
    }

    /**
     * Allows subscript access to schemaless attributes.
     *
     * @param instance The instance
     * @param name The name of the field
     * @return the value
     */
    def getAt(D instance, String name) {
        if (instance.hasProperty(name)) {
            return instance.getProperty(name)
        }

        def dbo = getDbo(instance)
        if (dbo != null && dbo.containsField(name)) {
            return org.springframework.datastore.mapping.simpledb.util.SimpleDBTemplate.get(name)
        }
        return null
    }

    /**
     * Return the DBObject instance for the entity
     *
     * @param instance The instance
     * @return The NativeSimpleDBItem instance
     */
    NativeSimpleDBItem getDbo(D instance) {
        execute (new SessionCallback<NativeSimpleDBItem>() {
            NativeSimpleDBItem doInSession(Session session) {

                if (!session.contains(instance) && !instance.save()) {
                    throw new IllegalStateException(
                        "Cannot obtain DBObject for transient instance, save a valid instance first")
                }

                EntityPersister persister = session.getPersister(instance)
                def id = persister.getObjectIdentifier(instance)
                return session.getCachedEntry(persister.getPersistentEntity(), id)
            }
        })
    }
}

class SimpleDBGormStaticApi<D> extends GormStaticApi<D> {
    SimpleDBGormStaticApi(Class<D> persistentClass, Datastore datastore, List<FinderMethod> finders) {
        super(persistentClass, datastore, finders)
    }

    @Override
    SimpleDBCriteriaBuilder createCriteria() {
        return new SimpleDBCriteriaBuilder(persistentClass, datastore.currentSession)
    }
}
