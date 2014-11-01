package org.grails.datastore.mapping.cassandra;

import static org.springframework.cassandra.core.cql.CqlIdentifier.cqlId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.grails.datastore.mapping.cassandra.config.Column;
import org.grails.datastore.mapping.cassandra.config.Table;
import org.grails.datastore.mapping.cassandra.utils.EnumUtil;
import org.grails.datastore.mapping.core.EntityCreationException;
import org.grails.datastore.mapping.model.IllegalMappingException;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cassandra.core.SessionCallback;
import org.springframework.cassandra.core.cql.CqlIdentifier;
import org.springframework.cassandra.core.cql.generator.CreateIndexCqlGenerator;
import org.springframework.cassandra.core.cql.generator.CreateTableCqlGenerator;
import org.springframework.cassandra.core.keyspace.ColumnSpecification;
import org.springframework.cassandra.core.keyspace.CreateIndexSpecification;
import org.springframework.cassandra.core.keyspace.CreateTableSpecification;
import org.springframework.cassandra.core.keyspace.TableOption;
import org.springframework.cassandra.core.keyspace.TableOption.CachingOption;
import org.springframework.cassandra.core.keyspace.TableOption.CompactionOption;
import org.springframework.cassandra.core.keyspace.TableOption.CompressionOption;
import org.springframework.dao.DataAccessException;
import org.springframework.data.cassandra.config.CassandraSessionFactoryBean;
import org.springframework.data.cassandra.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.mapping.CassandraPersistentEntity;
import org.springframework.data.cassandra.mapping.CassandraPersistentProperty;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.util.Assert;

import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.TableMetadata;

public class GormCassandraSessionFactoryBean extends CassandraSessionFactoryBean {

	private static Logger log = LoggerFactory.getLogger(GormCassandraSessionFactoryBean.class);
	private static final String COMPACT_STORAGE = "compact_storage";

	protected MappingContext gormMappingContext;
	protected CassandraMappingContext springCassandraMappingContext;
	private static List<String> allowableTableOptions;

	public GormCassandraSessionFactoryBean(MappingContext mappingContext, CassandraMappingContext springCassandraMappingContext) {
		this.gormMappingContext = mappingContext;
		this.springCassandraMappingContext = springCassandraMappingContext;
		allowableTableOptions = EnumUtil.getValidEnumList(TableOption.class);
		allowableTableOptions.add(COMPACT_STORAGE);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		performIndexAction();
	}

	public void createTable(Class<?> cls) {
		Assert.notNull(cls);
		CassandraPersistentEntity<?> cassandraPersistentEntity = springCassandraMappingContext.getPersistentEntity(cls);
		if (cassandraPersistentEntity == null) {
			throw new EntityCreationException(String.format("Class [%s] not found in mapping context", cls.getName()));
		}
		createTable(cassandraPersistentEntity);
		createIndex(cassandraPersistentEntity);
	}

	protected void createTables(boolean dropTables, boolean dropUnused) {

		Metadata md = session.getCluster().getMetadata();
		KeyspaceMetadata kmd = md.getKeyspace(keyspaceName);

		// TODO: fix this with KeyspaceIdentifier
		if (kmd == null) { // try lower-cased keyspace name
			kmd = md.getKeyspace(keyspaceName.toLowerCase());
		}

		if (kmd == null) {
			throw new IllegalStateException(String.format("keyspace [%s] does not exist", keyspaceName));
		}

		for (TableMetadata table : kmd.getTables()) {
			if (dropTables) {
				if (dropUnused || mappingContext.usesTable(table)) {
					admin.dropTable(cqlId(table.getName()));
				}
			}
		}

		Collection<? extends CassandraPersistentEntity<?>> entities = converter.getMappingContext().getNonPrimaryKeyEntities();

		// create all specifications first so any spec errors causes the whole
		// operation to fail - minimizes the keyspace being left in an inconsistent state
		for (final CreateTableSpecification createTableSpecification : createTableSpecifications(entities)) {
			createTable(createTableSpecification);
		}
	}

	protected void createTable(CassandraPersistentEntity<?> cassandraPersistentEntity) {
		createTable(createTableSpecification(cassandraPersistentEntity));
	}

	protected void createTable(final CreateTableSpecification createTableSpecification) {
		admin.execute(new SessionCallback<Object>() {
			@Override
			public Object doInSession(Session s) throws DataAccessException {
				String cql = new CreateTableCqlGenerator(createTableSpecification).toCql();
				log.debug(String.format("executing [%s]",cql));
				try {
					s.execute(cql);
				} catch (Exception e) {
					throw new EntityCreationException(String.format("Failed to create table [%s]", createTableSpecification.getName()), e);
				}
				return null;
			}
		});
	}

	protected List<CreateTableSpecification> createTableSpecifications(Collection<? extends CassandraPersistentEntity<?>> entities) {
		final List<CreateTableSpecification> createTableSpecifications = new ArrayList<CreateTableSpecification>();
		for (final CassandraPersistentEntity<?> entity : entities) {
			CreateTableSpecification createTableSpecification = createTableSpecification(entity);
			createTableSpecifications.add(createTableSpecification);
		}

		return createTableSpecifications;
	}

