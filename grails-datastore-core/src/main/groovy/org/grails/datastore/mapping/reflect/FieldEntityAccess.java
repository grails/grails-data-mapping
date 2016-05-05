package org.grails.datastore.mapping.reflect;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.transform.trait.Traits;
import org.grails.datastore.mapping.engine.EntityAccess;
import org.grails.datastore.mapping.model.AbstractPersistentEntity;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.PersistentProperty;
import org.springframework.cglib.reflect.FastClass;
import org.springframework.cglib.reflect.FastMethod;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Uses field reflection or CGlib to improve performance
 *
 * @author Graeme Rocher
 * @since 5.0
 */
public class FieldEntityAccess implements EntityAccess {

    private static Map<String, EntityReflector> REFLECTORS  = new ConcurrentHashMap<String, EntityReflector>();

    private final PersistentEntity persistentEntity;
    private final Object entity;
    private final ConversionService conversionService;
    private final EntityReflector reflector;

    public FieldEntityAccess(PersistentEntity persistentEntity, Object entity, ConversionService conversionService) {
        this.persistentEntity = persistentEntity;
        this.entity = entity;
        this.conversionService = conversionService;
        this.reflector = getOrIntializeReflector(persistentEntity);
    }

    public static void clearReflectors() {
        REFLECTORS.clear();
    }

    public static EntityReflector getOrIntializeReflector(PersistentEntity persistentEntity) {
        String entityName = persistentEntity.getName();
        EntityReflector entityReflector = REFLECTORS.get(entityName);
        if(entityReflector == null) {
            entityReflector = new FieldEntityReflector(persistentEntity);
            REFLECTORS.put(entityName, entityReflector);
        }
        return entityReflector;
    }

    public static EntityReflector getReflector(String name) {
        return REFLECTORS.get(name);
    }

    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public Object getProperty(String name) {
        return reflector.getProperty(entity, name);
    }

    @Override
    public Object getPropertyValue(String name) {
        return getProperty(name);
    }

    @Override
    public Class getPropertyType(String name) {
        PersistentProperty property = persistentEntity.getPropertyByName(name);
        if(property != null) {
            return property.getType();
        }
        return null;
    }

    @Override
    public void setProperty(String name, Object value) {
        FieldEntityReflector.PropertyWriter writer = reflector.getPropertyWriter(name);
        writer.write(entity, conversionService.convert(value, writer.propertyType()));
    }

    @Override
    public Object getIdentifier() {
        return reflector.getIdentifier(entity);
    }

    @Override
    public void setIdentifier(Object id) {
        reflector.setIdentifier(entity, conversionService.convert(id, reflector.identifierType()));
    }

    @Override
    public void setIdentifierNoConversion(Object id) {
        reflector.setIdentifier(entity, id);
    }

    @Override
    public String getIdentifierName() {
        return reflector.getIdentifierName();
    }

    @Override
    public PersistentEntity getPersistentEntity() {
        return persistentEntity;
    }

    @Override
    public void refresh() {
        // no-op
    }

    @Override
    public void setPropertyNoConversion(String name, Object value) {
        reflector.setProperty(entity, name, value);
    }



    static class FieldEntityReflector implements EntityReflector {
        final PersistentEntity entity;
        final PropertyReader[] readers;
        final PropertyWriter[] writers;
        final PropertyReader identifierReader;
        final PropertyWriter identifierWriter;
        final String identifierName;
        final Class identifierType;
        final Map<String, PropertyReader> readerMap = new HashMap<String, PropertyReader>();
        final Map<String, PropertyWriter> writerMap = new HashMap<String, PropertyWriter>();
        final FastClass fastClass;

