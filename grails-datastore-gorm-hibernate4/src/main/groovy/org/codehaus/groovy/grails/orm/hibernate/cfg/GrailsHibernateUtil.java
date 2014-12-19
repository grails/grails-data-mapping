/*
 * Copyright 2004-2005 Graeme Rocher
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
package org.codehaus.groovy.grails.orm.hibernate.cfg;

import grails.core.GrailsApplication;
import grails.core.GrailsClass;
import grails.core.GrailsDomainClass;
import grails.core.GrailsDomainClassProperty;
import grails.util.CollectionUtils;
import grails.util.GrailsClassUtils;
import grails.util.Holders;
import groovy.lang.GroovyObject;
import groovy.lang.GroovySystem;
import groovy.lang.MetaClass;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.orm.hibernate.GrailsHibernateDomainClass;
import org.codehaus.groovy.grails.orm.hibernate.proxy.GroovyAwareJavassistProxyFactory;
import org.codehaus.groovy.grails.orm.hibernate.proxy.HibernateProxyHandler;
import org.codehaus.groovy.grails.orm.hibernate.query.HibernateQueryConstants;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.grails.core.DefaultGrailsDomainClass;
import org.grails.core.artefact.DomainClassArtefactHandler;
import org.hibernate.*;
import org.hibernate.criterion.Order;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.property.Getter;
import org.hibernate.property.Setter;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.CompositeType;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.convert.ConversionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility methods for configuring Hibernate inside Grails.
 *
 * @author Graeme Rocher
 * @since 0.4
 */
public class GrailsHibernateUtil implements HibernateQueryConstants{
    protected static final Log LOG = LogFactory.getLog(GrailsHibernateUtil.class);

    private static final String DYNAMIC_FILTER_ENABLER = "dynamicFilterEnabler";


    public static final Class<?>[] EMPTY_CLASS_ARRAY = {};


    private static HibernateProxyHandler proxyHandler = new HibernateProxyHandler();

    private static GrailsDomainBinder binder = new GrailsDomainBinder();

    @SuppressWarnings("rawtypes")
    public static void enableDynamicFilterEnablerIfPresent(SessionFactory sessionFactory, Session session) {
        if (sessionFactory != null && session != null) {
            final Set definedFilterNames = sessionFactory.getDefinedFilterNames();
            if (definedFilterNames != null && definedFilterNames.contains(DYNAMIC_FILTER_ENABLER))
                session.enableFilter(DYNAMIC_FILTER_ENABLER); // work around for HHH-2624
        }
    }

    public static void configureHibernateDomainClasses(SessionFactory sessionFactory,
            String sessionFactoryName, GrailsApplication application) {
        Map<String, GrailsDomainClass> hibernateDomainClassMap = new HashMap<String, GrailsDomainClass>();
        for (Object o : sessionFactory.getAllClassMetadata().values()) {
            ClassMetadata classMetadata = (ClassMetadata) o;
            configureDomainClass(sessionFactory, sessionFactoryName, application, classMetadata,
                    classMetadata.getMappedClass(), hibernateDomainClassMap);
        }
        configureInheritanceMappings(hibernateDomainClassMap);
    }

    @SuppressWarnings("rawtypes")
    public static void configureInheritanceMappings(Map hibernateDomainClassMap) {
        // now get through all domainclasses, and add all subclasses to root class
        for (Object o : hibernateDomainClassMap.values()) {
            GrailsDomainClass baseClass = (GrailsDomainClass) o;
            if (!baseClass.isRoot()) {
                Class<?> superClass = baseClass.getClazz().getSuperclass();

                while (!superClass.equals(Object.class) && !superClass.equals(GroovyObject.class)) {
                    GrailsDomainClass gdc = (GrailsDomainClass) hibernateDomainClassMap.get(superClass.getName());

                    if (gdc == null || gdc.getSubClasses() == null) {
                        LOG.debug("did not find superclass names when mapping inheritance....");
                        break;
                    }
                    gdc.getSubClasses().add(baseClass);
                    superClass = superClass.getSuperclass();
                }
            }
        }
    }

