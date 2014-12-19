package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

class DefaultIndexDefinitionTests extends AbstractGrailsHibernateTests{

    @Test
    void testDefaultIndex() {
        def did = DefaultIndexDefinition.newInstance(name:"Bob").save()
        assert did != null
    }

    @Override
    protected getDomainClasses() {
        [DefaultIndexDefinition]
    }
}
@Entity
class DefaultIndexDefinition {
    Long id
    Long version

    String name
    static mapping = {
        name index:true
    }
}

