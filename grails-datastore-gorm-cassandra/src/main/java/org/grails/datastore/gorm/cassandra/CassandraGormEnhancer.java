package org.grails.datastore.gorm.cassandra;

import org.grails.datastore.gorm.GormEnhancer;
import org.grails.datastore.gorm.GormInstanceApi;
import org.grails.datastore.gorm.GormStaticApi;
import org.grails.datastore.mapping.core.Datastore;

public class CassandraGormEnhancer extends GormEnhancer {

	public CassandraGormEnhancer(Datastore datastore) {
		super(datastore);
	}

	@Override
	protected <D> GormInstanceApi<D> getInstanceApi(Class<D> cls) {
		System.out.println("Instacne API");
		return super.getInstanceApi(cls);
	}

	@Override
	protected <D> GormStaticApi<D> getStaticApi(Class<D> cls) {
		System.out.println("Static API");
		return super.getStaticApi(cls);
	}

}
