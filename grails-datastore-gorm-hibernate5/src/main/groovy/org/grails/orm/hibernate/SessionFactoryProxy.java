/*
 * Copyright 2011 SpringSource
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
package org.grails.orm.hibernate;

import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.hibernate.*;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.QueryCache;
import org.hibernate.cache.spi.Region;
import org.hibernate.cache.spi.UpdateTimestampsCache;
import org.hibernate.cache.spi.access.RegionAccessStrategy;
import org.hibernate.cfg.Settings;
import org.hibernate.context.spi.CurrentSessionContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.engine.ResultSetMappingDefinition;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.engine.query.spi.QueryPlanCache;
import org.hibernate.engine.spi.*;
import org.hibernate.exception.spi.SQLExceptionConverter;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.internal.NamedQueryRepository;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.stat.Statistics;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.type.Type;
import org.hibernate.type.TypeResolver;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.InfrastructureProxy;
import org.springframework.util.ReflectionUtils;

import javax.naming.NamingException;
import javax.naming.Reference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * <p>Proxies the SessionFactory allowing for the underlying SessionFactory instance to be replaced at runtime.
 * Used to enable rebuilding of the SessionFactory at development time</p>
 *
 * <p>NOTE: This class is not for production use and is development time only!</p>
 *
 * @since 2.0
 * @author Graeme Rocher
 */
@SuppressWarnings("rawtypes")
public class SessionFactoryProxy extends GroovyObjectSupport implements SessionFactoryImplementor, ApplicationContextAware, InitializingBean, InfrastructureProxy  {

    private static final long serialVersionUID = 1;

    private String targetBean;
    private ApplicationContext applicationContext;
    private Class currentSessionContextClass = GrailsSessionContext.class;
    private SessionFactory currentSessionFactory;

    /**
     * The target bean to proxy
     *
     * @param targetBean The name of the target bean
     */
    public void setTargetBean(String targetBean) {
        this.targetBean = targetBean;
    }

    @Override
    public Object getProperty(String property) {
        try {
            return super.getProperty(property);
        } catch (MissingPropertyException e) {
            return InvokerHelper.getProperty(getCurrentSessionFactory(), property);
        }
    }

    /**
     * The class to use for the current session context
     *
     * @param currentSessionContextClass The current session context class
     */
    public void setCurrentSessionContextClass(Class currentSessionContextClass) {
        this.currentSessionContextClass = currentSessionContextClass;
    }

    /**
     * @return The current SessionFactory being proxied
     */
    public SessionFactory getCurrentSessionFactory() {

        SessionFactory sf = applicationContext.getBean(targetBean, SessionFactoryHolder.class).getSessionFactory();

        if (!sf.equals(currentSessionFactory)) {
            currentSessionFactory = sf;
            updateCurrentSessionContext(sf);
        }

        return currentSessionFactory;
    }

    /**
     * @return The current SessionFactoryImplementor being proxied
     */
    public SessionFactoryImplementor getCurrentSessionFactoryImplementor() {
        return (SessionFactoryImplementor) getCurrentSessionFactory();
    }

    public Session openSession() throws HibernateException {
        return getCurrentSessionFactory().openSession();
    }

    public Session getCurrentSession() throws HibernateException {
        return getCurrentSessionFactory().getCurrentSession();
    }

    public StatelessSession openStatelessSession() {
        return getCurrentSessionFactory().openStatelessSession();
    }

    public StatelessSession openStatelessSession(Connection connection) {
        return getCurrentSessionFactory().openStatelessSession(connection);
    }

    public ClassMetadata getClassMetadata(Class entityClass) {
        return getCurrentSessionFactory().getClassMetadata(entityClass);
    }

    public ClassMetadata getClassMetadata(String entityName) {
        return getCurrentSessionFactory().getClassMetadata(entityName);
    }

    public CollectionMetadata getCollectionMetadata(String roleName) {
        return getCurrentSessionFactory().getCollectionMetadata(roleName);
    }

    public Map<String, ClassMetadata> getAllClassMetadata() {
        return getCurrentSessionFactory().getAllClassMetadata();
    }

    public Map getAllCollectionMetadata() {
        return getCurrentSessionFactory().getAllCollectionMetadata();
    }

    public Statistics getStatistics() {
        return getCurrentSessionFactory().getStatistics();
    }

    public void close() throws HibernateException {
        getCurrentSessionFactory().close();
    }

    public boolean isClosed() {
        return getCurrentSessionFactory().isClosed();
    }

    public Cache getCache() {
        return getCurrentSessionFactory().getCache();
    }

    public Set getDefinedFilterNames() {
        return getCurrentSessionFactory().getDefinedFilterNames();
    }

    public FilterDefinition getFilterDefinition(String filterName) throws HibernateException {
        return getCurrentSessionFactory().getFilterDefinition(filterName);
    }

    public boolean containsFetchProfileDefinition(String name) {
        return getCurrentSessionFactory().containsFetchProfileDefinition(name);
    }

