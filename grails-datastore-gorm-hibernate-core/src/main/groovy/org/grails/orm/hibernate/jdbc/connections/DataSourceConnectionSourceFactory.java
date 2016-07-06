package org.grails.orm.hibernate.jdbc.connections;

import org.grails.datastore.mapping.core.connections.ConnectionSource;
import org.grails.datastore.mapping.core.connections.ConnectionSourceFactory;
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettings;
import org.grails.datastore.mapping.core.connections.DefaultConnectionSource;
import org.grails.orm.hibernate.cfg.Settings;
import org.grails.orm.hibernate.jdbc.DataSourceBuilder;
import org.springframework.core.env.PropertyResolver;
import org.springframework.jndi.JndiObjectFactoryBean;

import javax.sql.DataSource;
import java.io.Serializable;
import java.util.Map;

/**
 * A {@link ConnectionSourceFactory} for creating JDBC {@link DataSource} connections
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public class DataSourceConnectionSourceFactory implements ConnectionSourceFactory<DataSource, DataSourceSettings> {
    @Override
    public ConnectionSource<DataSource, DataSourceSettings> create(String name, PropertyResolver configuration) {
        return create(name, configuration, null);
    }

    @Override
    public <F extends ConnectionSourceSettings> ConnectionSource<DataSource, DataSourceSettings> create(String name, PropertyResolver configuration, F fallbackSettings) {
        DataSourceSettingsBuilder builder = new DataSourceSettingsBuilder(configuration, ConnectionSource.DEFAULT.equals(name) ? Settings.SETTING_DATASOURCE : Settings.SETTING_DATASOURCES  + '.' + name);
        DataSourceSettings settings = builder.build();

        return create(name, settings);
    }

    public ConnectionSource<DataSource, DataSourceSettings> create(String name, DataSourceSettings settings) {

        if(settings.getJndiName() != null) {
            JndiObjectFactoryBean jndiObjectFactoryBean = new JndiObjectFactoryBean();
            jndiObjectFactoryBean.setExpectedType(DataSource.class);
            jndiObjectFactoryBean.setJndiName(settings.getJndiName());

            return new DefaultConnectionSource<>(name, (DataSource)jndiObjectFactoryBean.getObject(), settings);
        }
        else {

            DataSourceBuilder dataSourceBuilder = new DataSourceBuilder(getClass().getClassLoader());

            String driverClassName = settings.getDriverClassName();
            String username = settings.getUsername();
            String password = settings.getPassword();
            Map properties = settings.getProperties();
            String url = settings.getUrl();

            if(properties != null && !properties.isEmpty()) {
                dataSourceBuilder.properties(settings.toProperties());
            }
            dataSourceBuilder.url(url);

            if(driverClassName != null) {
                dataSourceBuilder.driverClassName(driverClassName);
            }
            if(username != null && password != null) {
                dataSourceBuilder.username(username);
                dataSourceBuilder.password(password);
            }

            return new DefaultConnectionSource<>(name, dataSourceBuilder.build(), settings);
        }
    }

    @Override
    public Serializable getConnectionSourcesConfigurationKey() {
        return Settings.SETTING_DATASOURCES;
    }
}