        public FieldEntityReflector(PersistentEntity entity) {
            this.entity = entity;
            Class javaClass = entity.getJavaClass();
            PersistentProperty identity = entity.getIdentity();
            ClassPropertyFetcher cpf = ClassPropertyFetcher.forClass(entity.getJavaClass());
            fastClass = FastClass.create(entity.getJavaClass());
            if(identity != null) {
                String identityName = identity.getName();
                this.identifierName = identityName;
                this.identifierType = identity.getType();
                Field field = ReflectionUtils.findField(javaClass, identityName);
                if(field != null) {
                    ReflectionUtils.makeAccessible(field);
                    identifierReader = new FieldReader(field);
                    identifierWriter = new FieldWriter(field);
                }
                else {
                    PropertyDescriptor descriptor = cpf.getPropertyDescriptor(identityName);
                    Method readMethod = descriptor.getReadMethod();

                    Traits.TraitBridge traitBridge = readMethod.getAnnotation(Traits.TraitBridge.class);
                    if(traitBridge != null) {
                        String traitFieldName = traitBridge.traitClass().getName().replace('.','_') + "__" + identityName;
                        field = ReflectionUtils.findField(javaClass, traitFieldName);
                        if(field != null) {
                            ReflectionUtils.makeAccessible(field);
                            identifierReader = new FieldReader(field);
                            identifierWriter = new FieldWriter(field);
                        }
                        else {
                            Method writeMethod = descriptor.getWriteMethod();

                            identifierReader = new FastMethodReader(fastClass.getMethod(readMethod));
                            identifierWriter = new FastMethodWriter(fastClass.getMethod(writeMethod), descriptor.getPropertyType());
                        }
                    }
                    else {
                        Method writeMethod = descriptor.getWriteMethod();

                        identifierReader = new FastMethodReader(fastClass.getMethod(readMethod));
                        identifierWriter = new FastMethodWriter(fastClass.getMethod(writeMethod), descriptor.getPropertyType());
                    }
                }

                readerMap.put(identifierName, identifierReader);
                writerMap.put(identifierName, identifierWriter);
            }
            else {
                this.identifierName = null;
                this.identifierReader = null;
                this.identifierWriter = null;
                this.identifierType = null;
            }
            PersistentProperty[] composite = ((AbstractPersistentEntity) entity).getCompositeIdentity();
            if(composite != null) {
                for (PersistentProperty property : composite) {
                    String propertyName = property.getName();
                    Field field = ReflectionUtils.findField(javaClass, propertyName);
                    if (field != null) {
                        ReflectionUtils.makeAccessible(field);
                        FieldReader reader = new FieldReader(field);
                        readerMap.put(propertyName, reader);
                        FieldWriter writer = new FieldWriter(field);
                        writerMap.put(propertyName, writer);
                    } else {

                        PropertyDescriptor descriptor = cpf.getPropertyDescriptor(propertyName);
                        Method readMethod = descriptor.getReadMethod();
                        Method writeMethod = descriptor.getWriteMethod();
                        FastMethodReader reader = new FastMethodReader(fastClass.getMethod(readMethod));
                        readerMap.put(propertyName, reader);
                        FastMethodWriter writer = new FastMethodWriter(fastClass.getMethod(writeMethod), descriptor.getPropertyType());
                        writerMap.put(propertyName, writer);
                    }
                }
            }
            List<PersistentProperty> properties = entity.getPersistentProperties();
            readers = new PropertyReader[properties.size()];
            writers = new PropertyWriter[properties.size()];
            for (int i = 0; i < properties.size(); i++) {
                PersistentProperty property = properties.get(i);

                String propertyName = property.getName();
                Field field = ReflectionUtils.findField(javaClass, propertyName);
                if(field != null) {
                    ReflectionUtils.makeAccessible(field);
                    FieldReader reader = new FieldReader(field);
                    readers[i] = reader;
                    readerMap.put(propertyName, reader);
                    FieldWriter writer = new FieldWriter(field);
                    writers[i] = writer;
                    writerMap.put(propertyName, writer);
                }
                else {

                    PropertyDescriptor descriptor = cpf.getPropertyDescriptor(propertyName);
                    Method readMethod = descriptor.getReadMethod();
                    Method writeMethod = descriptor.getWriteMethod();
                    FastMethodReader reader = new FastMethodReader(fastClass.getMethod(readMethod));
                    readers[i] = reader;
                    readerMap.put(propertyName, reader);
                    FastMethodWriter writer = new FastMethodWriter(fastClass.getMethod(writeMethod), descriptor.getPropertyType());
                    writers[i] = writer;
                    writerMap.put(propertyName, writer);

                }
            }
        }

