package grails.redis.bootstrap

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
class RedisDatastoreSpringInitializer extends AbstractDatastoreInitializer{

    RedisDatastoreSpringInitializer(Collection<Class> persistentClasses) {
        super(persistentClasses)
    }

    RedisDatastoreSpringInitializer(Class... persistentClasses) {
        super(persistentClasses)
    }

    RedisDatastoreSpringInitializer(Map configuration, Collection<Class> persistentClasses) {
        super(configuration, persistentClasses)
    }

    RedisDatastoreSpringInitializer(Map configuration, Class... persistentClasses) {
        super(configuration, persistentClasses)
    }

    @Override
    Closure getBeanDefinitions(BeanDefinitionRegistry beanDefinitionRegistry) {
        {->
            def callable = getCommonConfiguration(beanDefinitionRegistry)
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

            callable = getAdditionalBeansConfiguration(beanDefinitionRegistry, "redis")
            callable.delegate = delegate
            callable.call()


            "org.grails.gorm.neo4j.internal.GORM_ENHANCER_BEAN-redis"(GormEnhancer, ref("redisDatastore"), ref("redisTransactionManager")) { bean ->
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
