package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

import java.sql.ResultSet

import static junit.framework.Assert.*
import org.junit.Test


class MappingDefinitionInheritedBySubclassTests extends AbstractGrailsHibernateTests {


    @Test
    void testMappingInheritance() {

        MappingDefinitionInheritedChild.newInstance(name:"Fred", active:true).save()
        MappingDefinitionInheritedChild.newInstance(name:"Bob", active:false).save()
        MappingDefinitionInheritedChild.newInstance(name:"Eddie", active:true).save()

        session.clear()
        def results = MappingDefinitionInheritedChild.list()

        assert results.size() == 3
        assert results[0].name == "Bob"
        assert results[1].name == "Eddie"
        assert results[2].name == "Fred"

        ResultSet rs = session.connection().createStatement().executeQuery("select active from mapping_definition_inherited_parent where name = 'Bob'")

        assert rs.next() == true
        assert rs.getString("active") == "N"
    }

    @Override
    protected getDomainClasses() {
        [MappingDefinitionInheritedParent, MappingDefinitionInheritedChild]
    }
}

@Entity
abstract class MappingDefinitionInheritedParent {
    Long id
    Long version

    Boolean active
    static mapping = {
        active type: 'yes_no'
    }
}

@Entity
class MappingDefinitionInheritedChild extends MappingDefinitionInheritedParent {
    String name

    static mapping = {
        sort 'name'
    }
}