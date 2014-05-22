package grails.gorm.tests

import com.datastax.driver.core.exceptions.InvalidQueryException


/**
 * Abstract base test for order by queries. Subclasses should do the necessary setup to configure GORM
 */
class OrderBySpec extends GormDatastoreSpec {
    
    void "Test order with criteria"() {
        given:            
            ["IPhone", "Samsung", "LG", "HTC", "Nokia", "Blackberry"].each {
                new SimpleWidget(category: "phone", name:it).save()
            }
            ["Alba", "Samsung", "Panasonic", "Toshiba"].each {
                new SimpleWidget(category: "tv", name:it).save()
            }

        when:
            def results = SimpleWidget.createCriteria().list {
                eq "category", "phone"
                order "name"
            }
        then:
            'Blackberry' == results[0].name
            'HTC' == results[1].name
            'IPhone' == results[2].name

        when:
            results = SimpleWidget.createCriteria().list {
                eq "category", "phone"
                order "name" , "desc"
            }

        then:
            'Samsung' == results[0].name
            'Nokia' == results[1].name
            'LG' == results[2].name
            
        when:
            results = SimpleWidget.createCriteria().list {
                'in' "category", ["phone", "tv"]
                order "name"
            }

        then:
            'Alba' == results[0].name
            'Blackberry' == results[1].name
            'HTC' == results[2].name
    }
    
    void "Test order by with list() method throw invalid query exception"() {
        given:
            ["IPhone", "Samsung", "LG", "HTC", "Nokia", "Blackberry"].each {
                new SimpleWidget(category: "phone", name:it).save()
            }

        when:
            def results = SimpleWidget.list(sort:"name")

        then:
            //order by only supported when the partition key restricted by an EQ or IN
            thrown InvalidQueryException
        
    }

    void "Test order by property name with dynamic finder"() {
        given:
             ["IPhone", "Samsung", "LG", "HTC", "Nokia", "Blackberry"].each {
                new SimpleWidget(category: "phone", name:it).save()
            }
            ["Alba", "Samsung", "Panasonic", "Toshiba"].each {
                new SimpleWidget(category: "tv", name:it).save()
            }

        when:
            def results = SimpleWidget.findAllByCategory("tv", [sort:"name"])

        then:
            'Alba' == results[0].name
            'Panasonic' == results[1].name
            'Samsung' == results[2].name

        when:
            results = SimpleWidget.findAllByCategory("tv", [sort:"name", order:"desc"])

        then:
            'Toshiba' == results[0].name
            'Samsung' == results[1].name
            'Panasonic' == results[2].name
    }
}
