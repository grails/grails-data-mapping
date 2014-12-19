package org.grails.orm.hibernate

import grails.gorm.tests.Plant
import org.junit.Test

/**
 * Tests the delete method.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class DeleteMethodTests extends AbstractGrailsHibernateTests {

    @Test
    void testDeleteAndFlush() {
        def plant = new Plant()
        plant.name = "Cabbage"
        plant.save()

        plant = Plant.get(1)
        plant.delete(flush:true)

        plant = Plant.get(1)
        assert plant == null
    }

    @Override
    protected getDomainClasses() {
        [Plant]
    }
}
