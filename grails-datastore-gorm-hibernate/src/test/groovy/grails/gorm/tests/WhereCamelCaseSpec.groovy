package grails.gorm.tests

import grails.persistence.Entity
import org.grails.datastore.gorm.query.transform.ApplyDetachedCriteriaTransform
import spock.lang.Issue

@Issue('https://github.com/grails/grails-data-mapping/issues/532')
@ApplyDetachedCriteriaTransform
class WhereCamelCaseSpec extends GormDatastoreSpec{
    def "simple prop without alias on left side"() {
        new CamelCaseFoo(name: "foo", fooName: "foo").save()
        new CamelCaseBar(name: "bar", barName: "bar").save()

        when:
        CamelCaseFoo.where {
            def f = CamelCaseFoo
            exists CamelCaseBar.where {
                name == f.name
            }.id()
        }.list()

        then:
        notThrown Exception
    }

    def "camelCase prop without alias on left side"() {
        new CamelCaseFoo(name: "foo", fooName: "foo").save()
        new CamelCaseBar(name: "bar", barName: "bar").save()

        when:
        CamelCaseFoo.where {
            def f = CamelCaseFoo
            exists CamelCaseBar.where {
                barName == f.fooName
            }.id()
        }.list()

        then:
        notThrown Exception
    }

    def "camelCase prop with alias on left side"() {
        new CamelCaseFoo(fooName: "foo").save()
        new CamelCaseBar(barName: "bar").save()

        when:
        CamelCaseFoo.where {
            def f = CamelCaseFoo
            exists CamelCaseBar.where {
                def b = CamelCaseBar
                b.barName == f.fooName
            }.id()
        }.list()

        then:
        notThrown Exception
    }

    @Override
    List getDomainClasses() {
        [CamelCaseFoo, CamelCaseBar]
    }

}

@Entity
class CamelCaseFoo {
    String name
    String fooName
}
@Entity
class CamelCaseBar {
    String name
    String barName
}
