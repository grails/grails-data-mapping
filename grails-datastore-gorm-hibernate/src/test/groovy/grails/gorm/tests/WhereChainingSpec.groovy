package grails.gorm.tests

import grails.persistence.Entity
import org.grails.datastore.gorm.query.transform.ApplyDetachedCriteriaTransform

/**
 * Created by graemerocher on 08/04/14.
 */
@ApplyDetachedCriteriaTransform
class WhereChainingSpec extends GormDatastoreSpec{

    void "Test chained where queries work correctly"() {
        given:"Some test data"
            createTestData()
        when:"When a chained where query is used"
            println Company.twos
            def results = Company.twos.list()
        then:"The results are correct"
            results.size() == 2
            results.find { it.name == '12'}
            results.find { it.name == '21'}
    }

    void createTestData() {
        Company.withTransaction {
            new Company(name:"12").save()
            new Company(name:"21").save()
            new Company(name:"22").save()
            new Company(name:"23").save()
            new Company(name:"13").save()
            new Company(name:"14").save()
            new Company(name:"15").save()
        }

    }

    @Override
    List getDomainClasses() {
        [Company]
    }
}

@Entity
@ApplyDetachedCriteriaTransform
class Company {
    Long id
    Long version
    String name

    static ones = Company.where { name =~ '%1%'}
    static twos = ones.where { name =~ '%2%' }

}