    public TypeHelper getTypeHelper() {
        return getCurrentSessionFactory().getTypeHelper();
    }

    public Reference getReference() throws NamingException {
        return getCurrentSessionFactory().getReference();
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public TypeResolver getTypeResolver() {
        return getCurrentSessionFactoryImplementor().getTypeResolver();
    }

    public Properties getProperties() {
        return getCurrentSessionFactoryImplementor().getProperties();
    }

    @Override
    public String getUuid() {
        return getCurrentSessionFactoryImplementor().getUuid();
    }

    public EntityPersister getEntityPersister(String entityName) throws MappingException {
        return getCurrentSessionFactoryImplementor().getEntityPersister(entityName);
    }

    public CollectionPersister getCollectionPersister(String role) throws MappingException {
        return getCurrentSessionFactoryImplementor().getCollectionPersister(role);
    }

    public Dialect getDialect() {
        return getCurrentSessionFactoryImplementor().getDialect();
    }

    public Interceptor getInterceptor() {
        return getCurrentSessionFactoryImplementor().getInterceptor();
    }

    public QueryPlanCache getQueryPlanCache() {
        return getCurrentSessionFactoryImplementor().getQueryPlanCache();
    }

    public Type[] getReturnTypes(String queryString) throws HibernateException {
        return getCurrentSessionFactoryImplementor().getReturnTypes(queryString);
    }

    public String[] getReturnAliases(String queryString) throws HibernateException {
        return getCurrentSessionFactoryImplementor().getReturnAliases(queryString);
    }

    public String[] getImplementors(String className) throws MappingException {
        return getCurrentSessionFactoryImplementor().getImplementors(className);
    }

    public String getImportedClassName(String name) {
        return getCurrentSessionFactoryImplementor().getImportedClassName(name);
    }

    public QueryCache getQueryCache() {
        return getCurrentSessionFactoryImplementor().getQueryCache();
    }

    public QueryCache getQueryCache(String regionName) throws HibernateException {
        return getCurrentSessionFactoryImplementor().getQueryCache(regionName);
    }

    public UpdateTimestampsCache getUpdateTimestampsCache() {
        return getCurrentSessionFactoryImplementor().getUpdateTimestampsCache();
    }

    public StatisticsImplementor getStatisticsImplementor() {
        return getCurrentSessionFactoryImplementor().getStatisticsImplementor();
    }

    public NamedQueryDefinition getNamedQuery(String queryName) {
        return getCurrentSessionFactoryImplementor().getNamedQuery(queryName);
    }

    @Override
    public void registerNamedQueryDefinition(String name, NamedQueryDefinition definition) {
        getCurrentSessionFactoryImplementor().registerNamedQueryDefinition(name,definition);
    }

    public NamedSQLQueryDefinition getNamedSQLQuery(String queryName) {
        return getCurrentSessionFactoryImplementor().getNamedSQLQuery(queryName);
    }

    @Override
    public void registerNamedSQLQueryDefinition(String name, NamedSQLQueryDefinition definition) {
        getCurrentSessionFactoryImplementor().registerNamedSQLQueryDefinition(name, definition);
    }

    public ResultSetMappingDefinition getResultSetMapping(String name) {
        return getCurrentSessionFactoryImplementor().getResultSetMapping(name);
    }

    public IdentifierGenerator getIdentifierGenerator(String rootEntityName) {
        return getCurrentSessionFactoryImplementor().getIdentifierGenerator(rootEntityName);
    }

    public Region getSecondLevelCacheRegion(String regionName) {
        return getCurrentSessionFactoryImplementor().getSecondLevelCacheRegion(regionName);
    }

    @Override
    public RegionAccessStrategy getSecondLevelCacheRegionAccessStrategy(String regionName) {
        return getCurrentSessionFactoryImplementor().getSecondLevelCacheRegionAccessStrategy(regionName);
    }

    public Map getAllSecondLevelCacheRegions() {
        return getCurrentSessionFactoryImplementor().getAllSecondLevelCacheRegions();
    }

    public SQLExceptionConverter getSQLExceptionConverter() {
        return getCurrentSessionFactoryImplementor().getSQLExceptionConverter();
    }

    public Settings getSettings() {
        return getCurrentSessionFactoryImplementor().getSettings();
    }

    public Session openTemporarySession() throws HibernateException {
        return getCurrentSessionFactoryImplementor().openTemporarySession();
    }

    public Set<String> getCollectionRolesByEntityParticipant(String entityName) {
        return getCurrentSessionFactoryImplementor().getCollectionRolesByEntityParticipant(entityName);
    }

    public EntityNotFoundDelegate getEntityNotFoundDelegate() {
        return getCurrentSessionFactoryImplementor().getEntityNotFoundDelegate();
    }

    public SQLFunctionRegistry getSqlFunctionRegistry() {
        return getCurrentSessionFactoryImplementor().getSqlFunctionRegistry();
    }

    public FetchProfile getFetchProfile(String name) {
        return getCurrentSessionFactoryImplementor().getFetchProfile(name);
    }

    @Deprecated
    public IdentifierGeneratorFactory getIdentifierGeneratorFactory() {
        return getCurrentSessionFactoryImplementor().getIdentifierGeneratorFactory();
    }

    public Type getIdentifierType(String className) throws MappingException {
        return getCurrentSessionFactoryImplementor().getIdentifierType(className);
    }

    public String getIdentifierPropertyName(String className) throws MappingException {
        return getCurrentSessionFactoryImplementor().getIdentifierPropertyName(className);
    }

    public Type getReferencedPropertyType(String className, String propertyName) throws MappingException {
        return getCurrentSessionFactoryImplementor().getReferencedPropertyType(className,propertyName);
    }

    public void afterPropertiesSet() {
        getCurrentSessionFactoryImplementor();
    }

    private void updateCurrentSessionContext(SessionFactory sessionFactory) {
        // patch the currentSessionContext variable of SessionFactoryImpl to use this proxy as the key
        CurrentSessionContext ssc = createCurrentSessionContext();
        if (ssc instanceof GrailsSessionContext) {
            ((GrailsSessionContext)ssc).initJta();
        }

        try {
            Class<? extends SessionFactory> sessionFactoryClass = sessionFactory.getClass();
            Field currentSessionContextField = sessionFactoryClass.getDeclaredField("currentSessionContext");
            if (currentSessionContextField != null) {
                ReflectionUtils.makeAccessible(currentSessionContextField);
                currentSessionContextField.set(sessionFactory, ssc);
            }
        } catch (NoSuchFieldException e) {
            // ignore
        } catch (SecurityException e) {
            // ignore
        } catch (IllegalArgumentException e) {
            // ignore
        } catch (IllegalAccessException e) {
            // ignore
        }
    }

    @SuppressWarnings("unchecked")
    protected CurrentSessionContext createCurrentSessionContext() {
        Class sessionContextClass = currentSessionContextClass;
        if (sessionContextClass == null) {
            sessionContextClass = GrailsSessionContext.class;
        }
        try {
            Constructor<CurrentSessionContext> constructor = sessionContextClass.getConstructor(SessionFactoryImplementor.class);
            return BeanUtils.instantiateClass(constructor, this);
        } catch (NoSuchMethodException e) {
            return new GrailsSessionContext(this);
        }
    }

    public Object getWrappedObject() {
        return getCurrentSessionFactory();
    }

    public SessionFactoryOptions getSessionFactoryOptions() {
        return getCurrentSessionFactoryImplementor().getSessionFactoryOptions();
    }

    public StatelessSessionBuilder withStatelessOptions() {
        return getCurrentSessionFactoryImplementor().withStatelessOptions();
    }

    public SessionBuilderImplementor withOptions() {
        return getCurrentSessionFactoryImplementor().withOptions();
    }

    public Map<String, EntityPersister> getEntityPersisters() {
        return getCurrentSessionFactoryImplementor().getEntityPersisters();
    }

    public Map<String, CollectionPersister> getCollectionPersisters() {
        return getCurrentSessionFactoryImplementor().getCollectionPersisters();
    }

    public JdbcServices getJdbcServices() {
        return getCurrentSessionFactoryImplementor().getJdbcServices();
    }

    public Region getNaturalIdCacheRegion(String regionName) {
        return getCurrentSessionFactoryImplementor().getNaturalIdCacheRegion(regionName);
    }

    @Override
    public RegionAccessStrategy getNaturalIdCacheRegionAccessStrategy(String regionName) {
        return null;
    }

    public SqlExceptionHelper getSQLExceptionHelper() {
        return getCurrentSessionFactoryImplementor().getSQLExceptionHelper();
    }

    public ServiceRegistryImplementor getServiceRegistry() {
        return getCurrentSessionFactoryImplementor().getServiceRegistry();
    }

    public void addObserver(SessionFactoryObserver observer) {
        getCurrentSessionFactoryImplementor().addObserver(observer);
    }

    public CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy() {
        return getCurrentSessionFactoryImplementor().getCustomEntityDirtinessStrategy();
    }

    public CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver() {
        return getCurrentSessionFactoryImplementor().getCurrentTenantIdentifierResolver();
    }

    @Override
    public NamedQueryRepository getNamedQueryRepository() {
        return getCurrentSessionFactoryImplementor().getNamedQueryRepository();
    }

    @Override
    public Iterable<EntityNameResolver> iterateEntityNameResolvers() {
        return getCurrentSessionFactoryImplementor().iterateEntityNameResolvers();
    }

    @Override
    public EntityPersister locateEntityPersister(Class byClass) {
        return null;
    }

    @Override
    public EntityPersister locateEntityPersister(String byName) {
        return null;
    }

    @Override
    public DeserializationResolver getDeserializationResolver() {
        return null;
    }
}
