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
package org.grails.datastore.mapping.cassandra;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.grails.datastore.gorm.cassandra.mapping.BasicCassandraMappingContext;
import org.grails.datastore.gorm.cassandra.mapping.MappingCassandraConverter;
import org.grails.datastore.gorm.cassandra.mapping.TimeZoneToStringConverter;
import org.grails.datastore.mapping.cassandra.config.CassandraMappingContext;
import org.grails.datastore.mapping.cassandra.utils.EnumUtil;
import org.grails.datastore.mapping.core.AbstractDatastore;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.core.SoftThreadLocalMap;
import org.grails.datastore.mapping.model.DatastoreConfigurationException;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cassandra.config.CassandraCqlClusterFactoryBean;
import org.springframework.cassandra.config.KeyspaceAction;
import org.springframework.cassandra.config.KeyspaceActionSpecificationFactoryBean;
import org.springframework.cassandra.config.KeyspaceAttributes;
import org.springframework.cassandra.core.WriteOptions;
import org.springframework.cassandra.core.keyspace.KeyspaceActionSpecification;
import org.springframework.cassandra.core.keyspace.KeyspaceOption.ReplicationStrategy;
import org.springframework.cassandra.support.CassandraExceptionTranslator;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertyResolver;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.core.CassandraAdminTemplate;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.util.Assert;

import com.datastax.driver.core.Cluster;

