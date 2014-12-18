package org.codehaus.groovy.grails.orm.hibernate

import grails.core.GrailsDomainClass
import grails.persistence.Entity
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainBinder
import org.hibernate.type.YesNoType

import static junit.framework.Assert.*
import org.junit.Test

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class MappingDefaultsTests extends AbstractGrailsHibernateTests {

    @Override
    protected ConfigObject getConfig() {
        new ConfigSlurper().parse('''
grails.gorm.default.mapping = {
   cache true
   id generator:'sequence\'
   'user-type'(type: org.hibernate.type.YesNoType, class: Boolean)

}
grails.gorm.default.constraints = {
   '*'(nullable:true, size:1..20)
   test blank:false
   another email:true
}
''')
    }

    @Override
    protected getDomainClasses() {
        [MappingDefaults]
    }

    @Test
    void testGlobalUserTypes() {
        GrailsDomainClass domain = ga.getDomainClass(MappingDefaults.name)
        def mapping = new GrailsDomainBinder().getMapping(domain)

        assertEquals YesNoType, mapping.userTypes[Boolean]

        def i = domain.clazz.newInstance(name:"helloworld", test:true)
        assertNotNull "should have saved instance", i.save(flush:true)

        session.clear()
        def rs = session.connection().prepareStatement("select test from mapping_defaults").executeQuery()
        rs.next()
        assertEquals "Y", rs.getString("test")
    }

}

@Entity
class MappingDefaults {
    Long id
    Long version

    String name
    Boolean test

    static constraints = {
        name(shared:"test")
    }
}