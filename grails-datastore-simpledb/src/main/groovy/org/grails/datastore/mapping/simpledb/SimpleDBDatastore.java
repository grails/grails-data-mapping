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
package org.grails.datastore.mapping.simpledb;

import static org.grails.datastore.mapping.config.utils.ConfigUtils.read;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.grails.datastore.mapping.cache.TPCacheAdapterRepository;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.model.types.OneToMany;
import org.grails.datastore.mapping.simpledb.engine.*;
import org.grails.datastore.mapping.simpledb.util.SimpleDBUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.grails.datastore.mapping.core.AbstractDatastore;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.simpledb.config.SimpleDBMappingContext;
import org.grails.datastore.mapping.simpledb.model.types.SimpleDBTypeConverterRegistrar;
import org.grails.datastore.mapping.simpledb.util.DelayAfterWriteSimpleDBTemplateDecorator;
import org.grails.datastore.mapping.simpledb.util.SimpleDBTemplate;
import org.grails.datastore.mapping.simpledb.util.SimpleDBTemplateImpl;

/**
 * A Datastore implementation for the AWS SimpleDB document store.
 *
 * @author Roman Stepanenko based on Graeme Rocher code for MongoDb and Redis
 * @since 0.1
 */
public class SimpleDBDatastore extends AbstractDatastore implements InitializingBean, MappingContext.Listener {

    public static final String SECRET_KEY = "secretKey";
    public static final String ACCESS_KEY = "accessKey";
    public static final String DOMAIN_PREFIX_KEY = "domainNamePrefix";
    public static final String DELAY_AFTER_WRITES_MS = "delayAfterWritesMS"; //used for testing - to fight eventual consistency if this flag value is 'true' it will add specified pause after writes

//    private Map<PersistentEntity, SimpleDBTemplate> simpleDBTemplates = new ConcurrentHashMap<PersistentEntity, SimpleDBTemplate>();
    private SimpleDBTemplate simpleDBTemplate;  //currently there is no need to create template per entity, we can share same instance
    protected Map<AssociationKey, SimpleDBAssociationInfo> associationInfoMap = new HashMap<AssociationKey, SimpleDBAssociationInfo>(); //contains entries only for those associations that need a dedicated domain
    protected Map<PersistentEntity, SimpleDBDomainResolver> entityDomainResolverMap = new HashMap<PersistentEntity, SimpleDBDomainResolver>();

    private String domainNamePrefix;

    public SimpleDBDatastore() {
        this(new SimpleDBMappingContext(), Collections.<String, String>emptyMap(), null, null);
    }

    /**
     * Constructs a SimpleDBDatastore using the given MappingContext and connection details map.
     *
     * @param mappingContext The SimpleDBMappingContext
     * @param connectionDetails The connection details containing the {@link #ACCESS_KEY} and {@link #SECRET_KEY} settings
     */
    public SimpleDBDatastore(MappingContext mappingContext,
            Map<String, String> connectionDetails, ConfigurableApplicationContext ctx, TPCacheAdapterRepository<SimpleDBNativeItem> adapterRepository) {
        super(mappingContext, connectionDetails, ctx, adapterRepository);

        if (mappingContext != null) {
            mappingContext.addMappingContextListener(this);
        }

        initializeConverters(mappingContext);

        domainNamePrefix = read(String.class, DOMAIN_PREFIX_KEY, connectionDetails, null);
    }

    public SimpleDBDatastore(MappingContext mappingContext, Map<String, String> connectionDetails) {
        this(mappingContext, connectionDetails, null, null);
    }

    public SimpleDBDatastore(MappingContext mappingContext) {
        this(mappingContext, Collections.<String, String>emptyMap(), null, null);
    }

    public SimpleDBTemplate getSimpleDBTemplate(@SuppressWarnings("unused") PersistentEntity entity) {
//        return simpleDBTemplates.get(entity);
        return simpleDBTemplate;
    }

    public SimpleDBTemplate getSimpleDBTemplate() {
        return simpleDBTemplate;
    }

