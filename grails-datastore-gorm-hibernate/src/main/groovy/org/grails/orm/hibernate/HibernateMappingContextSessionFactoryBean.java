package org.grails.orm.hibernate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.grails.orm.hibernate.cfg.HibernateMappingContext;
import org.grails.orm.hibernate.cfg.HibernateMappingContextConfiguration;
import org.grails.orm.hibernate.cfg.Mapping;
import org.grails.orm.hibernate.support.ClosureEventTriggeringInterceptor;
import org.grails.orm.hibernate.transaction.HibernateJtaTransactionManagerAdapter;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cache.CacheException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.event.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;
import org.springframework.orm.hibernate3.LocalTransactionManagerLookup;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ReflectionUtils;

import javax.naming.NameNotFoundException;
import javax.transaction.TransactionManager;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * Configures a SessionFactory using a {@link org.grails.orm.hibernate.cfg.HibernateMappingContext} and a {@link org.grails.orm.hibernate.cfg.HibernateMappingContextConfiguration}
 *
 * @author Graeme Rocher
 * @since 5.0
 */
public class HibernateMappingContextSessionFactoryBean extends LocalSessionFactoryBean {
    protected static final Log LOG = LogFactory.getLog(HibernateMappingContextSessionFactoryBean.class);
    protected Class<? extends HibernateMappingContextConfiguration> configClass = HibernateMappingContextConfiguration.class;
    protected Class<?> currentSessionContextClass;
    protected HibernateEventListeners hibernateEventListeners;
    protected ApplicationContext applicationContext;
    protected boolean proxyIfReloadEnabled = true;
    protected String sessionFactoryBeanName = "sessionFactory";
    protected String dataSourceName = Mapping.DEFAULT_DATA_SOURCE;
    protected HibernateMappingContext hibernateMappingContext;
    protected PlatformTransactionManager transactionManager;

    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * @param proxyIfReloadEnabled Sets whether a proxy should be created if reload is enabled
     */
    public void setProxyIfReloadEnabled(boolean proxyIfReloadEnabled) {
        this.proxyIfReloadEnabled = proxyIfReloadEnabled;
    }

    /**
     * Sets class to be used for the Hibernate CurrentSessionContext.
     *
     * @param currentSessionContextClass An implementation of the CurrentSessionContext interface
     */
    public void setCurrentSessionContextClass(Class<?> currentSessionContextClass) {
        this.currentSessionContextClass = currentSessionContextClass;
    }

    public void setHibernateMappingContext(HibernateMappingContext hibernateMappingContext) {
        this.hibernateMappingContext = hibernateMappingContext;
    }

    /**
     * Sets the class to be used for Hibernate Configuration.
     * @param configClass A subclass of the Hibernate Configuration class
     */
    public void setConfigClass(Class<? extends HibernateMappingContextConfiguration> configClass) {
        this.configClass = configClass;
    }

