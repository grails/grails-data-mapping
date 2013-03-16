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
package org.grails.datastore.mapping.dynamodb;

import static org.grails.datastore.mapping.config.utils.ConfigUtils.read;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.grails.datastore.mapping.cache.TPCacheAdapterRepository;
import org.grails.datastore.mapping.core.AbstractDatastore;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.dynamodb.config.DynamoDBMappingContext;
import org.grails.datastore.mapping.dynamodb.engine.AssociationKey;
import org.grails.datastore.mapping.dynamodb.engine.DynamoDBAssociationInfo;
import org.grails.datastore.mapping.dynamodb.engine.DynamoDBIdGenerator;
import org.grails.datastore.mapping.dynamodb.engine.DynamoDBIdGeneratorFactory;
import org.grails.datastore.mapping.dynamodb.engine.DynamoDBNativeItem;
import org.grails.datastore.mapping.dynamodb.engine.DynamoDBTableResolver;
import org.grails.datastore.mapping.dynamodb.engine.DynamoDBTableResolverFactory;
import org.grails.datastore.mapping.dynamodb.model.types.DynamoDBTypeConverterRegistrar;
import org.grails.datastore.mapping.dynamodb.util.DelayAfterWriteDynamoDBTemplateDecorator;
import org.grails.datastore.mapping.dynamodb.util.DynamoDBTemplate;
import org.grails.datastore.mapping.dynamodb.util.DynamoDBTemplateImpl;
import org.grails.datastore.mapping.dynamodb.util.DynamoDBUtil;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.types.Association;
import org.grails.datastore.mapping.model.types.OneToMany;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.converter.ConverterRegistry;

/**
 * A Datastore implementation for the AWS DynamoDB document store.
 *
 * @author Roman Stepanenko based on Graeme Rocher code for MongoDb and Redis
 * @since 0.1
 */
public class DynamoDBDatastore extends AbstractDatastore implements InitializingBean, MappingContext.Listener {

    public static final String SECRET_KEY = "secretKey";
    public static final String ACCESS_KEY = "accessKey";
    public static final String TABLE_NAME_PREFIX_KEY = "tableNamePrefix";
    public static final String DEFAULT_READ_CAPACITY_UNITS = "defaultReadCapacityUnits";
    public static final String DEFAULT_WRITE_CAPACITY_UNITS = "defaultWriteCapacityUnits";
    public static final String DELAY_AFTER_WRITES_MS = "delayAfterWritesMS"; //used for testing - to fight eventual consistency if this flag value is 'true' it will add specified pause after writes

    private DynamoDBTemplate dynamoDBTemplate;  //currently there is no need to create template per entity, we can share same instance
    protected Map<AssociationKey, DynamoDBAssociationInfo> associationInfoMap = new HashMap<AssociationKey, DynamoDBAssociationInfo>(); //contains entries only for those associations that need a dedicated table
    protected Map<PersistentEntity, DynamoDBTableResolver> entityDomainResolverMap = new HashMap<PersistentEntity, DynamoDBTableResolver>();
    protected Map<PersistentEntity, DynamoDBIdGenerator> entityIdGeneratorMap = new HashMap<PersistentEntity, DynamoDBIdGenerator>();

    private String tableNamePrefix;

    private long defaultReadCapacityUnits;
    private long defaultWriteCapacityUnits;

    public DynamoDBDatastore() {
        this(new DynamoDBMappingContext(), Collections.<String, String>emptyMap(), null, null);
    }

    /**
     * Constructs a DynamoDBDatastore using the given MappingContext and connection details map.
     *
     * @param mappingContext The DynamoDBMappingContext
     * @param connectionDetails The connection details containing the {@link #ACCESS_KEY} and {@link #SECRET_KEY} settings
     */
    public DynamoDBDatastore(MappingContext mappingContext,
                             Map<String, String> connectionDetails, ConfigurableApplicationContext ctx, TPCacheAdapterRepository<DynamoDBNativeItem> adapterRepository) {
        super(mappingContext, connectionDetails, ctx, adapterRepository);

        if (mappingContext != null) {
            mappingContext.addMappingContextListener(this);
        }

        initializeConverters(mappingContext);

        tableNamePrefix = read(String.class, TABLE_NAME_PREFIX_KEY, connectionDetails, null);
        defaultReadCapacityUnits = read(Long.class, DEFAULT_READ_CAPACITY_UNITS, connectionDetails, (long)1); //minimum for the account in us-east-1 is 1
        defaultWriteCapacityUnits = read(Long.class, DEFAULT_WRITE_CAPACITY_UNITS, connectionDetails, (long)1); //minimum for the account in us-east-1 is 1
    }

    public DynamoDBDatastore(MappingContext mappingContext, Map<String, String> connectionDetails) {
        this(mappingContext, connectionDetails, null, null);
    }

    public DynamoDBDatastore(MappingContext mappingContext) {
        this(mappingContext, Collections.<String, String>emptyMap(), null, null);
    }

    public DynamoDBTemplate getDynamoDBTemplate(PersistentEntity entity) {
//        return dynamoDBTemplates.get(entity);
        return dynamoDBTemplate;
    }

