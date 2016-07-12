package org.grails.orm.hibernate.connections;

import org.grails.datastore.mapping.core.connections.*;
import org.grails.orm.hibernate.cfg.Settings;
import org.grails.orm.hibernate.jdbc.connections.DataSourceConnectionSourceFactory;
import org.grails.orm.hibernate.jdbc.connections.DataSourceSettings;
import org.grails.orm.hibernate.jdbc.connections.DataSourceSettingsBuilder;
import org.hibernate.SessionFactory;
import org.springframework.core.env.PropertyResolver;

import javax.sql.DataSource;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * Constructs a Hibernate {@link SessionFactory}
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public abstract class AbstractHibernateConnectionSourceFactory extends AbstractConnectionSourceFactory<SessionFactory, HibernateConnectionSourceSettings> {

    protected DataSourceConnectionSourceFactory dataSourceConnectionSourceFactory = new DataSourceConnectionSourceFactory();

    /**
     * Sets the factory for creating SQL {@link DataSource} connection sources
     *
     * @param dataSourceConnectionSourceFactory
     */
    public void setDataSourceConnectionSourceFactory(DataSourceConnectionSourceFactory dataSourceConnectionSourceFactory) {
        this.dataSourceConnectionSourceFactory = dataSourceConnectionSourceFactory;
    }

    public ConnectionSource<SessionFactory, HibernateConnectionSourceSettings> create(String name, HibernateConnectionSourceSettings settings) {
        DataSourceSettings dataSourceSettings = settings.getDataSource();
        ConnectionSource<DataSource, DataSourceSettings> dataSourceConnectionSource = dataSourceConnectionSourceFactory.create(name, dataSourceSettings);
        return create(name, dataSourceConnectionSource, settings);
    }


    @Override
    public Serializable getConnectionSourcesConfigurationKey() {
        return Settings.SETTING_DATASOURCES;
    }

    protected abstract ConnectionSource<SessionFactory, HibernateConnectionSourceSettings> create(String name, ConnectionSource<DataSource, DataSourceSettings> dataSourceConnectionSource,  HibernateConnectionSourceSettings settings);

    protected <F extends ConnectionSourceSettings> HibernateConnectionSourceSettings buildSettings(String name, PropertyResolver configuration, F fallbackSettings, boolean isDefaultDataSource) {
        HibernateConnectionSourceSettingsBuilder builder;
        HibernateConnectionSourceSettings settings;
        if(isDefaultDataSource) {
            String qualified = Settings.SETTING_DATASOURCES + '.' + Settings.SETTING_DATASOURCE;
            builder = new HibernateConnectionSourceSettingsBuilder(configuration, "", fallbackSettings);
            Map config = configuration.getProperty(qualified, Map.class, Collections.emptyMap());
            settings = builder.build();
            if(!config.isEmpty()) {

                DataSourceSettings dsfallbackSettings = null;
                if(fallbackSettings instanceof HibernateConnectionSourceSettings) {
                    dsfallbackSettings = ((HibernateConnectionSourceSettings)fallbackSettings).getDataSource();
                }
                else if(fallbackSettings instanceof DataSourceSettings) {
                    dsfallbackSettings = (DataSourceSettings) fallbackSettings;
                }
                DataSourceSettingsBuilder dataSourceSettingsBuilder = new DataSourceSettingsBuilder(configuration, qualified, dsfallbackSettings);
                DataSourceSettings dataSourceSettings = dataSourceSettingsBuilder.build();
                settings.setDataSource(dataSourceSettings);
            }
        }
        else {
            String prefix = Settings.SETTING_DATASOURCES + "." + name;
            builder = new HibernateConnectionSourceSettingsBuilder(configuration, prefix, fallbackSettings);

            DataSourceSettings dsfallbackSettings = null;
            if(fallbackSettings instanceof HibernateConnectionSourceSettings) {
                dsfallbackSettings = ((HibernateConnectionSourceSettings)fallbackSettings).getDataSource();
            }
            else if(fallbackSettings instanceof DataSourceSettings) {
                dsfallbackSettings = (DataSourceSettings) fallbackSettings;
            }

            settings = builder.build();
            if(configuration.getProperty(prefix + ".dataSource", Map.class, Collections.emptyMap()).isEmpty()) {
                DataSourceSettingsBuilder dataSourceSettingsBuilder = new DataSourceSettingsBuilder(configuration, prefix, dsfallbackSettings);
                DataSourceSettings dataSourceSettings = dataSourceSettingsBuilder.build();
                settings.setDataSource(dataSourceSettings);
            }
        }
        return settings;
    }
}
