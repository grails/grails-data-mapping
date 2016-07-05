package org.grails.orm.hibernate.connections;

import org.grails.datastore.mapping.core.connections.ConnectionSource;
import org.grails.datastore.mapping.core.connections.ConnectionSourceFactory;
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettings;
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettingsBuilder;
import org.grails.orm.hibernate.cfg.Settings;
import org.grails.orm.hibernate.jdbc.connections.DataSourceConnectionSourceFactory;
import org.grails.orm.hibernate.jdbc.connections.DataSourceSettings;
import org.hibernate.SessionFactory;
import org.springframework.core.env.PropertyResolver;

import javax.sql.DataSource;
import java.io.Serializable;

/**
 * Constructs a Hibernate {@link SessionFactory}
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public abstract class AbstractHibernateConnectionSourceFactory implements ConnectionSourceFactory<SessionFactory, HibernateConnectionSourceSettings> {

    protected DataSourceConnectionSourceFactory dataSourceConnectionSourceFactory = new DataSourceConnectionSourceFactory();

    /**
     * Sets the factory for creating SQL {@link DataSource} connection sources
     *
     * @param dataSourceConnectionSourceFactory
     */
    public void setDataSourceConnectionSourceFactory(DataSourceConnectionSourceFactory dataSourceConnectionSourceFactory) {
        this.dataSourceConnectionSourceFactory = dataSourceConnectionSourceFactory;
    }

    @Override
    public ConnectionSource<SessionFactory, HibernateConnectionSourceSettings> create(String name, PropertyResolver configuration) {
        ConnectionSourceSettingsBuilder builder = new ConnectionSourceSettingsBuilder(configuration);
        ConnectionSourceSettings fallbackSettings = builder.build();

        return create(name, configuration, fallbackSettings);
    }

    @Override
    public <F extends ConnectionSourceSettings> ConnectionSource<SessionFactory, HibernateConnectionSourceSettings> create(String name, PropertyResolver configuration, F fallbackSettings) {

        HibernateConnectionSourceSettingsBuilder builder;
        if(ConnectionSource.DEFAULT.equals(name)) {
            builder = new HibernateConnectionSourceSettingsBuilder(configuration, "", fallbackSettings);
        }
        else {
            builder = new HibernateConnectionSourceSettingsBuilder(configuration, Settings.PREFIX + "." + name, fallbackSettings);
        }
        HibernateConnectionSourceSettings settings = builder.build();

        return create(name, settings);
    }

    public ConnectionSource<SessionFactory, HibernateConnectionSourceSettings> create(String name, HibernateConnectionSourceSettings settings) {
        DataSourceSettings dataSourceSettings = settings.getDataSource();

        ConnectionSource<DataSource, DataSourceSettings> dataSourceConnectionSource = dataSourceConnectionSourceFactory.create(name, dataSourceSettings);


        return create(name, dataSourceConnectionSource, settings);
    }

    public abstract ConnectionSource<SessionFactory, HibernateConnectionSourceSettings> create(String name, ConnectionSource<DataSource, DataSourceSettings> dataSourceConnectionSource,  HibernateConnectionSourceSettings settings);

    @Override
    public Serializable getConnectionSourcesConfigurationKey() {
        return Settings.SETTING_DATASOURCES;
    }
}