    private static void configureDomainClass(SessionFactory sessionFactory, String sessionFactoryName,
            GrailsApplication application, ClassMetadata cmd, Class<?> persistentClass,
            Map<String, GrailsDomainClass> hibernateDomainClassMap) {
        LOG.trace("Configuring domain class [" + persistentClass + "]");
        GrailsDomainClass dc = (GrailsDomainClass) application.getArtefact(DomainClassArtefactHandler.TYPE, persistentClass.getName());
        if (dc == null && sessionFactory.getClassMetadata(persistentClass) != null) {
            // a patch to add inheritance to this system
            GrailsHibernateDomainClass ghdc = new GrailsHibernateDomainClass(
                    persistentClass, sessionFactory, sessionFactoryName, application, cmd);

            hibernateDomainClassMap.put(persistentClass.getName(), ghdc);
            dc = (GrailsDomainClass) application.addArtefact(DomainClassArtefactHandler.TYPE, ghdc);
        }
    }

    public static void populateArgumentsForCriteria(GrailsApplication grailsApplication, Class<?> targetClass, Criteria c, Map argMap, ConversionService conversionService) {
        populateArgumentsForCriteria(grailsApplication, targetClass, c, argMap, conversionService, true);
    }

    /**
     * Populates criteria arguments for the given target class and arguments map
     *
     * @param grailsApplication the GrailsApplication instance
     * @param targetClass The target class
     * @param c The criteria instance
     * @param argMap The arguments map
     */
    @SuppressWarnings("rawtypes")
    public static void populateArgumentsForCriteria(GrailsApplication grailsApplication, Class<?> targetClass, Criteria c, Map argMap, ConversionService conversionService, boolean useDefaultMapping) {
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
            c.setReadOnly(GrailsClassUtils.getBooleanFromMap(ARGUMENT_READ_ONLY, argMap));
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
        if (GrailsClassUtils.getBooleanFromMap(ARGUMENT_LOCK, argMap)) {
            c.setLockMode(LockMode.PESSIMISTIC_WRITE);
            c.setCacheable(false);
        }
        else {
            if (argMap.containsKey(ARGUMENT_CACHE)) {
                c.setCacheable(GrailsClassUtils.getBooleanFromMap(ARGUMENT_CACHE, argMap));
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
                    addOrderPossiblyNested(grailsApplication, c, targetClass, (String) sort, order, ignoreCase);
                }
            } else {
                final String sort = (String) sortObj;
                final String order = ORDER_DESC.equalsIgnoreCase(orderParam) ? ORDER_DESC : ORDER_ASC;
                addOrderPossiblyNested(grailsApplication, c, targetClass, sort, order, ignoreCase);
            }
        }
        else if (useDefaultMapping) {
            Mapping m = binder.getMapping(targetClass);
            if (m != null) {
                Map sortMap = m.getSort().getNamesAndDirections();
                for (Object sort : sortMap.keySet()) {
                    final String order = ORDER_DESC.equalsIgnoreCase((String) sortMap.get(sort)) ? ORDER_DESC : ORDER_ASC;
                    addOrderPossiblyNested(grailsApplication, c, targetClass, (String) sort, order, true);
                }
            }
        }
    }

    /**
     * Populates criteria arguments for the given target class and arguments map
     *
     * @param targetClass The target class
     * @param c The criteria instance
     * @param argMap The arguments map
     *
     * @deprecated Use {@link #populateArgumentsForCriteria(org.codehaus.groovy.grails.commons.GrailsApplication, Class, org.hibernate.Criteria, java.util.Map)} instead
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
    private static void addOrderPossiblyNested(GrailsApplication grailsApplication, Criteria c, Class<?> targetClass, String sort, String order, boolean ignoreCase) {
        int firstDotPos = sort.indexOf(".");
        if (firstDotPos == -1) {
            addOrder(c, sort, order, ignoreCase);
        } else { // nested property
            String sortHead = sort.substring(0,firstDotPos);
            String sortTail = sort.substring(firstDotPos+1);
            GrailsDomainClassProperty property = getGrailsDomainClassProperty(grailsApplication, targetClass, sortHead);
            if(property != null) {
                if (property.isEmbedded()) {
                    // embedded objects cannot reference entities (at time of writing), so no more recursion needed
                    addOrder(c, sort, order, ignoreCase);
                } else {
                    Criteria subCriteria = c.createCriteria(sortHead);
                    Class<?> propertyTargetClass = property.getReferencedDomainClass().getClazz();
                    GrailsHibernateUtil.cacheCriteriaByMapping(grailsApplication, propertyTargetClass, subCriteria);
                    addOrderPossiblyNested(grailsApplication, subCriteria, propertyTargetClass, sortTail, order, ignoreCase); // Recurse on nested sort
                }

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
    private static GrailsDomainClassProperty getGrailsDomainClassProperty(GrailsApplication grailsApplication, Class<?> targetClass, String propertyName) {
        GrailsClass grailsClass = grailsApplication != null ? grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, targetClass.getName()) : null;
        if (!(grailsClass instanceof GrailsDomainClass)) {
            throw new IllegalArgumentException("Unexpected: class is not a domain class:"+targetClass.getName());
        }
        GrailsDomainClass domainClass = (GrailsDomainClass) grailsClass;
        return domainClass.getPropertyByName(propertyName);
    }

    /**
     * Configures the criteria instance to cache based on the configured mapping.
     *
     * @param targetClass The target class
     * @param criteria The criteria
     */
    public static void cacheCriteriaByMapping(Class<?> targetClass, Criteria criteria) {
        Mapping m = binder.getMapping(targetClass);
        if (m != null && m.getCache() != null && m.getCache().getEnabled()) {
            criteria.setCacheable(true);
        }
    }

    public static void cacheCriteriaByMapping(GrailsApplication grailsApplication, Class<?> targetClass, Criteria criteria) {
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
        if (metaClass.hasProperty(target, GrailsDomainClassProperty.VERSION)!=null) {
            Object version = metaClass.getProperty(target, GrailsDomainClassProperty.VERSION);
            if (version instanceof Long) {
                Long newVersion = (Long) version + 1;
                metaClass.setProperty(target, GrailsDomainClassProperty.VERSION, newVersion);
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

    public static boolean isCacheQueriesByDefault() {
        return isCacheQueriesByDefault(Holders.getGrailsApplication());
    }

    public static boolean isCacheQueriesByDefault(GrailsApplication grailsApplication) {
        return isConfigTrue(grailsApplication, CONFIG_PROPERTY_CACHE_QUERIES);
    }

    public static boolean isOsivReadonly(GrailsApplication grailsApplication) {
        return isConfigTrue(grailsApplication, CONFIG_PROPERTY_OSIV_READONLY);
    }
    
    public static boolean isPassReadOnlyToHibernate(GrailsApplication grailsApplication) {
        return isConfigTrue(grailsApplication, CONFIG_PROPERTY_PASS_READONLY_TO_HIBERNATE);
    }    

    /**
     * Checks if a Config parameter is true or a System property with the same name is true
     *
     * @param application
     * @param propertyName
     * @return true if the Config parameter is true or the System property with the same name is true
     */
    public static boolean isConfigTrue(GrailsApplication application, String propertyName) {
        return ((application != null && application.getFlatConfig() != null && DefaultTypeTransformation.castToBoolean(application.getFlatConfig().get(propertyName))) ||
                Boolean.getBoolean(propertyName));
    }

    public static GroovyAwareJavassistProxyFactory buildProxyFactory(PersistentClass persistentClass) {
        GroovyAwareJavassistProxyFactory proxyFactory = new GroovyAwareJavassistProxyFactory();

        @SuppressWarnings("unchecked")
        Set<Class<HibernateProxy>> proxyInterfaces = CollectionUtils.newSet(HibernateProxy.class);

        final Class<?> javaClass = persistentClass.getMappedClass();
        final Property identifierProperty = persistentClass.getIdentifierProperty();
        final Getter idGetter = identifierProperty!=null?  identifierProperty.getGetter(javaClass) : null;
        final Setter idSetter = identifierProperty!=null? identifierProperty.getSetter(javaClass) : null;

        if (idGetter == null || idSetter == null) return null;

        try {
            proxyFactory.postInstantiate(persistentClass.getEntityName(), javaClass, proxyInterfaces,
                    idGetter.getMethod(), idSetter.getMethod(),
                    persistentClass.hasEmbeddedIdentifier() ?
                            (CompositeType) persistentClass.getIdentifier().getType() :
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

    public static boolean usesDatasource(GrailsDomainClass domainClass, String dataSourceName) {
        if (domainClass instanceof GrailsHibernateDomainClass) {
            GrailsHibernateDomainClass hibernateDomainClass = (GrailsHibernateDomainClass)domainClass;
            String sessionFactoryName = hibernateDomainClass.getSessionFactoryName();
            if (dataSourceName.equals(GrailsDomainClassProperty.DEFAULT_DATA_SOURCE)) {
                return "sessionFactory".equals(sessionFactoryName);
            }
            return sessionFactoryName.endsWith("_" + dataSourceName);
        }

        List<String> names = getDatasourceNames(domainClass);
        return names.contains(dataSourceName) ||
               names.contains(GrailsDomainClassProperty.ALL_DATA_SOURCES);
    }

    /**
     * If a domain class uses more than one datasource, we need to know which one to use
     * when calling a method without a namespace qualifier.
     *
     * @param domainClass the domain class
     * @return the default datasource name
     */
    public static String getDefaultDataSource(GrailsDomainClass domainClass) {
        if (domainClass instanceof GrailsHibernateDomainClass) {
            GrailsHibernateDomainClass hibernateDomainClass = (GrailsHibernateDomainClass)domainClass;
            String sessionFactoryName = hibernateDomainClass.getSessionFactoryName();
            if ("sessionFactory".equals(sessionFactoryName)) {
                return GrailsDomainClassProperty.DEFAULT_DATA_SOURCE;
            }
            return sessionFactoryName.substring("sessionFactory_".length());
        }

        List<String> names = getDatasourceNames(domainClass);
        if (names.size() == 1 && GrailsDomainClassProperty.ALL_DATA_SOURCES.equals(names.get(0))) {
            return GrailsDomainClassProperty.DEFAULT_DATA_SOURCE;
        }
        return names.get(0);
    }

    public static List<String> getDatasourceNames(GrailsDomainClass domainClass) {
        // Mappings won't have been built yet when this is called from
        // HibernatePluginSupport.doWithSpring  so do a temporary evaluation but don't cache it
        Mapping mapping = isMappedWithHibernate(domainClass) ? binder.evaluateMapping(domainClass, null, false) : null;
        if (mapping == null) {
            mapping = new Mapping();
        }
        return mapping.getDatasources();
    }

    public static boolean isMappedWithHibernate(GrailsDomainClass domainClass) {
        return domainClass instanceof GrailsHibernateDomainClass || domainClass.getMappingStrategy().equals( GrailsDomainClass.GORM );
    }

    public static void autoAssociateBidirectionalOneToOnes(DefaultGrailsDomainClass domainClass, Object target) {
        List<GrailsDomainClassProperty> associations = domainClass.getAssociations();
        for (GrailsDomainClassProperty association : associations) {
            if (!association.isOneToOne() || !association.isBidirectional() || !association.isOwningSide()) {
                continue;
            }

            if (!isInitialized(target, association.getName())) {
                continue;
            }

            GrailsDomainClassProperty otherSide = association.getOtherSide();
            if (otherSide == null) {
                continue;
            }

            BeanWrapper bean = new BeanWrapperImpl(target);
            Object inverseObject = bean.getPropertyValue(association.getName());
            if (inverseObject == null) {
                continue;
            }

            if (!isInitialized(inverseObject, otherSide.getName())) {
                continue;
            }

            BeanWrapper inverseBean = new BeanWrapperImpl(inverseObject);
            Object propertyValue = inverseBean.getPropertyValue(otherSide.getName());
            if (propertyValue == null) {
                inverseBean.setPropertyValue(otherSide.getName(), target);
            }
        }
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
