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
package org.grails.datastore.mapping.model;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.grails.datastore.mapping.model.types.conversion.DefaultConversionService;
import org.grails.datastore.mapping.proxy.JavassistProxyFactory;
import org.grails.datastore.mapping.proxy.ProxyFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Validator;

/**
 * Abstract implementation of the MappingContext interface.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractMappingContext implements MappingContext {

    public static final String GROOVY_PROXY_FACTORY_NAME = "org.grails.datastore.gorm.proxy.GroovyProxyFactory";
    public static final String JAVASIST_PROXY_FACTORY = "javassist.util.proxy.ProxyFactory";
    protected Collection<PersistentEntity> persistentEntities = new ConcurrentLinkedQueue<PersistentEntity>();
    protected Map<String,PersistentEntity>  persistentEntitiesByName = new ConcurrentHashMap<String,PersistentEntity>();
    protected Map<PersistentEntity,Map<String,PersistentEntity>>  persistentEntitiesByDiscriminator = new ConcurrentHashMap<PersistentEntity,Map<String,PersistentEntity>>();
    protected Map<PersistentEntity,Validator>  entityValidators = new ConcurrentHashMap<PersistentEntity, Validator>();
    protected Collection<Listener> eventListeners = new ConcurrentLinkedQueue<Listener>();
    protected GenericConversionService conversionService = new DefaultConversionService();
    protected ProxyFactory proxyFactory;

    public ConversionService getConversionService() {
        return conversionService;
    }

    public ConverterRegistry getConverterRegistry() {
        return conversionService;
    }

    public abstract MappingFactory getMappingFactory();

    public ProxyFactory getProxyFactory() {
        if (this.proxyFactory == null) {
            ClassLoader classLoader = AbstractMappingContext.class.getClassLoader();
            if (ClassUtils.isPresent(JAVASIST_PROXY_FACTORY, classLoader)) {
                proxyFactory = DefaultProxyFactoryCreator.create();
            }
            else if (ClassUtils.isPresent(GROOVY_PROXY_FACTORY_NAME, classLoader)) {
                try {
                    proxyFactory = (ProxyFactory) BeanUtils.instantiate(ClassUtils.forName(GROOVY_PROXY_FACTORY_NAME, classLoader));
                } catch (ClassNotFoundException e) {
                    proxyFactory = DefaultProxyFactoryCreator.create();
                }
            }
            else {
                proxyFactory = DefaultProxyFactoryCreator.create();
            }
        }
        return proxyFactory;
    }

    public void addMappingContextListener(Listener listener) {
        if (listener != null)
            eventListeners.add(listener);
    }

    public void setProxyFactory(ProxyFactory factory) {
        if (factory != null) {
            this.proxyFactory = factory;
        }
    }

    private static class DefaultProxyFactoryCreator {
        public static ProxyFactory create() {
            return new JavassistProxyFactory();
        }
    }

    public void addTypeConverter(Converter converter) {
        conversionService.addConverter(converter);
    }

    public Validator getEntityValidator(PersistentEntity entity) {
        if (entity != null) {
            return entityValidators.get(entity);
        }
        return null;
    }

    public void addEntityValidator(PersistentEntity entity, Validator validator) {
        if (entity != null && validator != null) {
            entityValidators.put(entity, validator);
        }
    }

    public PersistentEntity addExternalPersistentEntity(Class javaClass) {
        Assert.notNull(javaClass, "PersistentEntity class cannot be null");

        PersistentEntity entity = persistentEntitiesByName.get(javaClass.getName());

        if (entity == null) {
            entity = addPersistentEntityInternal(javaClass, true);
        }

        return entity;
    }

    /**
     * Adds a PersistentEntity instance
     *
     * @param javaClass The Java class representing the entity
     * @param override  Whether to override an existing entity
     * @return The PersistentEntity instance
     */
    public PersistentEntity addPersistentEntity(Class javaClass, boolean override) {
        Assert.notNull(javaClass, "PersistentEntity class cannot be null");
        if (override) {
            return addPersistentEntityInternal(javaClass, false);
        }
        return addPersistentEntity(javaClass);
    }

    public final PersistentEntity addPersistentEntity(Class javaClass) {
        Assert.notNull(javaClass, "PersistentEntity class cannot be null");

        PersistentEntity entity = persistentEntitiesByName.get(javaClass.getName());

        if (entity == null) {
            entity = addPersistentEntityInternal(javaClass, false);
        }

        return entity;
    }

    private PersistentEntity addPersistentEntityInternal(Class javaClass, boolean isExternal) {
        PersistentEntity entity = createPersistentEntity(javaClass);
        if (entity == null) {
            return null;
        }
        entity.setExternal(isExternal);

        persistentEntities.remove(entity); persistentEntities.add(entity);
        persistentEntitiesByName.put(entity.getName(), entity);

        try {
            entity.initialize();
        }
        catch(IllegalMappingException x) {
            persistentEntities.remove(entity);
            persistentEntitiesByName.remove(entity.getName());
            throw x;
        }

        if (!entity.isRoot()) {
            PersistentEntity root = entity.getRootEntity();
            Map<String, PersistentEntity> children = persistentEntitiesByDiscriminator.get(root);
            if (children == null) {
                children = new ConcurrentHashMap<String,PersistentEntity>();
                persistentEntitiesByDiscriminator.put(root, children);
            }
            children.put(entity.getDiscriminator(), entity);
        }
        for (Listener eventListener : eventListeners) {
            eventListener.persistentEntityAdded(entity);
        }
        return entity;
    }

    /**
     * Returns true if the given entity is in an inheritance hierarchy
     *
     * @param entity The entity
     * @return True if it is
     */
    public boolean isInInheritanceHierarchy(PersistentEntity entity) {
        if (entity != null) {
            if (!entity.isRoot()) return true;
            PersistentEntity rootEntity = entity.getRootEntity();
            final Map<String, PersistentEntity> children = persistentEntitiesByDiscriminator.get(rootEntity);
            if (children != null && !children.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public PersistentEntity getChildEntityByDiscriminator(PersistentEntity root, String discriminator) {
        final Map<String, PersistentEntity> children = persistentEntitiesByDiscriminator.get(root);
        if (children != null) {
            return children.get(discriminator);
        }
        return null;
    }

    protected abstract PersistentEntity createPersistentEntity(Class javaClass);

    public PersistentEntity createEmbeddedEntity(Class type) {
        EmbeddedPersistentEntity embedded = new EmbeddedPersistentEntity(type, this);
        embedded.initialize();
        return embedded;
    }

    public Collection<PersistentEntity> getPersistentEntities() {
        return persistentEntities;
    }

    public boolean isPersistentEntity(Class type) {
        return type != null && getPersistentEntity(type.getName()) != null;

    }

    public boolean isPersistentEntity(Object value) {
        return value != null && isPersistentEntity(value.getClass());
    }

    public PersistentEntity getPersistentEntity(String name) {
        final int proxyIndicator = name.indexOf("_$$_");
        if (proxyIndicator > -1) {
            name = name.substring(0, proxyIndicator);
        }

        return persistentEntitiesByName.get(name);
    }
}
