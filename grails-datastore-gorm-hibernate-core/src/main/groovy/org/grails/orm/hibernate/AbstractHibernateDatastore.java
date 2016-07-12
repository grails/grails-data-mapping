/* Copyright (C) 2011 SpringSource
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

import grails.gorm.multitenancy.Tenants;
import groovy.lang.Closure;
import org.grails.datastore.mapping.config.Settings;
import org.grails.datastore.mapping.core.AbstractDatastore;
import org.grails.datastore.mapping.core.Datastore;
import org.grails.datastore.mapping.core.DatastoreAware;
import org.grails.datastore.mapping.core.connections.*;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.datastore.mapping.multitenancy.MultiTenancySettings;
import org.grails.datastore.mapping.multitenancy.MultiTenantCapableDatastore;
import org.grails.datastore.mapping.multitenancy.TenantResolver;
import org.grails.datastore.mapping.multitenancy.resolvers.FixedTenantResolver;
import org.grails.orm.hibernate.cfg.HibernateMappingContext;
import org.grails.orm.hibernate.connections.HibernateConnectionSource;
import org.grails.orm.hibernate.connections.HibernateConnectionSourceSettings;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertyResolver;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Datastore implementation that uses a Hibernate SessionFactory underneath.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public abstract class AbstractHibernateDatastore extends AbstractDatastore implements ApplicationContextAware, Settings, MultiTenantCapableDatastore<SessionFactory, HibernateConnectionSourceSettings>, Closeable {

    public static final String CONFIG_PROPERTY_CACHE_QUERIES = "grails.hibernate.cache.queries";
    public static final String CONFIG_PROPERTY_OSIV_READONLY = "grails.hibernate.osiv.readonly";
    public static final String CONFIG_PROPERTY_PASS_READONLY_TO_HIBERNATE = "grails.hibernate.pass.readonly";
    protected final SessionFactory sessionFactory;
    protected final ConnectionSources<SessionFactory, HibernateConnectionSourceSettings> connectionSources;
    protected final String defaultFlushModeName;
    protected final MultiTenancySettings.MultiTenancyMode multiTenantMode;
    protected AbstractEventTriggeringInterceptor eventTriggeringInterceptor;
    protected final boolean osivReadOnly;
    protected final boolean passReadOnlyToHibernate;
    protected final boolean isCacheQueries;
    protected final int defaultFlushMode;
    protected final boolean failOnError;
    protected final String dataSourceName;
    protected final TenantResolver tenantResolver;

    protected AbstractHibernateDatastore(ConnectionSources<SessionFactory, HibernateConnectionSourceSettings> connectionSources, HibernateMappingContext mappingContext) {
        super(mappingContext, connectionSources.getBaseConfiguration(), null);
        this.connectionSources = connectionSources;
        ConnectionSource<SessionFactory, HibernateConnectionSourceSettings> defaultConnectionSource = connectionSources.getDefaultConnectionSource();
        this.dataSourceName = defaultConnectionSource.getName();
        this.sessionFactory = defaultConnectionSource.getSource();
        HibernateConnectionSourceSettings settings = defaultConnectionSource.getSettings();
        HibernateConnectionSourceSettings.HibernateSettings hibernateSettings = settings.getHibernate();
        this.osivReadOnly = hibernateSettings.getOsiv().isReadonly();
        this.passReadOnlyToHibernate = hibernateSettings.isReadOnly();
        this.isCacheQueries = hibernateSettings.getCache().isQueries();
        this.failOnError = settings.isFailOnError();
        FlushMode flushMode = FlushMode.valueOf(hibernateSettings.getFlush().getMode().name());
        this.defaultFlushModeName = flushMode.name();
        this.defaultFlushMode = flushMode.getLevel();

        MultiTenancySettings multiTenancySettings = settings.getMultiTenancy();
        this.tenantResolver = multiTenancySettings.getTenantResolver();
        if(tenantResolver instanceof DatastoreAware) {
            ((DatastoreAware)tenantResolver).setDatastore(this);
        }
        this.multiTenantMode = multiTenancySettings.getMode();
    }

    protected AbstractHibernateDatastore(MappingContext mappingContext, SessionFactory sessionFactory, PropertyResolver config, ApplicationContext applicationContext, String dataSourceName) {
        super(mappingContext, config, (ConfigurableApplicationContext) applicationContext);
        this.connectionSources = new SingletonConnectionSources<>(new HibernateConnectionSource(dataSourceName, sessionFactory, null, null ), config);
        this.sessionFactory = sessionFactory;
        this.dataSourceName = dataSourceName;
        initializeConverters(mappingContext);
        if(applicationContext != null) {
            setApplicationContext(applicationContext);
        }

        osivReadOnly = config.getProperty(CONFIG_PROPERTY_OSIV_READONLY, Boolean.class, false);
        passReadOnlyToHibernate = config.getProperty(CONFIG_PROPERTY_PASS_READONLY_TO_HIBERNATE, Boolean.class, false);
        isCacheQueries = config.getProperty(CONFIG_PROPERTY_CACHE_QUERIES, Boolean.class, false);

        if( config.getProperty(SETTING_AUTO_FLUSH, Boolean.class, false) ) {
            this.defaultFlushModeName = FlushMode.AUTO.name();
            defaultFlushMode = FlushMode.AUTO.level;
        }
        else {
            FlushMode flushMode = config.getProperty(SETTING_FLUSH_MODE, FlushMode.class, FlushMode.COMMIT);
            this.defaultFlushModeName = flushMode.name();
            defaultFlushMode = flushMode.level;
        }
        failOnError = config.getProperty(SETTING_FAIL_ON_ERROR, Boolean.class, false);
        this.tenantResolver = new FixedTenantResolver();
        this.multiTenantMode = MultiTenancySettings.MultiTenancyMode.NONE;
    }

    public AbstractHibernateDatastore(MappingContext mappingContext, SessionFactory sessionFactory, PropertyResolver config) {
        this(mappingContext, sessionFactory, config, null, ConnectionSource.DEFAULT);
    }

    @Override
    public MultiTenancySettings.MultiTenancyMode getMultiTenancyMode() {
        return this.multiTenantMode;
    }

    @Override
    public Datastore getDatastoreForTenantId(Serializable tenantId) {
        if(multiTenantMode == MultiTenancySettings.MultiTenancyMode.SINGLE) {
            return getDatastoreForConnection(tenantId.toString());
        }
        else {
            return this;
        }
    }

    @Override
    public TenantResolver getTenantResolver() {
        return this.tenantResolver;
    }

    @Override
    public ConnectionSources<SessionFactory, HibernateConnectionSourceSettings> getConnectionSources() {
        return this.connectionSources;
    }

    /**
     * Obtain a child datastore for the given connection name
     *
     * @param connectionName The name of the connection
     * @return The child data store
     */
    public abstract  AbstractHibernateDatastore getDatastoreForConnection(String connectionName);

    public boolean isAutoFlush() {
        return defaultFlushMode == FlushMode.AUTO.level;
    }

    /**
     * @return Obtains the default flush mode level
     */
    public int getDefaultFlushMode() {
        return defaultFlushMode;
    }

    /**
     * @return The name of the default value flush
     */
    public String getDefaultFlushModeName() {
        return defaultFlushModeName;
    }

    public boolean isFailOnError() {
        return failOnError;
    }

    public boolean isOsivReadOnly() {
        return osivReadOnly;
    }

    public boolean isPassReadOnlyToHibernate() {
        return passReadOnlyToHibernate;
    }

    public boolean isCacheQueries() {
        return isCacheQueries;
    }

    /**
     * @return The Hibernate {@link SessionFactory} being used by this datastore instance
     */
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }


    // for testing
    public AbstractEventTriggeringInterceptor getEventTriggeringInterceptor() {
        return eventTriggeringInterceptor;
    }

    /**
     * @return The data source name being used
     */
    public String getDataSourceName() {
        return this.dataSourceName;
    }

    /**
     * Execute the given operation with the given flush mode
     *
     * @param flushMode
     * @param callable The callable
     */
    public abstract void withFlushMode(FlushMode flushMode, Callable<Boolean> callable);

    /**
     * We use a separate enum here because the classes differ between Hibernate 3 and 4
     *
     * @see org.hibernate.FlushMode
     */
    public enum FlushMode {
        MANUAL(0),
        COMMIT(5),
        AUTO(10),
        ALWAYS(20);

        private final int level;

        FlushMode(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    @Override
    public void destroy() throws Exception {
        super.destroy();
        AbstractHibernateGormInstanceApi.resetInsertActive();
        connectionSources.close();
    }

    @Override
    public void close() throws IOException {
        try {
            destroy();
        } catch (Exception e) {
            throw new IOException("Error closing hibernate datastore: " + e.getMessage(), e);
        }
    }

    /**
     * Obtains a hibernate template for the given flush mode
     *
     * @param flushMode The flush mode
     * @return The IHibernateTemplate
     */
    public abstract IHibernateTemplate getHibernateTemplate(int flushMode);

    public IHibernateTemplate getHibernateTemplate() {
        return getHibernateTemplate(defaultFlushMode);
    }

    /**
     * @return Opens a session
     */
    public abstract Session openSession();

    @Override
    public <T> T withSession(final Closure<T> callable) {
        Closure<T> multiTenantCallable = prepareMultiTenantClosure(callable);
        return getHibernateTemplate().execute(multiTenantCallable);
    }

    public <T> T withNewSession(final Closure<T> callable) {
        Closure<T> multiTenantCallable = prepareMultiTenantClosure(callable);
        return getHibernateTemplate().executeWithNewSession(multiTenantCallable);
    }

    @Override
    public <T1> T1 withNewSession(Serializable tenantId, Closure<T1> callable) {
        if(getMultiTenancyMode() == MultiTenancySettings.MultiTenancyMode.SINGLE) {
            return getDatastoreForConnection(tenantId.toString()).withNewSession(callable);
        }
        else {
            return withNewSession(callable);
        }
    }

    /**
     * Enable the tenant id filter for the given datastore and entity
     *
     */
    public void enableMultiTenancyFilter() {
        Serializable currentId = Tenants.currentId(getClass());
        if(!ConnectionSource.DEFAULT.equals(currentId)) {
            getHibernateTemplate()
                    .getSessionFactory()
                    .getCurrentSession()
                    .enableFilter(GormProperties.TENANT_IDENTITY)
                    .setParameter(GormProperties.TENANT_IDENTITY, currentId);
        }
    }

    /**
     * Disable the tenant id filter for the given datastore and entity
     */
    public void disableMultiTenancyFilter() {
        getHibernateTemplate()
            .getSessionFactory()
            .getCurrentSession()
            .disableFilter(GormProperties.TENANT_IDENTITY);
    }

    protected <T> Closure<T> prepareMultiTenantClosure(final Closure<T> callable) {
        final boolean isMultiTenant = getMultiTenancyMode() == MultiTenancySettings.MultiTenancyMode.MULTI;
        Closure<T> multiTenantCallable;
        if(isMultiTenant) {
            multiTenantCallable = new Closure<T>(this) {
                @Override
                public T call(Object... args) {
                    enableMultiTenancyFilter();
                    try {
                        return callable.call(args);
                    } finally {
                        disableMultiTenancyFilter();
                    }
                }
            };
        }
        else {
            multiTenantCallable = callable;
        }
        return multiTenantCallable;
    }
}
