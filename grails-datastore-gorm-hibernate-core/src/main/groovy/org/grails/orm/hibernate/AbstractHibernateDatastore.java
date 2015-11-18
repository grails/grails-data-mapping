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

import org.grails.datastore.mapping.core.AbstractDatastore;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.orm.hibernate.cfg.Mapping;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertyResolver;

import java.util.concurrent.Callable;

/**
 * Datastore implementation that uses a Hibernate SessionFactory underneath.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public abstract class AbstractHibernateDatastore extends AbstractDatastore implements ApplicationContextAware {

    public static final String CONFIG_PROPERTY_CACHE_QUERIES = "grails.hibernate.cache.queries";
    public static final String CONFIG_PROPERTY_OSIV_READONLY = "grails.hibernate.osiv.readonly";
    public static final String CONFIG_PROPERTY_PASS_READONLY_TO_HIBERNATE = "grails.hibernate.pass.readonly";
    public static final String CONFIG_PROPERTY_AUTO_FLUSH = "grails.gorm.autoFlush";
    public static final String CONFIG_PROPERTY_FLUSH_MODE = "grails.gorm.flushMode";
    public static final String CONFIG_PROPERTY_FAIL_ON_ERROR = "grails.gorm.failOnError";
    public static final String CONFIG_PROPERTY_DEFAULT_MAPPING = "grails.gorm.default.mapping";
    protected final SessionFactory sessionFactory;
    protected final PropertyResolver config;
    protected AbstractEventTriggeringInterceptor eventTriggeringInterceptor;
    private final boolean osivReadOnly;
    private final boolean passReadOnlyToHibernate;
    private final boolean isCacheQueries;
    private final int defaultFlushMode;
    private final boolean failOnError;
    private final String dataSourceName;


    protected AbstractHibernateDatastore(MappingContext mappingContext, SessionFactory sessionFactory, PropertyResolver config, ApplicationContext applicationContext, String dataSourceName) {
        super(mappingContext, config, (ConfigurableApplicationContext) applicationContext);
        this.sessionFactory = sessionFactory;
        this.config = config;
        this.dataSourceName = dataSourceName;
        initializeConverters(mappingContext);
        if(applicationContext != null) {
            setApplicationContext(applicationContext);
        }

        osivReadOnly = config.getProperty(CONFIG_PROPERTY_OSIV_READONLY, Boolean.class, false);
        passReadOnlyToHibernate = config.getProperty(CONFIG_PROPERTY_PASS_READONLY_TO_HIBERNATE, Boolean.class, false);
        isCacheQueries = config.getProperty(CONFIG_PROPERTY_CACHE_QUERIES, Boolean.class, false);
        if( config.getProperty(CONFIG_PROPERTY_AUTO_FLUSH, Boolean.class, false) ) {
            defaultFlushMode = FlushMode.AUTO.level;
        }
        else {
            defaultFlushMode = config.getProperty(CONFIG_PROPERTY_FLUSH_MODE, Integer.class, FlushMode.COMMIT.level);
        }
        failOnError = config.getProperty(CONFIG_PROPERTY_FAIL_ON_ERROR, Boolean.class, false);
    }

    public boolean isAutoFlush() {
        return defaultFlushMode == FlushMode.AUTO.level;
    }

    public int getDefaultFlushMode() {
        return defaultFlushMode;
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

    public AbstractHibernateDatastore(MappingContext mappingContext, SessionFactory sessionFactory, PropertyResolver config) {
        this(mappingContext, sessionFactory, config, null, Mapping.DEFAULT_DATA_SOURCE);
    }

    /**
     * @return The Hibernate {@link SessionFactory} being used by this datastore instance
     */
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    @Override
    protected boolean registerValidationListener() {
        return false;
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

}
