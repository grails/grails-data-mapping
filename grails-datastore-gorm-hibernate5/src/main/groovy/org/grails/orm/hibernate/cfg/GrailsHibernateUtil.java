/* Copyright 2004-2005 Graeme Rocher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.orm.hibernate.cfg;

import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.grails.datastore.mapping.config.Entity;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.model.types.Embedded;
import org.grails.datastore.mapping.reflect.ClassUtils;
import org.grails.orm.hibernate.AbstractHibernateDatastore;
import org.grails.orm.hibernate.HibernateDatastore;
import org.grails.orm.hibernate.proxy.GroovyAwareJavassistProxyFactory;
import org.grails.orm.hibernate.proxy.HibernateProxyHandler;
import org.grails.orm.hibernate.support.HibernateRuntimeUtils;
import org.hibernate.*;
import org.hibernate.criterion.Order;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;
import org.springframework.core.convert.ConversionService;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Utility methods for configuring Hibernate inside Grails.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
public class GrailsHibernateUtil extends HibernateRuntimeUtils {
    protected static final Log LOG = LogFactory.getLog(GrailsHibernateUtil.class);

    public static final String ARGUMENT_FETCH_SIZE = "fetchSize";
    public static final String ARGUMENT_TIMEOUT = "timeout";
    public static final String ARGUMENT_READ_ONLY = "readOnly";
    public static final String ARGUMENT_FLUSH_MODE = "flushMode";
    public static final String ARGUMENT_MAX = "max";
    public static final String ARGUMENT_OFFSET = "offset";
    public static final String ARGUMENT_ORDER = "order";
    public static final String ARGUMENT_SORT = "sort";
    public static final String ORDER_DESC = "desc";
    public static final String ORDER_ASC = "asc";
    public static final String ARGUMENT_FETCH = "fetch";
    public static final String ARGUMENT_IGNORE_CASE = "ignoreCase";
    public static final String ARGUMENT_CACHE = "cache";
    public static final String ARGUMENT_LOCK = "lock";
    public static final Class<?>[] EMPTY_CLASS_ARRAY = {};


    private static HibernateProxyHandler proxyHandler = new HibernateProxyHandler();

    private static GrailsDomainBinder binder;



    public static void populateArgumentsForCriteria(AbstractHibernateDatastore datastore, Class<?> targetClass, Criteria c, Map argMap, ConversionService conversionService) {
        populateArgumentsForCriteria(datastore, targetClass, c, argMap, conversionService, true);
    }

    /**
     * Populates criteria arguments for the given target class and arguments map
     *
     * @param datastore the GrailsApplication instance
     * @param targetClass The target class
     * @param c The criteria instance
     * @param argMap The arguments map
     */
    @SuppressWarnings("rawtypes")
    public static void populateArgumentsForCriteria(AbstractHibernateDatastore datastore, Class<?> targetClass, Criteria c, Map argMap, ConversionService conversionService, boolean useDefaultMapping) {
        Integer maxParam = null;
        Integer offsetParam = null;
        if (argMap.containsKey(ARGUMENT_MAX)) {
            maxParam = conversionService.convert(argMap.get(ARGUMENT_MAX),Integer.class);
        }
        if (argMap.containsKey(ARGUMENT_OFFSET)) {
            offsetParam = conversionService.convert(argMap.get(ARGUMENT_OFFSET),Integer.class);
        }
        if (argMap.containsKey(ARGUMENT_FETCH_SIZE)) {
            c.setFetchSize(conversionService.convert(argMap.get(ARGUMENT_FETCH_SIZE),Integer.class));
        }
        if (argMap.containsKey(ARGUMENT_TIMEOUT)) {
            c.setTimeout(conversionService.convert(argMap.get(ARGUMENT_TIMEOUT),Integer.class));
        }
        if (argMap.containsKey(ARGUMENT_FLUSH_MODE)) {
            c.setFlushMode(convertFlushMode(argMap.get(ARGUMENT_FLUSH_MODE)));
        }
        if (argMap.containsKey(ARGUMENT_READ_ONLY)) {
            c.setReadOnly(ClassUtils.getBooleanFromMap(ARGUMENT_READ_ONLY, argMap));
        }
        String orderParam = (String)argMap.get(ARGUMENT_ORDER);
        Object fetchObj = argMap.get(ARGUMENT_FETCH);
        if (fetchObj instanceof Map) {
            Map fetch = (Map)fetchObj;
            for (Object o : fetch.keySet()) {
                String associationName = (String) o;
                c.setFetchMode(associationName, getFetchMode(fetch.get(associationName)));
            }
        }

        final int max = maxParam == null ? -1 : maxParam;
        final int offset = offsetParam == null ? -1 : offsetParam;
        if (max > -1) {
            c.setMaxResults(max);
        }
        if (offset > -1) {
            c.setFirstResult(offset);
        }
        if (ClassUtils.getBooleanFromMap(ARGUMENT_LOCK, argMap)) {
            c.setLockMode(LockMode.PESSIMISTIC_WRITE);
            c.setCacheable(false);
        }
        else {
            if (argMap.containsKey(ARGUMENT_CACHE)) {
                c.setCacheable(ClassUtils.getBooleanFromMap(ARGUMENT_CACHE, argMap));
            } else {
                cacheCriteriaByMapping(targetClass, c);
            }
        }

        final Object sortObj = argMap.get(ARGUMENT_SORT);
        if (sortObj != null) {
            boolean ignoreCase = true;
            Object caseArg = argMap.get(ARGUMENT_IGNORE_CASE);
            if (caseArg instanceof Boolean) {
                ignoreCase = (Boolean) caseArg;
            }
            if (sortObj instanceof Map) {
                Map sortMap = (Map) sortObj;
                for (Object sort : sortMap.keySet()) {
                    final String order = ORDER_DESC.equalsIgnoreCase((String) sortMap.get(sort)) ? ORDER_DESC : ORDER_ASC;
                    addOrderPossiblyNested(datastore, c, targetClass, (String) sort, order, ignoreCase);
                }
            } else {
                final String sort = (String) sortObj;
                final String order = ORDER_DESC.equalsIgnoreCase(orderParam) ? ORDER_DESC : ORDER_ASC;
                addOrderPossiblyNested(datastore, c, targetClass, sort, order, ignoreCase);
            }
        }
        else if (useDefaultMapping) {
            Mapping m = GrailsDomainBinder.getMapping(targetClass);
            if (m != null) {
                Map sortMap = m.getSort().getNamesAndDirections();
                for (Object sort : sortMap.keySet()) {
                    final String order = ORDER_DESC.equalsIgnoreCase((String) sortMap.get(sort)) ? ORDER_DESC : ORDER_ASC;
                    addOrderPossiblyNested(datastore, c, targetClass, (String) sort, order, true);
                }
            }
        }
    }

    public static void setBinder(GrailsDomainBinder binder) {
        GrailsHibernateUtil.binder = binder;
    }

    /**
     * Populates criteria arguments for the given target class and arguments map
     *
     * @param targetClass The target class
     * @param c The criteria instance
     * @param argMap The arguments map
     *
     * @deprecated Use {@link #populateArgumentsForCriteria(HibernateDatastore, Class, org.hibernate.Criteria, java.util.Map)} instead
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    public static void populateArgumentsForCriteria(Class<?> targetClass, Criteria c, Map argMap, ConversionService conversionService) {
        populateArgumentsForCriteria(null, targetClass, c, argMap, conversionService);
    }

    @SuppressWarnings("rawtypes")
    public static void populateArgumentsForCriteria(Criteria c, Map argMap, ConversionService conversionService) {
        populateArgumentsForCriteria(null, null, c, argMap, conversionService);
    }

    private static FlushMode convertFlushMode(Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof FlushMode) {
            return (FlushMode)object;
        }
        return FlushMode.valueOf(String.valueOf(object));
    }

    /**
     * Add order to criteria, creating necessary subCriteria if nested sort property (ie. sort:'nested.property').
     */
    private static void addOrderPossiblyNested(AbstractHibernateDatastore datastore, Criteria c, Class<?> targetClass, String sort, String order, boolean ignoreCase) {
        int firstDotPos = sort.indexOf(".");
        if (firstDotPos == -1) {
            addOrder(c, sort, order, ignoreCase);
        } else { // nested property
            String sortHead = sort.substring(0,firstDotPos);
            String sortTail = sort.substring(firstDotPos+1);
            PersistentProperty property = getGrailsDomainClassProperty(datastore, targetClass, sortHead);
            if (property instanceof Embedded) {
                // embedded objects cannot reference entities (at time of writing), so no more recursion needed
                addOrder(c, sort, order, ignoreCase);
            } else if(property instanceof Association) {
                Criteria subCriteria = c.createCriteria(sortHead);
                Class<?> propertyTargetClass = ((Association)property).getAssociatedEntity().getJavaClass();
                GrailsHibernateUtil.cacheCriteriaByMapping(datastore, propertyTargetClass, subCriteria);
                addOrderPossiblyNested(datastore, subCriteria, propertyTargetClass, sortTail, order, ignoreCase); // Recurse on nested sort
            }
        }
    }

    /**
     * Add order directly to criteria.
     */
    private static void addOrder(Criteria c, String sort, String order, boolean ignoreCase) {
        if (ORDER_DESC.equals(order)) {
            c.addOrder(ignoreCase ? Order.desc(sort).ignoreCase() : Order.desc(sort));
        }
        else {
            c.addOrder(ignoreCase ? Order.asc(sort).ignoreCase() : Order.asc(sort));
        }
    }

    /**
     * Get hold of the GrailsDomainClassProperty represented by the targetClass' propertyName,
     * assuming targetClass corresponds to a GrailsDomainClass.
     */
    private static PersistentProperty getGrailsDomainClassProperty(AbstractHibernateDatastore datastore, Class<?> targetClass, String propertyName) {
        PersistentEntity grailsClass = datastore != null ? datastore.getMappingContext().getPersistentEntity( targetClass.getName()) : null;
        if (grailsClass == null) {
            throw new IllegalArgumentException("Unexpected: class is not a domain class:"+targetClass.getName());
        }
        return grailsClass.getPropertyByName(propertyName);
    }

    /**
     * Configures the criteria instance to cache based on the configured mapping.
     *
     * @param targetClass The target class
     * @param criteria The criteria
     */
    public static void cacheCriteriaByMapping(Class<?> targetClass, Criteria criteria) {
        Mapping m = GrailsDomainBinder.getMapping(targetClass);
        if (m != null && m.getCache() != null && m.getCache().getEnabled()) {
            criteria.setCacheable(true);
        }
    }

    public static void cacheCriteriaByMapping(AbstractHibernateDatastore datastore, Class<?> targetClass, Criteria criteria) {
        cacheCriteriaByMapping(targetClass, criteria);
    }

    /**
     * Retrieves the fetch mode for the specified instance; otherwise returns the default FetchMode.
     *
     * @param object The object, converted to a string
     * @return The FetchMode
     */
    public static FetchMode getFetchMode(Object object) {
        String name = object != null ? object.toString() : "default";
        if (name.equalsIgnoreCase(FetchMode.JOIN.toString()) || name.equalsIgnoreCase("eager")) {
            return FetchMode.JOIN;
        }
        if (name.equalsIgnoreCase(FetchMode.SELECT.toString()) || name.equalsIgnoreCase("lazy")) {
            return FetchMode.SELECT;
        }
        return FetchMode.DEFAULT;
    }

    /**
     * Sets the target object to read-only using the given SessionFactory instance. This
     * avoids Hibernate performing any dirty checking on the object
     *
     * @see #setObjectToReadWrite(Object, org.hibernate.SessionFactory)
     *
     * @param target The target object
     * @param sessionFactory The SessionFactory instance
     */
    public static void setObjectToReadyOnly(Object target, SessionFactory sessionFactory) {
        Session session = sessionFactory.getCurrentSession();
        if (canModifyReadWriteState(session, target)) {
            if (target instanceof HibernateProxy) {
                target = ((HibernateProxy)target).getHibernateLazyInitializer().getImplementation();
            }
            session.setReadOnly(target, true);
            session.setFlushMode(FlushMode.MANUAL);
        }
    }

    private static boolean canModifyReadWriteState(Session session, Object target) {
        return session.contains(target) && Hibernate.isInitialized(target);
    }

    /**
     * Sets the target object to read-write, allowing Hibernate to dirty check it and auto-flush changes.
     *
     * @see #setObjectToReadyOnly(Object, org.hibernate.SessionFactory)
     *
     * @param target The target object
     * @param sessionFactory The SessionFactory instance
     */
    public static void setObjectToReadWrite(final Object target, SessionFactory sessionFactory) {
        Session session = sessionFactory.getCurrentSession();
        if (!canModifyReadWriteState(session, target)) {
            return;
        }

        SessionImplementor sessionImpl = (SessionImplementor) session;
        EntityEntry ee = sessionImpl.getPersistenceContext().getEntry(target);

        if (ee == null || ee.getStatus() != Status.READ_ONLY) {
            return;
        }

        Object actualTarget = target;
        if (target instanceof HibernateProxy) {
            actualTarget = ((HibernateProxy)target).getHibernateLazyInitializer().getImplementation();
        }

        session.setReadOnly(actualTarget, false);
        session.setFlushMode(FlushMode.AUTO);
        incrementVersion(target);
    }

    /**
     * Increments the entities version number in order to force an update
     * @param target The target entity
     */
    public static void incrementVersion(Object target) {
        MetaClass metaClass = GroovySystem.getMetaClassRegistry().getMetaClass(target.getClass());
        if (metaClass.hasProperty(target, GormProperties.VERSION)!=null) {
            Object version = metaClass.getProperty(target, GormProperties.VERSION);
            if (version instanceof Long) {
                Long newVersion = (Long) version + 1;
                metaClass.setProperty(target, GormProperties.VERSION, newVersion);
            }
        }
    }

    /**
     * Ensures the meta class is correct for a given class
     *
     * @param target The GroovyObject
     * @param persistentClass The persistent class
     */
    public static void ensureCorrectGroovyMetaClass(Object target, Class<?> persistentClass) {
        if (target instanceof GroovyObject) {
            GroovyObject go = ((GroovyObject)target);
            if (!go.getMetaClass().getTheClass().equals(persistentClass)) {
                go.setMetaClass(GroovySystem.getMetaClassRegistry().getMetaClass(persistentClass));
            }
        }
    }

    /**
     * Unwraps and initializes a HibernateProxy.
     * @param proxy The proxy
     * @return the unproxied instance
     */
    public static Object unwrapProxy(HibernateProxy proxy) {
        return proxyHandler.unwrapProxy(proxy);
    }

    /**
     * Returns the proxy for a given association or null if it is not proxied
     *
     * @param obj The object
     * @param associationName The named assoication
     * @return A proxy
     */
    public static HibernateProxy getAssociationProxy(Object obj, String associationName) {
        return proxyHandler.getAssociationProxy(obj, associationName);
    }

    /**
     * Checks whether an associated property is initialized and returns true if it is
     *
     * @param obj The name of the object
     * @param associationName The name of the association
     * @return true if is initialized
     */
    public static boolean isInitialized(Object obj, String associationName) {
        return proxyHandler.isInitialized(obj, associationName);
    }

    public static GroovyAwareJavassistProxyFactory buildProxyFactory(PersistentClass persistentClass) {
        GroovyAwareJavassistProxyFactory proxyFactory = new GroovyAwareJavassistProxyFactory();

        @SuppressWarnings("unchecked")
        Set<Class<HibernateProxy>> proxyInterfaces = new HashSet<Class<HibernateProxy>>();
        proxyInterfaces.add(HibernateProxy.class);

        final Class<?> javaClass = persistentClass.getMappedClass();
        final Property identifierProperty = persistentClass.getIdentifierProperty();
        final Method idGetterMethod = identifierProperty!=null?  identifierProperty.getGetter(javaClass).getMethod() : null;
        final Method idSetterMethod = identifierProperty!=null? identifierProperty.getSetter(javaClass).getMethod() : null;
        final Type identifierType = persistentClass.hasEmbeddedIdentifier() ? persistentClass.getIdentifier().getType() : null;

        try {
            proxyFactory.postInstantiate(persistentClass.getEntityName(), javaClass, proxyInterfaces,
                    idGetterMethod, idSetterMethod,
                    identifierType instanceof CompositeType ?
                            (CompositeType) identifierType :
                            null);
        }
        catch (HibernateException e) {
            LOG.warn("Cannot instantiate proxy factory: " + e.getMessage());
            return null;
        }

        return proxyFactory;
    }

    public static Object unwrapIfProxy(Object instance) {
        return proxyHandler.unwrapIfProxy(instance);
    }

    public static boolean usesDatasource(PersistentEntity domainClass, String dataSourceName) {
        List<String> names = getDatasourceNames(domainClass);
        return names.contains(dataSourceName) ||
                names.contains(Mapping.ALL_DATA_SOURCES);
    }

    /**
     * If a domain class uses more than one datasource, we need to know which one to use
     * when calling a method without a namespace qualifier.
     *
     * @param domainClass the domain class
     * @return the default datasource name
     */
    public static String getDefaultDataSource(PersistentEntity domainClass) {
        List<String> names = getDatasourceNames(domainClass);
        if (names.size() == 1 && Mapping.ALL_DATA_SOURCES.equals(names.get(0))) {
            return Mapping.DEFAULT_DATA_SOURCE;
        }
        return names.get(0);
    }

    public static List<String> getDatasourceNames(PersistentEntity domainClass) {
        final Entity mappedForm = domainClass.getMapping().getMappedForm();
        if(mappedForm instanceof Mapping)  {
            Mapping mapping = (Mapping) mappedForm;
            return mapping.getDatasources();
        }
        return Collections.singletonList(Mapping.DEFAULT_DATA_SOURCE);
    }

    public static boolean isMappedWithHibernate(PersistentEntity domainClass) {
        return domainClass instanceof HibernatePersistentEntity;
    }



    public static String qualify(final String prefix, final String name) {
        return StringHelper.qualify(prefix, name);
    }

    public static boolean isNotEmpty(final String string) {
        return StringHelper.isNotEmpty(string);
    }

    public static String unqualify(final String qualifiedName) {
        return StringHelper.unqualify(qualifiedName);
    }


}
