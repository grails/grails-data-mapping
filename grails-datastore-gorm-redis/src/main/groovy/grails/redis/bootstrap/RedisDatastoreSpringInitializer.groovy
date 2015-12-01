package grails.redis.bootstrap

import groovy.transform.InheritConstructors
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.bootstrap.AbstractDatastoreInitializer
import org.grails.datastore.gorm.redis.bean.factory.RedisDatastoreFactoryBean
import org.grails.datastore.gorm.redis.bean.factory.RedisMappingContextFactoryBean
import org.grails.datastore.gorm.support.AbstractDatastorePersistenceContextInterceptor
import org.grails.datastore.gorm.support.DatastorePersistenceContextInterceptor
import org.springframework.beans.factory.support.BeanDefinitionRegistry
/**
 * For bootstrapping Redis
 *
 * @author Graeme Rocher
 * @since 5.0
 */
@InheritConstructors
class RedisDatastoreSpringInitializer extends AbstractDatastoreInitializer{

    @Override
    Closure getBeanDefinitions(BeanDefinitionRegistry beanDefinitionRegistry) {
        {->
            def callable = getCommonConfiguration(beanDefinitionRegistry, "redis")
            callable.delegate = delegate
            callable.call()


            redisDatastoreMappingContext(RedisMappingContextFactoryBean) {
                grailsApplication = ref('grailsApplication')
                defaultExternal = secondaryDatastore
            }

            redisDatastore(RedisDatastoreFactoryBean) {
                config = configuration
                mappingContext = ref("redisDatastoreMappingContext")
            }


            "org.grails.gorm.neo4j.internal.GORM_ENHANCER_BEAN-redis"(GormEnhancer, ref("redisDatastore")) { bean ->
                bean.initMethod = 'enhance'
                bean.destroyMethod = 'close'
                bean.lazyInit = false
                includeExternal = !secondaryDatastore
            }
        }
    }

    @Override
    protected Class<AbstractDatastorePersistenceContextInterceptor> getPersistenceInterceptorClass() {
        return DatastorePersistenceContextInterceptor
    }
}
