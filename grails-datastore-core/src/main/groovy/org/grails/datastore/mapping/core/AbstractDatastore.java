/* Copyright (C) 2010 SpringSource
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
package org.grails.datastore.mapping.core;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.grails.datastore.mapping.cache.TPCacheAdapterRepository;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.proxy.ProxyFactory;
import org.grails.datastore.mapping.reflect.FastClassData;
import org.grails.datastore.mapping.reflect.FastEntityAccess;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.grails.datastore.mapping.config.Property;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.grails.datastore.mapping.model.PropertyMapping;
import org.grails.datastore.mapping.model.types.BasicTypeConverterRegistrar;
import org.grails.datastore.mapping.transactions.SessionHolder;
import org.grails.datastore.mapping.validation.ValidatingEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;

/**
 * Abstract Datastore implementation that deals with binding the Session to thread locale upon creation.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AbstractDatastore implements Datastore, StatelessDatastore, DisposableBean {


    private ApplicationContext applicationContext;

    private static final SoftThreadLocalMap ERRORS_MAP = new SoftThreadLocalMap();
    private static final SoftThreadLocalMap VALIDATE_MAP = new SoftThreadLocalMap();

    protected MappingContext mappingContext;
    protected Map<String, String> connectionDetails = Collections.emptyMap();
    protected TPCacheAdapterRepository cacheAdapterRepository;

    public AbstractDatastore() {}

    public AbstractDatastore(MappingContext mappingContext) {
        this(mappingContext, null, null);
    }

    public AbstractDatastore(MappingContext mappingContext, Map<String, String> connectionDetails,
              ConfigurableApplicationContext ctx) {
        this(mappingContext, connectionDetails,  ctx, null);
    }

    public AbstractDatastore(MappingContext mappingContext, Map<String, String> connectionDetails,
              ConfigurableApplicationContext ctx, TPCacheAdapterRepository cacheAdapterRepository) {
        this.mappingContext = mappingContext;
        this.connectionDetails = connectionDetails != null ? connectionDetails : Collections.<String, String>emptyMap();
        setApplicationContext(ctx);
        this.cacheAdapterRepository = cacheAdapterRepository;
    }

    public void destroy() throws Exception {
        ERRORS_MAP.remove();
        VALIDATE_MAP.remove();
    }

    public void setApplicationContext(ApplicationContext ctx) {
        applicationContext = ctx;
        if (ctx != null && registerValidationListener()) {
            Assert.isInstanceOf(ConfigurableApplicationContext.class, applicationContext,
                    "ApplicationContext must be an instanceof ConfigurableApplicationContext");
            ((ConfigurableApplicationContext)ctx).addApplicationListener(new ValidatingEventListener(this));
        }
    }

    protected boolean registerValidationListener() {
        return true;
    }

    public void setConnectionDetails(Map<String, String> connectionDetails) {
        this.connectionDetails = connectionDetails;
    }

    public Session connect() {
        return connect(connectionDetails);
    }

    public final Session connect(Map<String, String> connDetails) {
        Session session = createSession(connDetails);
        publishSessionCreationEvent(session);
        return session;
    }

    private void publishSessionCreationEvent(Session session) {
        ApplicationEventPublisher applicationEventPublisher = getApplicationEventPublisher();
        if(applicationEventPublisher != null) {
            applicationEventPublisher.publishEvent(new SessionCreationEvent(session));
        }
    }

    @Override
    public Session connectStateless() {
        Session session = createStatelessSession(connectionDetails);
        publishSessionCreationEvent(session);
        return session;
    }


    /**
     * Obtains a {@link FastClassData} instance for the given entity
     *
     * @param entity The entity
     * @return The class data
     */
    public FastClassData getFastClassData(PersistentEntity entity) {
        return getMappingContext().getFastClassData(entity);
    }

    /**
     * Creates the native session
     *
     * @param connectionDetails The session details
     * @return The session object
     */
    protected abstract Session createSession(Map<String, String> connectionDetails);

    /**
     * Creates the native stateless session
     *
     * @param connectionDetails The session details
     * @return The session object
     */
    protected Session createStatelessSession(Map<String, String> connectionDetails) {
        return createSession(connectionDetails);
    }


    public Session getCurrentSession() throws ConnectionNotFoundException {
        return DatastoreUtils.doGetSession(this, false);
    }

    public boolean hasCurrentSession() {
        return TransactionSynchronizationManager.hasResource(this);
    }

    /**
     * Static way to retrieve the session
     * @return The session instance
     * @throws ConnectionNotFoundException If no session has been created
     */
    public static Session retrieveSession() throws ConnectionNotFoundException {
        return retrieveSession(Datastore.class);
    }

    /**
     * Static way to retrieve the session
     * @param datastoreClass The type of datastore
     * @return The session instance
     * @throws ConnectionNotFoundException If no session has been created
     */
    public static Session retrieveSession(Class datastoreClass) throws ConnectionNotFoundException {
        final Map<Object, Object> resourceMap = TransactionSynchronizationManager.getResourceMap();
        Session session = null;

        if (resourceMap != null && !resourceMap.isEmpty()) {
            for (Object key : resourceMap.keySet()) {
                if (datastoreClass.isInstance(key)) {
                    SessionHolder sessionHolder = (SessionHolder) resourceMap.get(key);
                    if (sessionHolder != null) {
                        session = sessionHolder.getSession();
                    }
                }
            }
        }

        if (session == null) {
            throw new ConnectionNotFoundException("No datastore session found. Call Datastore.connect(..) before calling Datastore.getCurrentSession()");
        }
        return session;
    }

    public MappingContext getMappingContext() {
        return mappingContext;
    }

    public ConfigurableApplicationContext getApplicationContext() {
        return (ConfigurableApplicationContext)applicationContext;
    }

    public ApplicationEventPublisher getApplicationEventPublisher() {
        return getApplicationContext();
    }

    public Errors getObjectErrors(final Object o) {
        return getValidationErrorsMap().get(System.identityHashCode(o));
    }

    public void setObjectErrors(Object object, Errors errors) {
        getValidationErrorsMap().put(System.identityHashCode(object), errors);
    }

    public void setSkipValidation(final Object o, final boolean skip) {
        VALIDATE_MAP.get().put(System.identityHashCode(o), skip);
    }

    public boolean skipValidation(final Object o) {
        final Object skipValidation = VALIDATE_MAP.get().get(System.identityHashCode(o));
        return skipValidation instanceof Boolean && (Boolean) skipValidation;
    }

    public static Map<Object, Errors> getValidationErrorsMap() {
        return ERRORS_MAP.get();
    }

    public static Map<Object, Boolean> getValidationSkipMap() {
        return VALIDATE_MAP.get();
    }

    protected void initializeConverters(MappingContext mappingContext) {
        final ConverterRegistry conversionService = mappingContext.getConverterRegistry();
        BasicTypeConverterRegistrar registrar = new BasicTypeConverterRegistrar();
        registrar.register(conversionService);
    }

    protected boolean isIndexed(PersistentProperty property) {
        PropertyMapping<Property> pm = property.getMapping();
        final Property keyValue = pm.getMappedForm();
        return keyValue != null && keyValue.isIndex();
    }

    public boolean isSchemaless() {
        return false;
    }

}
