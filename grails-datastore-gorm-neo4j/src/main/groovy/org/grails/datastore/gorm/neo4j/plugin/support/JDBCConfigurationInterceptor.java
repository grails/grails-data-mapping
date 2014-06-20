package org.grails.datastore.gorm.neo4j.plugin.support;

import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.Properties;

/**
 * if dataSource.url contains a instance reference, we need change config to supply
 * the graphDatabaseService using the given instance name to config
 * It's crucial to have a dependency here to graphDatabaseService to force it's instantiation
 * before the DataSource.
 */
public class JDBCConfigurationInterceptor implements BeanFactoryPostProcessor {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    public static final String JDBC_NEO4J_PREFIX = "jdbc:neo4j:instance:";
    private GraphDatabaseService graphDatabaseService;
    private ConfigObject config;

    public ConfigObject getConfig() {
        return config;
    }

    public void setConfig(ConfigObject config) {
        this.config = config;
    }

    public GraphDatabaseService getGraphDatabaseService() {
        return graphDatabaseService;
    }

    public void setGraphDatabaseService(GraphDatabaseService graphDatabaseService) {
        this.graphDatabaseService = graphDatabaseService;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

        ConfigObject dataSourceConfig = (ConfigObject) config.get("dataSource");
        String url = (String) dataSourceConfig.get("url");
        log.info("dataSource url is {}", url); // e.g. jdbc:neo4j:instance:dummy
        if (url.startsWith(JDBC_NEO4J_PREFIX)) {
            String instanceName = url.substring(JDBC_NEO4J_PREFIX.length());

            Properties props = new Properties();
            props.put("dataSource.properties.dbProperties." + instanceName, graphDatabaseService);
            log.info("setting graphDb reference for instance {}", instanceName);
            config.merge(new ConfigSlurper().parse(props));
        }
    }
}
