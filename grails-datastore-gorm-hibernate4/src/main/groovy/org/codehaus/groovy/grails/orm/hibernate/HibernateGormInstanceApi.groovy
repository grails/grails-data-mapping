package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.domain.GrailsDomainClassMappingContext
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.codehaus.groovy.grails.orm.hibernate.metaclass.MergePersistentMethod
import org.codehaus.groovy.grails.orm.hibernate.metaclass.SavePersistentMethod
import org.grails.datastore.gorm.GormInstanceApi
import org.hibernate.FlushMode
import org.hibernate.LockMode
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.engine.spi.EntityEntry
import org.hibernate.engine.spi.SessionImplementor
import org.hibernate.proxy.HibernateProxy
import org.springframework.dao.DataAccessException

/**
 * The implementation of the GORM instance API contract for Hibernate.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class HibernateGormInstanceApi extends GormInstanceApi {
    private static final EMPTY_ARRAY = [] as Object[]

    private SavePersistentMethod saveMethod
    private MergePersistentMethod mergeMethod
    private GrailsHibernateTemplate hibernateTemplate
    private SessionFactory sessionFactory
    private ClassLoader classLoader
    private boolean cacheQueriesByDefault = false

    private config = Collections.emptyMap()

    HibernateGormInstanceApi(Class persistentClass, HibernateDatastore datastore, ClassLoader classLoader) {
        super(persistentClass, datastore)

        this.classLoader = classLoader
        sessionFactory = datastore.getSessionFactory()

        def mappingContext = datastore.mappingContext
        if (mappingContext instanceof GrailsDomainClassMappingContext) {
            GrailsDomainClassMappingContext domainClassMappingContext = mappingContext
            def grailsApplication = domainClassMappingContext.getGrailsApplication()
            def domainClass = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, persistentClass.name)
            config = grailsApplication.config?.grails?.gorm
            saveMethod = new SavePersistentMethod(sessionFactory, classLoader, grailsApplication, domainClass, datastore)
            mergeMethod = new MergePersistentMethod(sessionFactory, classLoader, grailsApplication, domainClass, datastore)
            hibernateTemplate = new GrailsHibernateTemplate(sessionFactory, grailsApplication)
            cacheQueriesByDefault = GrailsHibernateUtil.isCacheQueriesByDefault(grailsApplication)
        } else {
            hibernateTemplate = new GrailsHibernateTemplate(sessionFactory)
        }
    }

    /**
     * Checks whether a field is dirty
     *
     * @param instance The instance
     * @param fieldName The name of the field
     *
     * @return true if the field is dirty
     */
    boolean isDirty(instance, String fieldName) {
        def session = sessionFactory.currentSession
        def entry = findEntityEntry(instance, session)
        if (!entry || !entry.loadedState) {
            return false
        }

        Object[] values = entry.persister.getPropertyValues(instance, session.entityMode)
        int[] dirtyProperties = entry.persister.findDirty(values, entry.loadedState, instance, session)
        int fieldIndex = entry.persister.propertyNames.findIndexOf { fieldName == it }
        return fieldIndex in dirtyProperties
    }

    /**
     * Checks whether an entity is dirty
     *
     * @param instance The instance
     * @return true if it is dirty
     */
    boolean isDirty(instance) {
        def session = sessionFactory.currentSession
        def entry = findEntityEntry(instance, session)
        if (!entry || !entry.loadedState) {
            return false
        }

        Object[] values = entry.persister.getPropertyValues(instance, session.entityMode)
        def dirtyProperties = entry.persister.findDirty(values, entry.loadedState, instance, session)
        return dirtyProperties != null
    }

    /**
     * Obtains a list of property names that are dirty
     *
     * @param instance The instance
     * @return A list of property names that are dirty
     */
    List getDirtyPropertyNames(instance) {
        def session = sessionFactory.currentSession
        def entry = findEntityEntry(instance, session)
        if (!entry || !entry.loadedState) {
            return []
        }

        Object[] values = entry.persister.getPropertyValues(instance, session.entityMode)
        int[] dirtyProperties = entry.persister.findDirty(values, entry.loadedState, instance, session)
        def names = []
        for (index in dirtyProperties) {
            names << entry.persister.propertyNames[index]
        }
        names
    }

    /**
     * Gets the original persisted value of a field.
     *
     * @param fieldName The field name
     * @return The original persisted value
     */
    Object getPersistentValue(instance, String fieldName) {
        def session = sessionFactory.currentSession
        def entry = findEntityEntry(instance, session, false)
        if (!entry || !entry.loadedState) {
            return null
        }

        int fieldIndex = entry.persister.propertyNames.findIndexOf { fieldName == it }
        return fieldIndex == -1 ? null : entry.loadedState[fieldIndex]
    }

    @Override
    Object lock(instance) {
        hibernateTemplate.lock(instance, LockMode.PESSIMISTIC_WRITE)
    }

    @Override
    Object refresh(instance) {
        hibernateTemplate.refresh(instance)
        return instance
    }

    @Override
    Object save(instance) {
        if (saveMethod) {
            return saveMethod.invoke(instance, "save", EMPTY_ARRAY)
        }
        return super.save(instance)
    }

    Object save(instance, boolean validate) {
        if (saveMethod) {
            return saveMethod.invoke(instance, "save", [validate] as Object[])
        }
        return super.save(instance, validate)
    }

    @Override
    Object merge(instance) {
        if (mergeMethod) {
            mergeMethod.invoke(instance, "merge", EMPTY_ARRAY)
        }
        else {
            return super.merge(instance)
        }
    }

    @Override
    Object merge(instance, Map params) {
        if (mergeMethod) {
            mergeMethod.invoke(instance, "merge", [params] as Object[])
        }
        else {
            return super.merge(instance, params)
        }
    }

    @Override
    Object save(instance, Map params) {
        if (saveMethod) {
            return saveMethod.invoke(instance, "save", [params] as Object[])
        }
        return super.save(instance, params)
    }

    @Override
    Object attach(instance) {
        hibernateTemplate.lock(instance, LockMode.NONE)
        return instance
    }

    @Override
    void discard(instance) {
        hibernateTemplate.evict instance
    }

    @Override
    void delete(instance) {
        def obj = instance
        try {
            hibernateTemplate.execute new GrailsHibernateTemplate.HibernateCallback() {
                def doInHibernate(Session session) {
                   session.delete obj
                   if (shouldFlush()) {
                       session.flush()
                   }
                }
            }
        }
        catch (DataAccessException e) {
            handleDataAccessException(hibernateTemplate, e)
        }
    }

    @Override
    void delete(instance, Map params) {
        def obj = instance
        hibernateTemplate.delete obj
        if (shouldFlush(params)) {
            try {
                hibernateTemplate.flush()
            }
            catch (DataAccessException e) {
                handleDataAccessException(hibernateTemplate, e)
            }
        }
    }

    @Override
    boolean instanceOf(instance, Class cls) {
        if (instance instanceof HibernateProxy) {
            return cls.isInstance(GrailsHibernateUtil.unwrapProxy(instance))
        }
        return cls.isInstance(instance)
    }

    @Override
    boolean isAttached(instance) {
        hibernateTemplate.contains instance
    }

    private EntityEntry findEntityEntry(instance, SessionImplementor session, boolean forDirtyCheck = true) {
        def entry = session.persistenceContext.getEntry(instance)
        if (!entry) {
            return null
        }

        if (forDirtyCheck && !entry.requiresDirtyCheck(instance) && entry.loadedState) {
            return null
        }

        entry
    }
    /**
    * Session should no longer be flushed after a data access exception occurs (such a constriant violation)
    */
   private void handleDataAccessException(GrailsHibernateTemplate template, DataAccessException e) {
       try {
           hibernateTemplate.execute new GrailsHibernateTemplate.HibernateCallback() {
               def doInHibernate(Session session) {
                   session.setFlushMode(FlushMode.MANUAL)
               }
           }
       }
       finally {
           throw e
       }
   }

   private boolean shouldFlush(Map map = [:]) {
       if (map?.containsKey('flush')) {
           return Boolean.TRUE == map.flush
       }
       return config.autoFlush instanceof Boolean ? config.autoFlush : false
   }
}
