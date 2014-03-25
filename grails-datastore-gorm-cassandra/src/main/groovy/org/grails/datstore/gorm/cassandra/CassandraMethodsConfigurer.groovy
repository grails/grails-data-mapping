package org.grails.datstore.gorm.cassandra

import org.grails.datastore.gorm.plugin.support.DynamicMethodsConfigurer
import org.grails.datastore.mapping.core.Datastore;
import org.springframework.transaction.PlatformTransactionManager

class CassandraMethodsConfigurer extends DynamicMethodsConfigurer {

	CassandraMethodsConfigurer(Datastore datastore, PlatformTransactionManager transactionManager) {
		super(datastore, transactionManager);
	}

	@Override
	String getDatastoreType() {
		return "Cassandra";
	}
}