        @Override
        public PersistentEntity getPersitentEntity() {
            return this.entity;
        }

        @Override
        public FastClass fastClass() {
            return fastClass;
        }

        @Override
        public PropertyReader getPropertyReader(String name) {
            final PropertyReader reader = readerMap.get(name);
            if(reader != null) {
                return reader;
            }
            throw new IllegalArgumentException("Property ["+name+"] is not a valid property of " + entity.getJavaClass());

        }

        @Override
        public PropertyWriter getPropertyWriter(String name) {
            final PropertyWriter writer = writerMap.get(name);
            if(writer != null) {
                return writer;
            }
            else {
                throw new IllegalArgumentException("Property ["+name+"] is not a valid property of " + entity.getJavaClass());
            }
        }

        @Override
        public Object getProperty(Object object, String name) {
            return getPropertyReader(name).read(object);
        }

        @Override
        public void setProperty(Object object, String name, Object value) {
            getPropertyWriter(name).write(object, value);
        }

        @Override
        public Class identifierType() {
            return identifierType;
        }

        @Override
        public Serializable getIdentifier(Object object) {
            if(identifierReader != null) {
                return (Serializable) identifierReader.read(object);
            }
            return null;
        }

        @Override
        public void setIdentifier(Object object, Object value) {
            if(identifierWriter != null) {
                identifierWriter.write(object, value);
            }
        }

        @Override
        public String getIdentifierName() {
            return identifierName;
        }

        @Override
        public Object getProperty(Object object, int index) {
            return readers[index].read(object);
        }

        @Override
        public void setProperty(Object object, int index, Object value) {
            writers[index].write(object,value);
        }



        static class FastMethodReader implements PropertyReader {
            final FastMethod method;

            public FastMethodReader(FastMethod method) {
                this.method = method;
            }

            @Override
            public Class propertyType() {
                return method.getReturnType();
            }

            @Override
            public Object read(Object object) {
                try {
                    return method.invoke(object, InvokerHelper.EMPTY_ARGS);
                } catch (InvocationTargetException e) {
                    ReflectionUtils.handleInvocationTargetException(e);
                    return null;
                }
            }
        }

        static class FastMethodWriter implements PropertyWriter {
            final FastMethod method;
            final Class propertyType;

            public FastMethodWriter(FastMethod method, Class propertyType) {
                this.method = method;
                this.propertyType = propertyType;
            }

            @Override
            public Class propertyType() {
                return propertyType;
            }

            @Override
            public void write(Object object, Object value) {
                try {
                    method.invoke(object, new Object[]{value});
                } catch (InvocationTargetException e) {
                    ReflectionUtils.handleInvocationTargetException(e);
                }
            }
        }

        static class FieldReader implements PropertyReader {
            final Field field;

            public FieldReader(Field field) {
                this.field = field;
            }

            @Override
            public Class propertyType() {
                return field.getType();
            }

            @Override
            public Object read(Object object) {
                return ReflectionUtils.getField(field, object);
            }
        }

        static class FieldWriter implements PropertyWriter {
            final Field field;

            public FieldWriter(Field field) {
                this.field = field;
            }

            @Override
            public Class propertyType() {
                return field.getType();
            }

            @Override
            public void write(Object object, Object value) {
                ReflectionUtils.setField(field, object, value);
            }
        }
    }
}
