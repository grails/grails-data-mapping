package org.grails.datastore.gorm.cassandra.plugin.support

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.grails.datastore.gorm.plugin.support.ApplicationContextConfigurer
import org.grails.datastore.mapping.cassandra.CassandraDatastore
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.data.cassandra.core.CassandraTemplate

class CassandraApplicationContextConfigurer extends ApplicationContextConfigurer {

    CassandraApplicationContextConfigurer() {
        super("Cassandra")
    }
    
    @Override
    public void configure(ConfigurableApplicationContext ctx) {       
        super.configure(ctx)
        
        GrailsApplication application = ctx.grailsApplication
        CassandraDatastore cassandraDatastore  = ctx.cassandraDatastore               
        
        for (clazz in application.controllerClasses) {
            addCassandraTemplate(application, cassandraDatastore, clazz)    
        }
        
        for (clazz in application.serviceClasses) {
            addCassandraTemplate(application, cassandraDatastore, clazz)
        }
    }
    
    def addCassandraTemplate = {application, cassandraDatastore, clazz ->
        CassandraTemplate cassandraTemplate = cassandraDatastore.cassandraTemplate        
        clazz.metaClass.getCassandraTemplate = {  cassandraTemplate }        
    }
}
