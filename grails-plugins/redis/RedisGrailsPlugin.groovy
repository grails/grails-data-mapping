import org.grails.plugins.redis.RedisMappingContextFactoryBean
import org.grails.plugins.redis.RedisDatastoreFactoryBean

class RedisGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3.4 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def author = "Graeme Rocher"
    def authorEmail = "graeme.rocher@springsource.com"
    def title = "Redis GORM"
    def description = '''\\
A plugin that 
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/redis"

    def doWithSpring = {
        def redisConfig = grailsApplication.config?.grails?.redis
        datastoreMappingContext(RedisMappingContextFactoryBean) {
          grailsApplication = ref('grailsApplication')
          pluginManager = ref('pluginManager')
        }
        springDatastore(RedisDatastoreFactoryBean) {
          config = redisConfig
          mappingContext = ref("datastoreMappingContext")
        }
    }
}
