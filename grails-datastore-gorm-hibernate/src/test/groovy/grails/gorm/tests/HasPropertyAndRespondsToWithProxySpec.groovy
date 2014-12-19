package grails.gorm.tests

import grails.persistence.Entity
import org.grails.orm.hibernate.cfg.GrailsHibernateUtil

/**
 * Created by graemerocher on 12/02/14.
 */
class HasPropertyAndRespondsToWithProxySpec extends GormDatastoreSpec {

    void "Test hasProperty method with a proxy"() {
        given:"A domain model with a proxy"
            def rt = new SubclassRespondsTo(name:"Bob")
            def hp = new HasProperty(one:rt)
            rt.save()
            hp.save(flush:true)

            session.clear()

        when:"The domain is retrieved"
            hp = HasProperty.get(1)

        then:"The association should be a proxy"
            !GrailsHibernateUtil.isInitialized(hp, "one")

        when:"The proxy is retrieved"
            def proxy = GrailsHibernateUtil.getAssociationProxy(hp, "one")

        then:"should have a name property!"
            proxy.hasProperty("name")
    }

    void "Test respondsTo with proxy"() {
        given:"A domain model with a proxy"
            def rt = new SubclassRespondsTo(name:"Bob")
            def hp = new HasProperty(one:rt)
            rt.save()
            hp.save(flush:true)

            session.clear()

        when:"The domain is retrieved"
            hp = HasProperty.get(1)

        then:"The association should be a proxy"
            !GrailsHibernateUtil.isInitialized(hp, "one")

        when:"The proxy is retrieved"
            def proxy = GrailsHibernateUtil.getAssociationProxy(hp, "one")

        then:"The respondTo method works as expected"
            proxy.respondsTo("foo")
            proxy.respondsTo("bar", String)
    }

    @Override
    List getDomainClasses() {
        [RespondsTo, SubclassRespondsTo, HasProperty]
    }
}
@Entity
class HasProperty {
    Long id
    Long version

    RespondsTo one
}

@Entity
class RespondsTo {
    Long id
    Long version
}

@Entity
class SubclassRespondsTo extends RespondsTo {
    String name
    def foo() { "good" }
    def bar(String i) { i }
}