    public DynamoDBTemplate getDynamoDBTemplate() {
        return dynamoDBTemplate;
    }

    @Override
    protected Session createSession(Map<String, String> connDetails) {
        String delayAfterWrite = read(String.class, DELAY_AFTER_WRITES_MS, connectionDetails, null);

        if (delayAfterWrite != null && !"".equals(delayAfterWrite)) {
            return new DelayAfterWriteDynamoDBSession(this, getMappingContext(), getApplicationEventPublisher(), Integer.parseInt(delayAfterWrite), cacheAdapterRepository);
        }
        return new DynamoDBSession(this, getMappingContext(), getApplicationEventPublisher(), cacheAdapterRepository);
    }

    public void afterPropertiesSet() throws Exception {
//        for (PersistentEntity entity : mappingContext.getPersistentEntities()) {
            // Only create DynamoDB templates for entities that are mapped with DynamoDB
//            if (!entity.isExternal()) {
//                createDynamoDBTemplate(entity);
//            }
//        }
    createDynamoDBTemplate();
}

    protected void createDynamoDBTemplate() {
        if (dynamoDBTemplate != null) {
            return;
        }

        String accessKey = read(String.class, ACCESS_KEY, connectionDetails, null);
        String secretKey = read(String.class, SECRET_KEY, connectionDetails, null);
        String delayAfterWrite = read(String.class, DELAY_AFTER_WRITES_MS, connectionDetails, null);

        dynamoDBTemplate = new DynamoDBTemplateImpl(accessKey, secretKey);
        if (delayAfterWrite != null && !"".equals(delayAfterWrite)) {
            dynamoDBTemplate = new DelayAfterWriteDynamoDBTemplateDecorator(dynamoDBTemplate, Integer.parseInt(delayAfterWrite));
        }
    }

    /**
     * If specified, returns table name prefix so that same AWS account can be used for more than one environment (DEV/TEST/PROD etc).
     * @return null if name was not specified in the configuration
     */
    public String getTableNamePrefix() {
        return tableNamePrefix;
    }

    public long getDefaultWriteCapacityUnits() {
        return defaultWriteCapacityUnits;
    }

    public long getDefaultReadCapacityUnits() {
        return defaultReadCapacityUnits;
    }

    public void persistentEntityAdded(PersistentEntity entity) {
        createDynamoDBTemplate();
        analyzeAssociations(entity);
        createEntityDomainResolver(entity);
        createEntityIdGenerator(entity);
    }

    /**
     * If the specified association has a dedicated AWS table, returns info for that association,
     * otherwise returns null.
     */
    public DynamoDBAssociationInfo getAssociationInfo(Association<?> association) {
        return associationInfoMap.get(generateAssociationKey(association));
    }

    /**
     * Returns table resolver for the specified entity.
     * @param entity
     * @return
     */
    public DynamoDBTableResolver getEntityDomainResolver(PersistentEntity entity) {
        return entityDomainResolverMap.get(entity);
    }

    /**
     * Returns id generator for the specified entity.
     * @param entity
     * @return
     */
    public DynamoDBIdGenerator getEntityIdGenerator(PersistentEntity entity) {
        return entityIdGeneratorMap.get(entity);
    }

    protected void createEntityDomainResolver(PersistentEntity entity) {
        DynamoDBTableResolverFactory resolverFactory = new DynamoDBTableResolverFactory();
        DynamoDBTableResolver tableResolver = resolverFactory.buildResolver(entity, this);

        entityDomainResolverMap.put(entity, tableResolver);
    }

    protected void createEntityIdGenerator(PersistentEntity entity) {
        DynamoDBIdGeneratorFactory factory = new DynamoDBIdGeneratorFactory();
        DynamoDBIdGenerator generator = factory.buildIdGenerator(entity, this);

        entityIdGeneratorMap.put(entity, generator);
    }

    @Override
    protected void initializeConverters(MappingContext mappingContext) {
        final ConverterRegistry conversionService = mappingContext.getConverterRegistry();
        new DynamoDBTypeConverterRegistrar().register(conversionService);
    }

    /**
     * Analyzes associations and for those associations that need to be stored
     * in a dedicated AWS table, creates info object with details for that association.
     */
    protected void analyzeAssociations(PersistentEntity entity) {
        for (Association<?> association : entity.getAssociations()) {
            if (association instanceof OneToMany && !association.isBidirectional()) {
                String associationDomainName = generateAssociationDomainName(association);
                associationInfoMap.put(generateAssociationKey(association), new DynamoDBAssociationInfo(associationDomainName));
            }
        }
    }

    protected AssociationKey generateAssociationKey(Association<?> association) {
        return new AssociationKey(association.getOwner(), association.getName());
    }

    protected String generateAssociationDomainName(Association<?> association) {
        String ownerDomainName = DynamoDBUtil.getMappedTableName(association.getOwner());
        return DynamoDBUtil.getPrefixedTableName(tableNamePrefix, ownerDomainName.toUpperCase() + "_" + association.getName().toUpperCase());
    }
}
