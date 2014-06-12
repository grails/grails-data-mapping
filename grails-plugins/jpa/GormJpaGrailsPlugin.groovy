import org.grails.datastore.gorm.jpa.plugin.support.*

class GormJpaGrailsPlugin {
    // the plugin version
    def version = "1.0.0.M1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.0.0 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def title = "Gorm Jpa Plugin" // Headline display name of the plugin
    def author = "Graeme Rocher"
    def authorEmail = "grocher@vmware.com"
    def description = '''\
Implementation of GORM for JPA
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/gorm-jpa"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
    def organization = [ name: "SpringSource", url: "http://www.springsource.com/" ]

    // Any additional developers beyond the author specified above.
    def developers = [ [ name: "Graeme Rocher", email: "grocher@vmware.com" ]]

    // Location of the plugin's issue tracker.
    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPGORMJPA" ]

    // Online location of the plugin's browseable source code.
    def scm = [ url: "https://github.com/grails/grails-data-mapping/tree/master/grails-plugins" ]

    def observe = ['services', 'domainClass']
    
    def doWithSpring = new JpaSpringConfigurer().getConfiguration()

    def doWithDynamicMethods = { ctx ->
        def datastore = ctx.jpaDatastore
        def transactionManager = ctx.getBean(org.springframework.orm.jpa.JpaTransactionManager)
        def methodsConfigurer = new JpaMethodsConfigurer(datastore, transactionManager)    
        def foe = application?.config?.grails?.gorm?.failOnError        
        methodsConfigurer.failOnError = foe instanceof Boolean ? foe : false
        methodsConfigurer.configure()
    }

    def onChange = { event ->
        if(event.ctx) {
            new JpaOnChangeHandler(event.ctx.jpaDatastore, event.ctx.jpaTransactionManager).onChange(delegate, event)            
        }
    }   
}
