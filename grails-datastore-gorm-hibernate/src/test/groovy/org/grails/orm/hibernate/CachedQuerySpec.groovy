package org.grails.orm.hibernate

import grails.gorm.DetachedCriteria
import grails.gorm.tests.Plant
import org.grails.datastore.mapping.query.Query
import spock.lang.Issue

/**
 * Created by graemerocher on 27/06/16.
 */
@Issue('https://github.com/grails/grails-data-mapping/issues/724')
class CachedQuerySpec extends GormSpec {

    void "test cache query argument with detached criteria"() {
        when:"A where query is created"
        DetachedCriteria criteria = Plant.where {
            name == "Carrot"
        }
        criteria = criteria.cache(false)


        then:"The query is not cached"
        !criteria.withPopulatedQuery( [:], {} ) { Query query ->
            query.@queryCache
        }
    }

    void "test cache query argument with dynamic finders criteria"() {
        when:"A where query is created"
        Query query = session.createQuery(Plant)
        query.cache(false)

        then:"The query is not cacheable"
        !query.@criteria.@cacheable
    }

    @Override
    List getDomainClasses() {
        [Plant]
    }
}