/**
 * A Datastore implementation for Cassandra. Uses Spring Data Cassandra Factory
 * beans to create and initialise the Cassandra driver cluster and session
 * 
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public class CassandraDatastore extends AbstractDatastore implements InitializingBean, DisposableBean, MappingContext.Listener {

	private static Logger log = LoggerFactory.getLogger(CassandraDatastore.class);
	// TODO make one keyspace for each session somehow, maybe just do a
	// different datastore instance?
	public static final String DEFAULT_KEYSPACE = "CassandraKeySpace";
	public static final SchemaAction DEFAULT_SCHEMA_ACTION = SchemaAction.NONE;
	public static final String CONTACT_POINTS = "grails.cassandra.contactPoints";
	public static final String PORT = "grails.cassandra.port";
	public static final String SCHEMA_ACTION = "grails.cassandra.dbCreate";
	public static final String KEYSPACE_CONFIG = "grails.cassandra.keyspace";
	public static final String KEYSPACE_NAME = "grails.cassandra.keyspace.name";
	public static final String DEFAULT_MAPPING = "grails.cassandra.default.mapping";
	public static final String KEYSPACE_ACTION = "grails.cassandra.keyspace.action";
	public static final String KEYSPACE_DURABLE_WRITES = "grails.cassandra.keyspace.durableWrites";
	public static final String KEYSPACE_REPLICATION_FACTOR = "grails.cassandra.keyspace.replicationFactor";
	public static final String KEYSPACE_REPLICATION_STRATEGY = "grails.cassandra.keyspace.replicationStrategy";
	public static final String KEYSPACE_NETWORK_TOPOLOGY = "grails.cassandra.keyspace.networkTopology";

	protected Cluster nativeCluster;
	protected com.datastax.driver.core.Session nativeSession;
	protected BasicCassandraMappingContext springCassandraMappingContext;
	protected CassandraTemplate cassandraTemplate;
	protected CassandraAdminTemplate cassandraAdminTemplate;
	protected CassandraCqlClusterFactoryBean cassandraCqlClusterFactoryBean;
	protected KeyspaceActionSpecificationFactoryBean keyspaceActionSpecificationFactoryBean;
	protected GormCassandraSessionFactoryBean cassandraSessionFactoryBean;
	protected boolean stateless = false;
	protected String keyspace;
	protected boolean developmentMode = false;
	
	private static final SoftThreadLocalMap PERSISTENCE_OPTIONS_MAP = new SoftThreadLocalMap();

	public CassandraDatastore() {
		this(new CassandraMappingContext(), Collections.<String, Object> emptyMap(), null);
	}

	public CassandraDatastore(Map<String, Object> connectionDetails, ConfigurableApplicationContext ctx) {
		this(new CassandraMappingContext(), connectionDetails, ctx);
	}

    public CassandraDatastore(CassandraMappingContext mappingContext, PropertyResolver connectionDetails, ConfigurableApplicationContext ctx) {
        super(mappingContext, connectionDetails, ctx);
        this.keyspace = mappingContext.getKeyspace();
        Assert.hasText(keyspace, "Keyspace must be set");
        springCassandraMappingContext = new BasicCassandraMappingContext(mappingContext);

        mappingContext.setSpringCassandraMappingContext(springCassandraMappingContext);
        if (mappingContext != null) {
            mappingContext.addMappingContextListener(this);
        }

        initializeConverters(mappingContext);

        if(log.isDebugEnabled()) {
            log.debug("Initializing Cassandra Datastore for keyspace: " + keyspace);
        }
    }

	public CassandraDatastore(CassandraMappingContext mappingContext, Map<String, Object> connectionDetails, ConfigurableApplicationContext ctx) {
		this(mappingContext, mapToPropertyResolver(connectionDetails), ctx);
	}

	/**
	 * Sets to development mode which enables automatic creation of keyspace and schema
	 *
	 * @param developmentMode True if development mode is enabled
     */
	public void setDevelopmentMode(boolean developmentMode) {
		this.developmentMode = developmentMode;
	}

	@Override
	protected void initializeConverters(MappingContext mappingContext) {
		super.initializeConverters(mappingContext);
		mappingContext.getConverterRegistry().addConverter(new TimeZoneToStringConverter());
	}

	public void setCassandraCqlClusterFactoryBean(CassandraCqlClusterFactoryBean cassandraCqlClusterFactoryBean) {
		this.cassandraCqlClusterFactoryBean = cassandraCqlClusterFactoryBean;
	}

	public void setKeyspaceActionSpecificationFactoryBean(KeyspaceActionSpecificationFactoryBean keyspaceActionSpecificationFactoryBean) {
		this.keyspaceActionSpecificationFactoryBean = keyspaceActionSpecificationFactoryBean;
	}

	public void setCassandraSessionFactoryBean(GormCassandraSessionFactoryBean cassandraSessionFactoryBean) {
		this.cassandraSessionFactoryBean = cassandraSessionFactoryBean;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		createCluster();
		createNativeSession();
	}

	protected Cluster createCluster() throws Exception {
		if (nativeCluster == null) {
			if (cassandraCqlClusterFactoryBean == null) {
				cassandraCqlClusterFactoryBean = new CassandraCqlClusterFactoryBean();
			}
			cassandraCqlClusterFactoryBean.setContactPoints(
					connectionDetails.getProperty(CONTACT_POINTS, CassandraCqlClusterFactoryBean.DEFAULT_CONTACT_POINTS)
			);
			cassandraCqlClusterFactoryBean.setPort(
					connectionDetails.getProperty(PORT, Integer.class, CassandraCqlClusterFactoryBean.DEFAULT_PORT)
			);
			Set<KeyspaceActionSpecification<?>> keyspaceSpecifications = createKeyspaceSpecifications();
			cassandraCqlClusterFactoryBean.setKeyspaceSpecifications(keyspaceSpecifications);
			cassandraCqlClusterFactoryBean.afterPropertiesSet();
			nativeCluster = cassandraCqlClusterFactoryBean.getObject();
			if (nativeCluster == null) {
				throw new DatastoreConfigurationException("Cassandra driver cluster not created");
			}
		}
		return nativeCluster;
	}

	protected Set<KeyspaceActionSpecification<?>> createKeyspaceSpecifications() {
		Set<KeyspaceActionSpecification<?>> specifications = Collections.emptySet();
		KeyspaceAction keyspaceAction = readKeyspaceAction();

		if (keyspaceAction != null) {
			log.info("Set keyspace generation strategy to '" + connectionDetails.getProperty(KEYSPACE_ACTION) + "'");
			if (keyspaceActionSpecificationFactoryBean == null) {
				keyspaceActionSpecificationFactoryBean = new KeyspaceActionSpecificationFactoryBean();
			}
			keyspaceActionSpecificationFactoryBean.setName(keyspace);
			keyspaceActionSpecificationFactoryBean.setAction(keyspaceAction);
			keyspaceActionSpecificationFactoryBean.setDurableWrites(
					connectionDetails.getProperty(KEYSPACE_DURABLE_WRITES, Boolean.class, KeyspaceAttributes.DEFAULT_DURABLE_WRITES)
			);
			ReplicationStrategy replicationStrategy = connectionDetails.getProperty(KEYSPACE_REPLICATION_STRATEGY, ReplicationStrategy.class, ReplicationStrategy.SIMPLE_STRATEGY);
			keyspaceActionSpecificationFactoryBean.setReplicationStrategy(replicationStrategy);

			if (replicationStrategy == ReplicationStrategy.SIMPLE_STRATEGY) {
				keyspaceActionSpecificationFactoryBean.setReplicationFactor(
						connectionDetails.getProperty(KEYSPACE_REPLICATION_FACTOR, Long.class, KeyspaceAttributes.DEFAULT_REPLICATION_FACTOR)
				);
			} else if (replicationStrategy == ReplicationStrategy.NETWORK_TOPOLOGY_STRATEGY) {
				Map networkTopology = connectionDetails.getProperty( KEYSPACE_NETWORK_TOPOLOGY, Map.class, null);
				if (networkTopology != null) {
					List<String> dataCenters = new ArrayList<String>();
					List<String> replicationFactors = new ArrayList<String>();
					for (Object o : networkTopology.entrySet()) {
						Entry entry = (Entry) o;
						dataCenters.add(String.valueOf(entry.getKey()));
						replicationFactors.add(String.valueOf(entry.getValue()));
					}
					keyspaceActionSpecificationFactoryBean.setNetworkTopologyDataCenters(dataCenters);
					keyspaceActionSpecificationFactoryBean.setNetworkTopologyReplicationFactors(replicationFactors);
				}
			}

			keyspaceActionSpecificationFactoryBean.setIfNotExists(true);
			try {
				keyspaceActionSpecificationFactoryBean.afterPropertiesSet();
				specifications = keyspaceActionSpecificationFactoryBean.getObject();

			} catch (Exception e) {
				throw new DatastoreConfigurationException(String.format("Failed to create keyspace [%s] ", keyspace), e);
			}
		}
		return specifications;
	}

	protected com.datastax.driver.core.Session createNativeSession() throws ClassNotFoundException, Exception {
		if (nativeSession == null) {
			Assert.notNull(nativeCluster, "Cassandra driver cluster not created");
			if (cassandraSessionFactoryBean == null) {
				cassandraSessionFactoryBean = new GormCassandraSessionFactoryBean(mappingContext, springCassandraMappingContext);
			}
			cassandraSessionFactoryBean.setCluster(nativeCluster);
			cassandraSessionFactoryBean.setKeyspaceName(this.keyspace);
			MappingCassandraConverter mappingCassandraConverter = new MappingCassandraConverter(cassandraMapping());
			cassandraSessionFactoryBean.setConverter(mappingCassandraConverter);
			SchemaAction schemaAction = readSchemaAction();
			cassandraSessionFactoryBean.setSchemaAction(schemaAction);
			if(log.isInfoEnabled()) {
				log.info("Set Cassandra db generation strategy to '" + schemaAction + "'");
			}
			// TODO: startup and shutdown scripts addition
			cassandraSessionFactoryBean.afterPropertiesSet();
			nativeSession = cassandraSessionFactoryBean.getObject();
			cassandraTemplate = new CassandraTemplate(nativeSession, mappingCassandraConverter);
			cassandraTemplate.setExceptionTranslator(new CassandraExceptionTranslator());
			cassandraAdminTemplate = new CassandraAdminTemplate(nativeSession, mappingCassandraConverter);
		}
		return nativeSession;
	}

	protected org.springframework.data.cassandra.mapping.CassandraMappingContext cassandraMapping() throws ClassNotFoundException {
		Collection<PersistentEntity> persistentEntities = mappingContext.getPersistentEntities();
		Set<Class<?>> entitySet = new HashSet<Class<?>>();
		for (PersistentEntity persistentEntity : persistentEntities) {
			entitySet.add(persistentEntity.getJavaClass());
		}
		springCassandraMappingContext.setInitialEntitySet(entitySet);
		springCassandraMappingContext.afterPropertiesSet();

		return springCassandraMappingContext;
	}

	@Override
	protected Session createSession(PropertyResolver connectionDetails) {
		if (stateless) {
			return createStatelessSession(connectionDetails);
		} else {
			return new CassandraSession(this, getMappingContext(), this.nativeSession, getApplicationEventPublisher(), false, cassandraTemplate);
		}
	}

	@Override
	protected Session createStatelessSession(PropertyResolver connectionDetails) {
		return new CassandraSession(this, getMappingContext(), this.nativeSession, getApplicationEventPublisher(), true, cassandraTemplate);
	}

	@Override
	public void persistentEntityAdded(PersistentEntity entity) {
		// get call here adds a persistententity to
		// springCassandraMappingContext
		springCassandraMappingContext.getPersistentEntity(entity.getJavaClass());
	}

	public Cluster getNativeCluster() {
		return nativeCluster;
	}

	public com.datastax.driver.core.Session getNativeSession() {
		return nativeSession;
	}

	public CassandraTemplate getCassandraTemplate() {
		return cassandraTemplate;
	}

	public void createTableDefinition(Class<?> cls) {
		cassandraSessionFactoryBean.createTable(cls);
	}

	public void setWriteOptions(final Object o, WriteOptions writeOptions) {
		if (o != null && writeOptions != null) {
			getPersistenceOptionsMap(o).put("writeOptions", writeOptions);
		}
	}

	public WriteOptions getWriteOptions(final Object o) {
		return (WriteOptions) getPersistenceOptionsMap(o).get("writeOptions");
	}

	@Override
	public void destroy() throws Exception {
		super.destroy();
		PERSISTENCE_OPTIONS_MAP.remove();
		if (cassandraSessionFactoryBean != null) {
			cassandraSessionFactoryBean.destroy();
		}
		if (cassandraCqlClusterFactoryBean != null) {				
			cassandraCqlClusterFactoryBean.destroy();
		}
	}


	private Map<String, Object> getPersistenceOptionsMap(final Object o) {
		Map<String, Object> persistenceOptionsMap = (Map<String, Object>) PERSISTENCE_OPTIONS_MAP.get().get(System.identityHashCode(o));
		if (persistenceOptionsMap == null) {
			persistenceOptionsMap = new HashMap<String, Object>();
			PERSISTENCE_OPTIONS_MAP.get().put(System.identityHashCode(o), persistenceOptionsMap);
		}
		return persistenceOptionsMap;
	}
	
	private KeyspaceAction readKeyspaceAction() {
		Map<String, KeyspaceAction> keyspaceActionMap = new HashMap<String, KeyspaceAction>();
		keyspaceActionMap.put("create", KeyspaceAction.CREATE);
		keyspaceActionMap.put("create-drop", KeyspaceAction.CREATE_DROP);
		return EnumUtil.findMatchingEnum(KEYSPACE_ACTION, connectionDetails.getProperty(KEYSPACE_ACTION,(String)null), keyspaceActionMap, developmentMode ? KeyspaceAction.CREATE_DROP : null);
	}
	
	private SchemaAction readSchemaAction() {		
		Map<String, SchemaAction> schemaActionMap = new HashMap<String, SchemaAction>();
		schemaActionMap.put("none", SchemaAction.NONE);
		schemaActionMap.put("create", SchemaAction.CREATE);
		schemaActionMap.put("recreate", SchemaAction.RECREATE);
		schemaActionMap.put("recreate-drop-unused", SchemaAction.RECREATE_DROP_UNUSED);
		return EnumUtil.findMatchingEnum(SCHEMA_ACTION, connectionDetails.getProperty(SCHEMA_ACTION, developmentMode ? "recreate-drop-unused" :  "none"), schemaActionMap, developmentMode ? SchemaAction.RECREATE_DROP_UNUSED : SchemaAction.NONE);
	}
}
