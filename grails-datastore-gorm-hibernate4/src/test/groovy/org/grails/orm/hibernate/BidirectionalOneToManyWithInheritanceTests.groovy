package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*
/**
 * test for GRAILS-2734.
 *
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Apr 9, 2008
 */
class BidirectionalOneToManyWithInheritanceTests extends AbstractGrailsHibernateTests {


    @Test
    void testBidirectionalOneToManyWithInheritance() {

        def doc = new Documentation()

        assertNotNull doc.addToConfigurationItems(new ChangeRequest())
                         .addToConfigurationItems(new Documentation())
                         .save(flush:true)

        session.clear()

        doc = Documentation.get(1)
        assertEquals 2,doc.configurationItems.size()
    }

    @Override
    protected getDomainClasses() {
        [ConfigurationItem, Documentation, ChangeRequest]
    }
}

@Entity
class ConfigurationItem {
    Long version
    Long id
    ConfigurationItem parent

    Set configurationItems

    static hasMany = [configurationItems: ConfigurationItem]
    static mappedBy = [configurationItems: 'parent']
    static belongsTo = [ConfigurationItem]
    static constraints = {
        parent(nullable: true)
    }

    static mapping = {
        table 'configuration_item'
        columns {
            parent lazy: false, column: 'bom'
        }
    }
}

@Entity
class Documentation extends ConfigurationItem{
    Long id
    Long version
}

@Entity
class ChangeRequest extends ConfigurationItem{
    Long id
    Long version
}