package org.grails.orm.hibernate

import grails.gorm.tests.Plant
import grails.gorm.tests.PlantCategory
import org.junit.Test

import static junit.framework.Assert.*

/**
 * Test for GRAILS-3178.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class CriteriaListDistinctTests extends AbstractGrailsHibernateTests {



    @Test
    void testListDistinct() {
        assertNotNull PlantCategory.newInstance(name:"leafy")
                                   .addToPlants(goesInPatch:true, name:"lettuce")
                                   .addToPlants(goesInPatch:true, name:"cabbage")
                                   .save(flush:true)

        assertNotNull PlantCategory.newInstance(name:"orange")
                                   .addToPlants(goesInPatch:true, name:"carrots")
                                   .addToPlants(goesInPatch:true, name:"pumpkin")
                                   .save(flush:true)

        assertNotNull PlantCategory.newInstance(name:"grapes")
                                   .addToPlants(goesInPatch:false, name:"red")
                                   .addToPlants(goesInPatch:false, name:"white")
                                   .save(flush:true)

        session.clear()

        def categories = PlantCategory.createCriteria().listDistinct {
            plants {
                eq('goesInPatch', true)
            }
            order('name', 'asc')
        }

        assertNotNull categories
        assertEquals 2, categories.size()
        assertEquals "leafy", categories[0].name
        assertEquals "orange", categories[1].name
    }

    @Test
    void testListDistinct2() {

        assertNotNull PlantCategory.newInstance(name:"leafy")
                                   .addToPlants(goesInPatch:true, name:"lettuce")
                                   .addToPlants(goesInPatch:true, name:"cabbage")
                                   .save(flush:true)

        assertNotNull PlantCategory.newInstance(name:"orange")
                                   .addToPlants(goesInPatch:true, name:"carrots")
                                   .addToPlants(goesInPatch:true, name:"pumpkin")
                                   .save(flush:true)

        assertNotNull PlantCategory.newInstance(name:"grapes")
                                   .addToPlants(goesInPatch:false, name:"red")
                                   .addToPlants(goesInPatch:true, name:"white")
                                   .save(flush:true)

        session.clear()

        def categories = PlantCategory.createCriteria().listDistinct {
            plants {
                eq('goesInPatch', true)
            }
            order('name', 'asc')
        }

        assertNotNull categories
        assertEquals 3, categories.size()
        assertEquals "grapes", categories[0].name
        assertEquals "leafy", categories[1].name
        assertEquals "orange", categories[2].name
    }

    @Override
    protected getDomainClasses() {
        [Plant, PlantCategory]
    }
}
