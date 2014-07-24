/*
 * Copyright 2003-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.orm.hibernate.support;

import grails.validation.ValidationException;
import groovy.lang.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsClassUtils;
import org.codehaus.groovy.grails.commons.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.orm.hibernate.AbstractHibernateGormInstanceApi;
import org.codehaus.groovy.grails.orm.hibernate.AbstractHibernateGormValidationApi;
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainBinder;
import org.codehaus.groovy.grails.orm.hibernate.cfg.Mapping;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.grails.datastore.gorm.support.BeforeValidateHelper.BeforeValidateEventTriggerCaller;
import org.grails.datastore.gorm.support.EventTriggerCaller;
import org.grails.datastore.mapping.engine.event.ValidationEvent;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.action.internal.EntityUpdateAction;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.ExecutableList;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.Errors;

/**
 * <p>Invokes closure events on domain entities such as beforeInsert, beforeUpdate and beforeDelete.
 *
 * <p>Also deals with auto time stamping of domain classes that have properties named 'lastUpdated' and/or 'dateCreated'.
 *
 * @author Lari Hotari
 * @since 1.3.5
 */
@SuppressWarnings({"rawtypes", "unchecked", "serial"})
public class ClosureEventListener implements SaveOrUpdateEventListener,
                                             PreLoadEventListener,
                                             PostLoadEventListener,
                                             PostInsertEventListener,
                                             PostUpdateEventListener,
                                             PostDeleteEventListener,
                                             PreDeleteEventListener,
                                             PreUpdateEventListener {

    private static final long serialVersionUID = 1;
    protected static final Log LOG = LogFactory.getLog(ClosureEventListener.class);

    EventTriggerCaller saveOrUpdateCaller;
    EventTriggerCaller beforeInsertCaller;
    EventTriggerCaller preLoadEventCaller;
    EventTriggerCaller postLoadEventListener;
    EventTriggerCaller postInsertEventListener;
    EventTriggerCaller postUpdateEventListener;
    EventTriggerCaller postDeleteEventListener;
    EventTriggerCaller preDeleteEventListener;
    EventTriggerCaller preUpdateEventListener;
    BeforeValidateEventTriggerCaller beforeValidateEventListener;
    boolean shouldTimestamp = false;
    MetaProperty dateCreatedProperty;
    MetaProperty lastUpdatedProperty;
    MetaClass domainMetaClass;
    boolean failOnErrorEnabled = false;
    MetaProperty errorsProperty;
    Map validateParams;
    MetaMethod validateMethod;

    public ClosureEventListener(Class<?> domainClazz, boolean failOnError, List failOnErrorPackages) {
        domainMetaClass = GroovySystem.getMetaClassRegistry().getMetaClass(domainClazz);
        dateCreatedProperty = domainMetaClass.getMetaProperty(GrailsDomainClassProperty.DATE_CREATED);
        lastUpdatedProperty = domainMetaClass.getMetaProperty(GrailsDomainClassProperty.LAST_UPDATED);
        if (dateCreatedProperty != null || lastUpdatedProperty != null) {
            Mapping m = new GrailsDomainBinder().getMapping(domainClazz);
            shouldTimestamp = m == null || m.isAutoTimestamp();
        }

        saveOrUpdateCaller = buildCaller(ClosureEventTriggeringInterceptor.ONLOAD_SAVE, domainClazz);
        beforeInsertCaller = buildCaller(ClosureEventTriggeringInterceptor.BEFORE_INSERT_EVENT, domainClazz);
        preLoadEventCaller = buildCaller(ClosureEventTriggeringInterceptor.ONLOAD_EVENT, domainClazz);
        if (preLoadEventCaller == null) {
            preLoadEventCaller = buildCaller(ClosureEventTriggeringInterceptor.BEFORE_LOAD_EVENT, domainClazz);
        }
        postLoadEventListener = buildCaller(ClosureEventTriggeringInterceptor.AFTER_LOAD_EVENT, domainClazz);
        postInsertEventListener = buildCaller(ClosureEventTriggeringInterceptor.AFTER_INSERT_EVENT, domainClazz);
        postUpdateEventListener = buildCaller(ClosureEventTriggeringInterceptor.AFTER_UPDATE_EVENT, domainClazz);
        postDeleteEventListener = buildCaller(ClosureEventTriggeringInterceptor.AFTER_DELETE_EVENT, domainClazz);
        preDeleteEventListener = buildCaller(ClosureEventTriggeringInterceptor.BEFORE_DELETE_EVENT, domainClazz);
        preUpdateEventListener = buildCaller(ClosureEventTriggeringInterceptor.BEFORE_UPDATE_EVENT, domainClazz);
        
        beforeValidateEventListener = new BeforeValidateEventTriggerCaller(domainClazz, domainMetaClass);

        if (failOnErrorPackages.size() > 0) {
            failOnErrorEnabled = GrailsClassUtils.isClassBelowPackage(domainClazz, failOnErrorPackages);
        } else {
            failOnErrorEnabled = failOnError;
        }

        validateParams = new HashMap();
        validateParams.put(AbstractHibernateGormValidationApi.ARGUMENT_DEEP_VALIDATE, Boolean.FALSE);

        errorsProperty = domainMetaClass.getMetaProperty(GrailsDomainClassProperty.ERRORS);

        validateMethod = domainMetaClass.getMetaMethod("validate",
                new Object[] { Map.class });
        
        try {
            actionQueueUpdatesField=ReflectionUtils.findField(ActionQueue.class, "updates");
            actionQueueUpdatesField.setAccessible(true);
            entityUpdateActionStateField=ReflectionUtils.findField(EntityUpdateAction.class, "state");
            entityUpdateActionStateField.setAccessible(true);
        } catch (Exception e) {
            // ignore
        }
    }
    
    private EventTriggerCaller buildCaller(String eventName, Class<?> domainClazz) {
        return EventTriggerCaller.buildCaller(eventName, domainClazz, domainMetaClass, null);
    }

    public void onSaveOrUpdate(SaveOrUpdateEvent event) throws HibernateException {
        // no-op, merely a hook for plugins to override
    }

    private Field actionQueueUpdatesField;
    private Field entityUpdateActionStateField;
    
    private void synchronizePersisterState(AbstractPreDatabaseOperationEvent event, Object[] state) {
        Object entity = event.getEntity();
        EntityPersister persister = event.getPersister();
        
        String[] propertyNames = persister.getPropertyNames();
        HashMap<Integer, Object> changedState=new HashMap<Integer, Object>();
        for (int i = 0; i < propertyNames.length; i++) {
            String p = propertyNames[i];
            MetaProperty metaProperty = domainMetaClass.getMetaProperty(p);
            if (ClosureEventTriggeringInterceptor.IGNORED.contains(p) || metaProperty == null) {
                continue;
            }
            Object value = metaProperty.getProperty(entity);
            if(state[i] != value) {
                changedState.put(i, value);
            }
            state[i] = value;
            persister.setPropertyValue(entity, i, value);
        }
        
        synchronizeEntityUpdateActionState(event, entity, changedState);
    }

    protected void synchronizeEntityUpdateActionState(AbstractPreDatabaseOperationEvent event, Object entity,
            HashMap<Integer, Object> changedState) {
        if(actionQueueUpdatesField != null && event instanceof PreInsertEvent && changedState.size() > 0) {
            try {
                ExecutableList<EntityUpdateAction> updates = (ExecutableList<EntityUpdateAction>)actionQueueUpdatesField.get(event.getSource().getActionQueue());
                if(updates != null) {
                    for (EntityUpdateAction updateAction : updates) {
                        if(updateAction.getInstance() == entity) {
                            Object[] updateState = (Object[])entityUpdateActionStateField.get(updateAction);
                            if (updateState != null) {
                                for(Map.Entry<Integer, Object> entry : changedState.entrySet()) {
                                    updateState[entry.getKey()] = entry.getValue();
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                LOG.warn("Exception in synchronizeEntityUpdateActionState", e);
            }
        }
     }

    public void onPreLoad(final PreLoadEvent event) {
        if (preLoadEventCaller == null) {
            return;
        }

        doWithManualSession(event, new Closure(this) {
            @Override
            public Object call() {
                preLoadEventCaller.call(event.getEntity());
                return null;
            }
        });
    }

    public void onPostLoad(final PostLoadEvent event) {
        if (postLoadEventListener == null) {
            return;
        }

       doWithManualSession(event, new Closure(this) {
            @Override
            public Object call() {
                postLoadEventListener.call(event.getEntity());
                return null;
            }
        });
    }

    public void onPostInsert(PostInsertEvent event) {
        final Object entity = event.getEntity();
        AbstractHibernateGormInstanceApi.clearDisabledValidations(entity);
        if (postInsertEventListener == null) {
            return;
        }

        doWithManualSession(event, new Closure(this) {
            @Override
            public Object call() {
                postInsertEventListener.call(entity);
                return null;
            }
        });
    }

    @Override
    public boolean requiresPostCommitHanding(EntityPersister persister) {
        return false;
    }

    public void onPostUpdate(PostUpdateEvent event) {
        final Object entity = event.getEntity();
        AbstractHibernateGormInstanceApi.clearDisabledValidations(entity);
        if (postUpdateEventListener == null) {
            return;
        }

        doWithManualSession(event, new Closure(this) {
            @Override
            public Object call() {
                postUpdateEventListener.call(entity);
                return null;
            }
        });
    }

    public void onPostDelete(PostDeleteEvent event) {
        final Object entity = event.getEntity();
        AbstractHibernateGormInstanceApi.clearDisabledValidations(entity);
        if (postDeleteEventListener == null) {
            return;
        }

        doWithManualSession(event, new Closure(this) {
            @Override
            public Object call() {
                postDeleteEventListener.call(entity);
                return null;
            }
        });
    }

    public boolean onPreDelete(final PreDeleteEvent event) {
        if (preDeleteEventListener == null) {
            return false;
        }

        return doWithManualSession(event, new Closure<Boolean>(this) {
            @Override
            public Boolean call() {
                return preDeleteEventListener.call(event.getEntity());
            }
        });
    }

    public boolean onPreUpdate(final PreUpdateEvent event) {
        return doWithManualSession(event, new Closure<Boolean>(this) {
            @Override
            public Boolean call() {
                Object entity = event.getEntity();
                boolean evict = false;
                if (preUpdateEventListener != null) {
                    evict = preUpdateEventListener.call(entity);
                    synchronizePersisterState(event, event.getState());
                }
                if (shouldTimestamp) {
                    long time = System.currentTimeMillis();
                    if (dateCreatedProperty != null && dateCreatedProperty.getProperty(entity)==null) {
                        Object now = applyTimestamp(entity, dateCreatedProperty,  time);
                        String[] propertyNames = event.getPersister().getPropertyNames();
                        event.getState()[ Arrays.asList(propertyNames).indexOf(GrailsDomainClassProperty.DATE_CREATED) ] = now;

                    }
                    if (lastUpdatedProperty != null) {
                        Object now = applyTimestamp(entity, lastUpdatedProperty,  time);
                        String[] propertyNames = event.getPersister().getPropertyNames();
                        event.getState()[ Arrays.asList(propertyNames).indexOf( GrailsDomainClassProperty.LAST_UPDATED) ] = now;
                    }
                }
                if (!AbstractHibernateGormInstanceApi.isAutoValidationDisabled(entity)
                        && !DefaultTypeTransformation.castToBoolean(validateMethod.invoke(entity, new Object[] { validateParams }))) {
                    evict = true;
                    if (failOnErrorEnabled) {
                        Errors errors = (Errors) errorsProperty.getProperty(entity);
                        throw new ValidationException("Validation error whilst flushing entity [" + entity.getClass().getName()
                                + "]", errors);
                    }
                }
                return evict;
            }
        });
    }

    private <T> T doWithManualSession(AbstractEvent event, Closure<T> callable) {
        Session session = event.getSession();
        FlushMode current = session.getFlushMode();
        try {
           session.setFlushMode(FlushMode.MANUAL);
           return callable.call();
        } finally {
            session.setFlushMode(current);
        }
    }

    public boolean onPreInsert(final PreInsertEvent event) {
        return doWithManualSession(event, new Closure<Boolean>(this) {
            @Override
            public Boolean call() {
                Object entity = event.getEntity();
                boolean synchronizeState = false;
                if (beforeInsertCaller != null) {
                    if (beforeInsertCaller.call(entity)) {
                        return true;
                    }
                    synchronizeState = true;
                }
                if (shouldTimestamp) {
                    long time = System.currentTimeMillis();
                    if (dateCreatedProperty != null) {
                        applyTimestamp(entity, dateCreatedProperty,  time);
                        synchronizeState = true;
                    }
                    if (lastUpdatedProperty != null) {
                        applyTimestamp(entity, lastUpdatedProperty,  time);
                        synchronizeState = true;
                    }
                }

                if (synchronizeState) {
                    synchronizePersisterState(event, event.getState());
                }

                boolean evict = false;
                if (!AbstractHibernateGormInstanceApi.isAutoValidationDisabled(entity)
                        && !DefaultTypeTransformation.castToBoolean(validateMethod.invoke(entity,
                                new Object[] { validateParams }))) {
                    evict = true;
                    if (failOnErrorEnabled) {
                        Errors errors = (Errors) errorsProperty.getProperty(entity);
                        throw new ValidationException("Validation error whilst flushing entity [" + entity.getClass().getName()
                                + "]", errors);
                    }
                }
                return evict;
            }

        });
    }

    protected Object applyTimestamp(Object entity, MetaProperty property, long time) {
        Object now = DefaultGroovyMethods.newInstance(property.getType(), new Object[] { time });
        property.setProperty(entity, now);
        return now;
    }

    public void onValidate(ValidationEvent event) {
        beforeValidateEventListener.call(event.getEntityObject(), event.getValidatedFields());
    }
}
