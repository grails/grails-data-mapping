package org.grails.datastore.mapping.cassandra.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.grails.datastore.mapping.config.Entity;
import org.grails.datastore.mapping.query.Query;

/**
 * Provides configuration options for mapping Cassandra tables
 *
 */
@SuppressWarnings("rawtypes")
public class Table extends Entity {
	private String keyspace;
	private List<Column> columns = new ArrayList<Column>();
	private List<Column> primaryKeys = new ArrayList<Column>();
	private List<String> primaryKeyNames = new ArrayList<String>();
	private Query.Order sort;
	
	private Map<String, Object> tableProperties;

	public Table() {

	}

	public Table(String keyspace) {
		this.keyspace = keyspace;
	}

	public String getKeyspace() {
		return keyspace;
	}

	public void setKeyspace(String keyspace) {
		this.keyspace = keyspace;
	}

	public void addColumn(Column column) {
		columns.add(column);
		if (column.isPrimaryKey()) {
			primaryKeys.add(column);
			primaryKeyNames.add(column.getName());
		}
	}
	
	public List<Column> getColumns() {
		return columns;
	}

	public List<Column> getPrimaryKeys() {
		return primaryKeys;
	}

	public String[] getPrimaryKeyNames() {
		return primaryKeyNames.toArray(new String[0]);
	}

	public boolean hasCompositePrimaryKeys() {
		return primaryKeys.size() > 1;
	}

	public boolean isPrimaryKey(String name) {
		return primaryKeyNames.contains(name);
	}

	public Query.Order getSort() {
		return sort;
	}

	public void setSort(Object sort) {
		if (sort instanceof Query.Order) {
			this.sort = (Query.Order) sort;
		}
		if (sort instanceof Map) {
			Map m = (Map) sort;
			if (!m.isEmpty()) {
				Map.Entry entry = (Map.Entry) m.entrySet().iterator().next();
				Object key = entry.getKey();
				if ("desc".equalsIgnoreCase(entry.getValue().toString())) {
					this.sort = Query.Order.desc(key.toString());
				} else {
					this.sort = Query.Order.asc(key.toString());
				}
			}
		} else {
			this.sort = Query.Order.asc(sort.toString());
		}
	}	

	public Map<String, Object> getTableProperties() {
		return tableProperties;
	}

	public void setTableProperties(Map<String, Object> tableProperties) {
		this.tableProperties = tableProperties;
	}
}
