package grails.gorm.tests

import grails.persistence.Entity
import spock.lang.Issue

/**
 */
class MultipleProjectionSpec extends GormDatastoreSpec{

    @Issue('GRAILS-10469')
    void "Test that multiple projections in criteria return the correct results"() {
        given:"A number of domain classes"
            def now = new Date()
            new User(name: "Bob", createTime: now).save()
            new User(name: "Bob", createTime: now).save(flush:true)

        when:"A criteria query with projections is executed"
            def results = User.createCriteria().list {
                projections {
                    distinct('id')
                    property('createTime')
                }
                order 'id', 'asc'
            }

            println results
        then:"The results are correct"
            results.size() == 2
            results[0].size() == 2
            results[0][0] == 1L
            results[0][1] == now
            results[1].size() == 2
            results[1][0] == 2L
            results[1][1] == now

    }

    @Override
    List getDomainClasses() {
        [User]
    }
}

@Entity
class User {
    Long id
    Long version
    String name
    Date createTime
}