    @Override
    protected Session createSession(Map<String, String> connDetails) {
        String delayAfterWrite = read(String.class, DELAY_AFTER_WRITES_MS, connectionDetails, null);

        if (delayAfterWrite != null && !"".equals(delayAfterWrite)) {
            return new DelayAfterWriteSimpleDBSession(this, getMappingContext(), getApplicationEventPublisher(), Integer.parseInt(delayAfterWrite), cacheAdapterRepository);
        }
        return new SimpleDBSession(this, getMappingContext(), getApplicationEventPublisher(), cacheAdapterRepository);
    }

    public void afterPropertiesSet() throws Exception {
        for (PersistentEntity entity : mappingContext.getPersistentEntities()) {
            // Only create SimpleDB templates for entities that are mapped with SimpleDB
            if (!entity.isExternal()) {
                createSimpleDBTemplate(entity);
            }
        }
    }

    protected void createSimpleDBTemplate(@SuppressWarnings("unused") PersistentEntity entity) {
        if (simpleDBTemplate != null) {
            return;
        }

        String accessKey = read(String.class, ACCESS_KEY, connectionDetails, null);
        String secretKey = read(String.class, SECRET_KEY, connectionDetails, null);
        String delayAfterWrite = read(String.class, DELAY_AFTER_WRITES_MS, connectionDetails, null);

        simpleDBTemplate = new SimpleDBTemplateImpl(accessKey, secretKey);
        if (delayAfterWrite != null && !"".equals(delayAfterWrite)) {
            simpleDBTemplate = new DelayAfterWriteSimpleDBTemplateDecorator(simpleDBTemplate, Integer.parseInt(delayAfterWrite));
        }
    }

    /**
     * If specified, returns domain name prefix so that same AWS account can be used for more than one environment (DEV/TEST/PROD etc).
     * @return null if name was not specified in the configuration
     */
    public String getDomainNamePrefix() {
        return domainNamePrefix;
    }

    public void persistentEntityAdded(PersistentEntity entity) {
        createSimpleDBTemplate(entity);
        analyzeAssociations(entity);
        createEntityDomainResolver(entity);
    }

    /**
     * If the specified association has a dedicated AWS domains, returns info for that association,
     * otherwise returns null.
     */
    public SimpleDBAssociationInfo getAssociationInfo(Association<?> association) {
        return associationInfoMap.get(generateAssociationKey(association));
    }

    /**
     * Returns domain resolver for the specified entity.
     * @param entity
     * @return
     */
    public SimpleDBDomainResolver getEntityDomainResolver(PersistentEntity entity) {
        return entityDomainResolverMap.get(entity);
    }

    protected void createEntityDomainResolver(PersistentEntity entity) {
        SimpleDBDomainResolverFactory resolverFactory = new SimpleDBDomainResolverFactory();
        SimpleDBDomainResolver domainResolver = resolverFactory.buildResolver(entity, this);

        entityDomainResolverMap.put(entity, domainResolver);
    }

    @Override
    protected void initializeConverters(@SuppressWarnings("hiding") MappingContext mappingContext) {
        final ConverterRegistry conversionService = mappingContext.getConverterRegistry();
        new SimpleDBTypeConverterRegistrar().register(conversionService);
    }

    /**
     * Analyzes associations and for those associations that need to be stored
     * in a dedicated AWS domain, creates info object with details for that association.
     */
    protected void analyzeAssociations(PersistentEntity entity) {
        for (Association<?> association : entity.getAssociations()) {
            if (association instanceof OneToMany && !association.isBidirectional()) {
                String associationDomainName = generateAssociationDomainName(association);
                associationInfoMap.put(generateAssociationKey(association), new SimpleDBAssociationInfo(associationDomainName));
            }
        }
    }

    protected AssociationKey generateAssociationKey(Association<?> association) {
        return new AssociationKey(association.getOwner(), association.getName());
    }

    protected String generateAssociationDomainName(Association<?> association) {
        String ownerDomainName = SimpleDBUtil.getMappedDomainName(association.getOwner());
        return SimpleDBUtil.getPrefixedDomainName(domainNamePrefix, ownerDomainName.toUpperCase()+"_"+association.getName().toUpperCase());
    }
}