    /**
     * Overrides default behaviour to allow for a configurable configuration class.
     */
    @Override
    protected Configuration newConfiguration() {
        HibernateMappingContextConfiguration grailsConfig = BeanUtils.instantiate(configClass);
        grailsConfig.setHibernateMappingContext(hibernateMappingContext);
        grailsConfig.setSessionFactoryBeanName(sessionFactoryBeanName);
        grailsConfig.setDataSourceName(dataSourceName);
        if (currentSessionContextClass != null) {
            grailsConfig.setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS, currentSessionContextClass.getName());
            // don't allow Spring's LocaalSessionFactoryBean to override setting
            setExposeTransactionAwareSessionFactory(false);
        }
        return grailsConfig;
    }

    @Override
    protected void postProcessMappings(Configuration config) throws HibernateException {
        super.postProcessMappings(config);
        if(requiresJtaTransactionManagerAdapter()) {
            configureJtaTransactionManagerAdapter(config);
        }
    }

    @Override
    protected SessionFactory buildSessionFactory() throws Exception {
        try {
            return super.buildSessionFactory();
        } finally {
            getConfigTimeTransactionManagerHolder().remove();
        }
    }

    protected boolean requiresJtaTransactionManagerAdapter() {
        return getConfigTimeTransactionManager()==null && getTransactionManager() != null;
    }

    /**
     * Configures adapter for adding transaction controlling hooks for supporting
     * Hibernate's org.hibernate.engine.transaction.Isolater class's interaction with transactions
     *
     * This is required when there is no real JTA transaction manager in use and Spring's
     * {@link TransactionAwareDataSourceProxy} is used.
     *
     * @param config
     */
    protected void configureJtaTransactionManagerAdapter(Configuration config) {
        getConfigTimeTransactionManagerHolder().set(new HibernateJtaTransactionManagerAdapter(getTransactionManager()));
        config.setProperty(Environment.TRANSACTION_MANAGER_STRATEGY, LocalTransactionManagerLookup.class.getName());
    }

    protected ThreadLocal<TransactionManager> getConfigTimeTransactionManagerHolder() {
        Field configTimeTransactionManagerHolderField = ReflectionUtils.findField(LocalSessionFactoryBean.class, "configTimeTransactionManagerHolder");
        ReflectionUtils.makeAccessible(configTimeTransactionManagerHolderField);
        @SuppressWarnings("unchecked")
        ThreadLocal<TransactionManager> configTimeTransactionManagerHolder=(ThreadLocal<TransactionManager>)ReflectionUtils.getField(configTimeTransactionManagerHolderField, null);
        return configTimeTransactionManagerHolder;
    }


    @Override
    protected SessionFactory newSessionFactory(Configuration configuration) throws HibernateException {
        try {

            SessionFactory sf = super.newSessionFactory(configuration);

            if (!proxyIfReloadEnabled) {
                return sf;
            }

            // if reloading is enabled in this environment then we need to use a SessionFactoryProxy instance
            SessionFactoryProxy sfp = new SessionFactoryProxy();
            String suffix = dataSourceName.equals(Mapping.DEFAULT_DATA_SOURCE) ? "" : '_' + dataSourceName;
            SessionFactoryHolder sessionFactoryHolder = applicationContext.getBean(
                    SessionFactoryHolder.BEAN_ID + suffix, SessionFactoryHolder.class);
            sessionFactoryHolder.setSessionFactory(sf);
            sfp.setApplicationContext(applicationContext);
            sfp.setCurrentSessionContextClass(currentSessionContextClass);
            sfp.setTargetBean(SessionFactoryHolder.BEAN_ID + suffix);
            sfp.afterPropertiesSet();
            return sfp;
        }
        catch (HibernateException e) {
            Throwable cause = e.getCause();
            if (isCacheConfigurationError(cause)) {
                LOG.error("There was an error configuring the Hibernate second level cache: " + getCauseMessage(e));
                LOG.error("This is normally due to one of two reasons. Either you have incorrectly specified the cache provider class name in [DataSource.groovy] or you do not have the cache provider on your classpath (eg. runtime (\"net.sf.ehcache:ehcache:1.6.1\"))");
                if (grails.util.Environment.isDevelopmentMode()) {
                    System.exit(1);
                }
            }
            throw e;
        }
    }

    protected String getCauseMessage(HibernateException e) {
        Throwable cause = e.getCause();
        if (cause instanceof InvocationTargetException) {
            cause = ((InvocationTargetException)cause).getTargetException();
        }
        return cause.getMessage();
    }

    protected boolean isCacheConfigurationError(Throwable cause) {
        if (cause instanceof InvocationTargetException) {
            cause = ((InvocationTargetException)cause).getTargetException();
        }
        return cause != null && (cause instanceof CacheException);
    }

    @Override
    public void destroy() throws HibernateException {
        try {
            super.destroy();
        } catch (HibernateException e) {
            if (e.getCause() instanceof NameNotFoundException) {
                LOG.debug(e.getCause().getMessage(), e);
            }
            else {
                throw e;
            }
        }
    }

    @Override
    protected void postProcessConfiguration(Configuration config) throws HibernateException {
        EventListeners listeners = config.getEventListeners();
        if (hibernateEventListeners != null && hibernateEventListeners.getListenerMap() != null) {
            Map<String,Object> listenerMap = hibernateEventListeners.getListenerMap();
            addNewListenerToConfiguration(config, "auto-flush", AutoFlushEventListener.class,
                    listeners.getAutoFlushEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "merge", MergeEventListener.class,
                    listeners.getMergeEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "create", PersistEventListener.class,
                    listeners.getPersistEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "create-onflush", PersistEventListener.class,
                    listeners.getPersistOnFlushEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "delete", DeleteEventListener.class,
                    listeners.getDeleteEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "dirty-check", DirtyCheckEventListener.class,
                    listeners.getDirtyCheckEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "evict", EvictEventListener.class,
                    listeners.getEvictEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "flush", FlushEventListener.class,
                    listeners.getFlushEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "flush-entity", FlushEntityEventListener.class,
                    listeners.getFlushEntityEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "load", LoadEventListener.class,
                    listeners.getLoadEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "load-collection", InitializeCollectionEventListener.class,
                    listeners.getInitializeCollectionEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "lock", LockEventListener.class,
                    listeners.getLockEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "refresh", RefreshEventListener.class,
                    listeners.getRefreshEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "replicate", ReplicateEventListener.class,
                    listeners.getReplicateEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "save-update", SaveOrUpdateEventListener.class,
                    listeners.getSaveOrUpdateEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "save", SaveOrUpdateEventListener.class,
                    listeners.getSaveEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "update", SaveOrUpdateEventListener.class,
                    listeners.getUpdateEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "pre-load", PreLoadEventListener.class,
                    listeners.getPreLoadEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "pre-update", PreUpdateEventListener.class,
                    listeners.getPreUpdateEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "pre-delete", PreDeleteEventListener.class,
                    listeners.getPreDeleteEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "pre-insert", PreInsertEventListener.class,
                    listeners.getPreInsertEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "pre-collection-recreate", PreCollectionRecreateEventListener.class,
                    listeners.getPreCollectionRecreateEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "pre-collection-remove", PreCollectionRemoveEventListener.class,
                    listeners.getPreCollectionRemoveEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "pre-collection-update", PreCollectionUpdateEventListener.class,
                    listeners.getPreCollectionUpdateEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "post-load", PostLoadEventListener.class,
                    listeners.getPostLoadEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "post-update", PostUpdateEventListener.class,
                    listeners.getPostUpdateEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "post-delete", PostDeleteEventListener.class,
                    listeners.getPostDeleteEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "post-insert", PostInsertEventListener.class,
                    listeners.getPostInsertEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "post-commit-update", PostUpdateEventListener.class,
                    listeners.getPostCommitUpdateEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "post-commit-delete", PostDeleteEventListener.class,
                    listeners.getPostCommitDeleteEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "post-commit-insert", PostInsertEventListener.class,
                    listeners.getPostCommitInsertEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "post-collection-recreate", PostCollectionRecreateEventListener.class,
                    listeners.getPostCollectionRecreateEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "post-collection-remove", PostCollectionRemoveEventListener.class,
                    listeners.getPostCollectionRemoveEventListeners(), listenerMap);
            addNewListenerToConfiguration(config, "post-collection-update", PostCollectionUpdateEventListener.class,
                    listeners.getPostCollectionUpdateEventListeners(), listenerMap);
        }
        // register workaround for GRAILS-8988 (do nullability checks for inserts in last PreInsertEventListener)
        ClosureEventTriggeringInterceptor.addNullabilityCheckerPreInsertEventListener(listeners);
    }

    @SuppressWarnings("unchecked")
    protected <T> void addNewListenerToConfiguration(final Configuration config, final String listenerType,
                                                     final Class<? extends T> klass, final T[] currentListeners, final Map<String,Object> newlistenerMap) {

        Object newListener = newlistenerMap.get(listenerType);
        if (newListener == null) return;

        if (currentListeners != null && currentListeners.length > 0) {
            T[] newListeners = (T[]) Array.newInstance(klass, currentListeners.length + 1);
            System.arraycopy(currentListeners, 0, newListeners, 0, currentListeners.length);
            newListeners[currentListeners.length] = (T)newListener;
            config.setListeners(listenerType, newListeners);
        }
        else {
            config.setListener(listenerType, newListener);
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setHibernateEventListeners(final HibernateEventListeners listeners) {
        hibernateEventListeners = listeners;
    }

    public void setSessionFactoryBeanName(String name) {
        sessionFactoryBeanName = name;
    }

    public void setDataSourceName(String name) {
        dataSourceName = name;
    }

}
