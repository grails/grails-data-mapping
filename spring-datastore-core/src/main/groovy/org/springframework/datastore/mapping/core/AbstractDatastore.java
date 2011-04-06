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
package org.springframework.datastore.mapping.core;

import java.util.Collections;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.datastore.mapping.config.Property;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentProperty;
import org.springframework.datastore.mapping.model.PropertyMapping;
import org.springframework.datastore.mapping.model.types.BasicTypeConverterRegistrar;
import org.springframework.datastore.mapping.transactions.SessionHolder;
import org.springframework.datastore.mapping.validation.ValidatingEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Abstract Datastore implementation that deals with binding the Session to thread locale upon creation.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public abstract class AbstractDatastore implements Datastore {

    protected MappingContext mappingContext;
    protected Map<String, String> connectionDetails = Collections.emptyMap();
    private ApplicationContext applicationContext;

    public AbstractDatastore(MappingContext mappingContext) {
        this(mappingContext, null, null);
    }

    public AbstractDatastore(MappingContext mappingContext, Map<String, String> connectionDetails,
              ConfigurableApplicationContext ctx) {
        this.mappingContext = mappingContext;
        this.connectionDetails = connectionDetails != null ? connectionDetails : Collections.<String, String>emptyMap();
        setApplicationContext(ctx);
    }

    public void setApplicationContext(ConfigurableApplicationContext ctx) {
        applicationContext = ctx;
        if (ctx != null) {
            ctx.addApplicationListener(new ValidatingEventListener(this));
        }
    }

    public void setConnectionDetails(Map<String, String> connectionDetails) {
        this.connectionDetails = connectionDetails;
    }

    public Session connect() {
        return connect(connectionDetails);
    }

    public AbstractDatastore() {
    }

    public final Session connect(@SuppressWarnings("hiding") Map<String, String> connectionDetails) {
        SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(this);
        final Session session = createSession(connectionDetails);

        if (session == null) {
            return null;
        }

        if (sessionHolder != null) {
            sessionHolder.addSession(session);
        }
        else {
            try {
                TransactionSynchronizationManager.bindResource(this, new SessionHolder(session));
            } catch (IllegalStateException e) {
                // ignore session bound by another thread
            }
        }

        return session;
    }

    /**
     * Creates the native session
     *
     * @param connectionDetails The session details
     * @return The session object
     */
    protected abstract Session createSession(@SuppressWarnings("hiding") Map<String, String> connectionDetails);

    public final Session getCurrentSession() throws ConnectionNotFoundException {
        SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(this);

        Session connection = null;
        if (sessionHolder == null) {
            connection = connect(connectionDetails);
        }
        else {
            connection = sessionHolder.getSession();
        }
        return connection;
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
        Session connection = null;

        if (resourceMap != null && !resourceMap.isEmpty()) {
            for (Object key : resourceMap.keySet()) {
                if (datastoreClass.isInstance(key)) {
                    SessionHolder sessionHolder = (SessionHolder) resourceMap.get(key);
                    if (sessionHolder != null) {
                        connection = sessionHolder.getSession();
                    }
                }
            }
        }

        if (connection == null) {
            throw new ConnectionNotFoundException("Not datastore session found. Call Datastore.connect(..) before calling Datastore.getCurrentSession()");
        }
        return connection;
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

    protected void initializeConverters(@SuppressWarnings("hiding") MappingContext mappingContext) {
        final ConverterRegistry conversionService = mappingContext.getConverterRegistry();
        BasicTypeConverterRegistrar registrar = new BasicTypeConverterRegistrar();
        registrar.register(conversionService);
    }

    protected boolean isIndexed(PersistentProperty property) {
        PropertyMapping<Property> pm = property.getMapping();
        final Property keyValue = pm.getMappedForm();
        return keyValue != null && keyValue.isIndex();
    }
}
