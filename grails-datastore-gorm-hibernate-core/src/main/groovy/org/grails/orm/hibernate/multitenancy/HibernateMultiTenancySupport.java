package org.grails.orm.hibernate.multitenancy;

import grails.gorm.multitenancy.Tenants;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.config.GormProperties;
import org.grails.orm.hibernate.AbstractHibernateDatastore;

import java.io.Serializable;

/**
 * Support methods for Hibernate Multi Tenancy
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public class HibernateMultiTenancySupport {

    public static final String TENANT_ID_SUFFIX = "TenantId";

    /**
     * The name of the Hibernate filter for the given class
     * @param entity The entity
     * @return The filter name
     */
    public static String getTenantIdFilterName(PersistentEntity entity) {
        return entity.getJavaClass().getSimpleName() + TENANT_ID_SUFFIX;
    }

    /**
     * Enable the tenant id filter for the given datastore and entity
     *
     * @param datastore The datastore
     * @param entity The entity
     */
    public static void enableFilter(AbstractHibernateDatastore datastore, PersistentEntity entity) {

        String filterName = getTenantIdFilterName(entity);
        Serializable currentId = Tenants.currentId(datastore.getClass());
        datastore.getHibernateTemplate()
                .getSessionFactory()
                .getCurrentSession()
                .enableFilter(filterName)
                .setParameter(GormProperties.TENANT_IDENTITY, currentId);
    }

    /**
     * Disable the tenant id filter for the given datastore and entity
     *
     * @param datastore The datastore
     * @param entity The entity
     */
    public static void disableFilter(AbstractHibernateDatastore datastore, PersistentEntity entity) {
        String filterName = getTenantIdFilterName(entity);
        datastore.getHibernateTemplate()
                .getSessionFactory()
                .getCurrentSession()
                .disableFilter(filterName);
    }
}
