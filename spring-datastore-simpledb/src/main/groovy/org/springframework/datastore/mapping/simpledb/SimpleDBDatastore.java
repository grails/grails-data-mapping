package org.springframework.datastore.mapping.simpledb;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.datastore.mapping.core.AbstractDatastore;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.model.*;
import org.springframework.datastore.mapping.simpledb.config.SimpleDBMappingContext;
import org.springframework.datastore.mapping.simpledb.util.SimpleDBTemplate;
import org.springframework.datastore.mapping.simpledb.util.SimpleDBTemplateImpl;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.datastore.mapping.config.utils.ConfigUtils.read;

/**
 * A Datastore implementation for the AWS SimpleDB document store.
 *
 * @author Roman Stepanenko based on Graeme Rocher code for MongoDb and Redis 
 * @since 0.1
 */
public class SimpleDBDatastore extends AbstractDatastore implements InitializingBean, MappingContext.Listener {

    public SimpleDBDatastore() {
        this(new SimpleDBMappingContext(), Collections.<String, String>emptyMap(), null);
    }

    /**
     * Constructs a SimpleDBDatastore using the given MappingContext and connection details map.
     *
     * @param mappingContext The MongoMappingContext
     * @param connectionDetails The connection details containing the {@link #ACCESS_KEY} and {@link #SECRET_KEY} settings
     */
    public SimpleDBDatastore(MappingContext mappingContext,
            Map<String, String> connectionDetails, ConfigurableApplicationContext ctx) {
        super(mappingContext, connectionDetails, ctx);

        if (mappingContext != null) {
            mappingContext.addMappingContextListener(this);
        }

        initializeConverters(mappingContext);
    }

    public SimpleDBDatastore(MappingContext mappingContext, Map<String, String> connectionDetails) {
        this(mappingContext, connectionDetails, null);
    }
    
    public SimpleDBDatastore(MappingContext mappingContext) {
        this(mappingContext, Collections.<String, String>emptyMap(), null);
    }

    public SimpleDBTemplate getSimpleDBTemplate(PersistentEntity entity) {
        return simpleDBTemplates.get(entity);
    }

    @Override
    protected Session createSession(Map<String, String> connDetails) {
        return new SimpleDBSession(this, getMappingContext(), getApplicationEventPublisher());
    }

    public void afterPropertiesSet() throws Exception {
        for (PersistentEntity entity : mappingContext.getPersistentEntities()) {
            // Only create SimpleDB templates for entities that are mapped with SimpleDB
            if(!entity.isExternal()) {
                createSimpleDBTemplate(entity);
            }
        }
    }

    protected void createSimpleDBTemplate(PersistentEntity entity) {
        String accessKey = read(String.class, ACCESS_KEY, connectionDetails, null);
        String secretKey = read(String.class, SECRET_KEY, connectionDetails, null);

        final SimpleDBTemplate template = new SimpleDBTemplateImpl(accessKey, secretKey, entity);

        simpleDBTemplates.put(entity, template);
    }


    public void persistentEntityAdded(PersistentEntity entity) {
        createSimpleDBTemplate(entity);
    }

    private Map<PersistentEntity, SimpleDBTemplate> simpleDBTemplates = new ConcurrentHashMap<PersistentEntity, SimpleDBTemplate>();

    public static final String SECRET_KEY = "secretKey";
    public static final String ACCESS_KEY = "accessKey";
}