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
import org.hibernate.SessionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.PropertyResolver;

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
    public static final String CONFIG_PROPERTY_FAIL_ON_ERROR = "grails.gorm.failOnError";
    public static final String CONFIG_PROPERTY_DEFAULT_MAPPING = "grails.gorm.default.mapping";
    protected final SessionFactory sessionFactory;
    protected final PropertyResolver config;
    protected AbstractEventTriggeringInterceptor eventTriggeringInterceptor;
    private final boolean osivReadOnly;
    private final boolean passReadOnlyToHibernate;
    private final boolean isCacheQueries;
    private final boolean autoFlush;
    private final boolean failOnError;


    protected AbstractHibernateDatastore(MappingContext mappingContext, SessionFactory sessionFactory, PropertyResolver config, ApplicationContext applicationContext) {
        super(mappingContext);
        this.sessionFactory = sessionFactory;
        this.config = config;
        initializeConverters(mappingContext);
        if(applicationContext != null) {
            setApplicationContext(applicationContext);
        }

        osivReadOnly = config.getProperty(CONFIG_PROPERTY_OSIV_READONLY, Boolean.class, false);
        passReadOnlyToHibernate = config.getProperty(CONFIG_PROPERTY_PASS_READONLY_TO_HIBERNATE, Boolean.class, false);
        isCacheQueries = config.getProperty(CONFIG_PROPERTY_CACHE_QUERIES, Boolean.class, false);
        autoFlush = config.getProperty(CONFIG_PROPERTY_AUTO_FLUSH, Boolean.class, false);
        failOnError = config.getProperty(CONFIG_PROPERTY_FAIL_ON_ERROR, Boolean.class, false);
    }

    public boolean isAutoFlush() {
        return autoFlush;
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
        this(mappingContext, sessionFactory, config, null);
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
}
