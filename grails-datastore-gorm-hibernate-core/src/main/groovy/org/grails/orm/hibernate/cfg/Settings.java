package org.grails.orm.hibernate.cfg;

/**
 * Settings for Hibernate
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public interface Settings extends org.grails.datastore.mapping.config.Settings {

    /**
     * The data sources setting
     */
    String SETTING_DATASOURCES = "dataSources";


    /**
     * The data source setting
     */
    String SETTING_DATASOURCE = "dataSource";

    /**
     * The dbCreate setting (defaults to none)
     */
    String SETTING_DB_CREATE = SETTING_DATASOURCE + ".dbCreate";
}