	/*
	 * For the specified entity creates a CreateTableSpecification and sets the
	 * clustering order and table options based on information held in an
	 * entity's Table class
	 */
	protected CreateTableSpecification createTableSpecification(final CassandraPersistentEntity<?> entity) {
		PersistentEntity gormEntity = gormMappingContext.getPersistentEntity(entity.getName());
		Table table = (Table) gormEntity.getMapping().getMappedForm();
		CreateTableSpecification createTableSpecification = springCassandraMappingContext.getCreateTableSpecificationFor(entity);

		// set the clustering order
		for (Column column : table.getColumns()) {
			if (column.getOrder() != null) {
				// need to work with CqlIdentifier as it may be different from
				// the property name, hence why CassandraPersistentProperty is
				// needed here
				CassandraPersistentProperty persistentProperty = entity.getPersistentProperty(column.getName());
				if (persistentProperty == null || !column.isClusterKey()) {
					throw new IllegalMappingException(String.format("Invalid mapping for property [%s]. [order] attribute can only be set for a clustered primary key", column.getName()));
				}
				CqlIdentifier clusteringKeyName = persistentProperty.getColumnName();
				for (ColumnSpecification clusterKeyColumn : createTableSpecification.getClusteredKeyColumns()) {
					if (clusterKeyColumn.getName().equals(clusteringKeyName)) {
						clusterKeyColumn.clustered(column.getOrder());
						break;
					}
				}
			}
		}

		// set the various table options
		Map<String, Object> gormTableProperties = table.getTableProperties();
		if (gormTableProperties != null) {
			if (gormTableProperties.remove(COMPACT_STORAGE) != null) {
				createTableSpecification.with(TableOption.COMPACT_STORAGE);
			}
			Map<TableOption, Object> tableOptions = convertToCassandraOptionEnumObjectMap(TableOption.class, gormTableProperties, "tableOptions");
			for (Entry<TableOption, Object> tableOptionEntry : tableOptions.entrySet()) {
				Object value = tableOptionEntry.getValue();
				Map<String, Object> tableOptionMap = value instanceof Map ? (Map<String, Object>) value : null;

				if (TableOption.COMPACTION == tableOptionEntry.getKey()) {
					Map<CompactionOption, Object> compactionOptions = convertToCassandraOptionEnumObjectMap(CompactionOption.class, tableOptionMap, "compaction options");
					createTableSpecification.with(TableOption.COMPACTION, compactionOptions);

				} else if (TableOption.COMPRESSION == tableOptionEntry.getKey()) {
					Map<CompressionOption, Object> compressionOptions = convertToCassandraOptionEnumObjectMap(CompressionOption.class, tableOptionMap, "compression options");
					createTableSpecification.with(TableOption.COMPRESSION, compressionOptions);

				} else if (TableOption.CACHING == tableOptionEntry.getKey()) {
					CachingOption cachingOption = EnumUtil.getRequiredEnum(CachingOption.class, "caching", String.valueOf(value));
					createTableSpecification.with(TableOption.CACHING, cachingOption);
					
				} else if (TableOption.COMPACT_STORAGE == tableOptionEntry.getKey()) {
					createTableSpecification.with(TableOption.COMPACT_STORAGE);
					
				} else {
					createTableSpecification.with(tableOptionEntry.getKey(), value);
				}
			}
		}
		return createTableSpecification;
	}

	/*
	 * For each key in the specified stringObjectMap, replace it with the
	 * equivalent Enum of type enumClass if found, else throw
	 * IllegalMappingException
	 */
	protected <E extends Enum<E>> Map<E, Object> convertToCassandraOptionEnumObjectMap(Class<E> enumClass, Map<String, Object> stringObjectMap, String optionEnumTypeName) {
		if (stringObjectMap == null) {
			return null;
		}
		Map<E, Object> result = new LinkedHashMap<E, Object>();
		for (Entry<String, Object> optionEntry : stringObjectMap.entrySet()) {
			E e = EnumUtil.findEnum(enumClass, optionEntry.getKey());
			if (e != null) {
				result.put(e, optionEntry.getValue());
			} else {
				List<String> allowable = null;
				if (enumClass == TableOption.class) {
					allowable = allowableTableOptions;
				} else {
					allowable = EnumUtil.getValidEnumList(enumClass);
				}
				throw new IllegalMappingException(String.format("Invalid option [%s] for [%s], allowable values are %s", optionEntry.getKey(), optionEnumTypeName, allowable));
			}
		}
		return result;
	}

	protected void performIndexAction() {
		switch (schemaAction) {

		case NONE:
			return;

		case RECREATE_DROP_UNUSED:
			// don't break!
		case RECREATE:
			// don't break!
		case CREATE:
			createIndex();
		}

	}

	protected void createIndex() {
		Collection<? extends CassandraPersistentEntity<?>> entities = mappingContext.getNonPrimaryKeyEntities();
		for (final CassandraPersistentEntity<?> entity : entities) {
			createIndex(entity);
		}
	}

	protected void createIndex(final CassandraPersistentEntity<?> entity) {
		entity.doWithProperties(new PropertyHandler<CassandraPersistentProperty>() {
			@Override
			public void doWithPersistentProperty(final CassandraPersistentProperty persistentProperty) {
				if (persistentProperty.isIndexed()) {
					final CreateIndexSpecification createIndexSpecification = new CreateIndexSpecification();
					createIndexSpecification.tableName(entity.getTableName()).columnName(persistentProperty.getColumnName()).ifNotExists();
					admin.execute(new SessionCallback<ResultSet>() {
						@Override
						public ResultSet doInSession(com.datastax.driver.core.Session s) throws DataAccessException {
							String cql = CreateIndexCqlGenerator.toCql(createIndexSpecification);
							log.debug(String.format("executing [%s]",cql));
							try {
								return s.execute(cql);
							} catch (Exception e) {
								throw new EntityCreationException(String.format("Failed to create index for property [%s] in entity [%s]", 
										persistentProperty.getName(),
										entity.getName()), e);
							}
						}
					});
				}
			}
		});
	}
	
	@Override
	public void destroy() throws Exception {
		executeScripts(shutdownScripts);
		if (session != null) {
			session.close();
		}
	}
}