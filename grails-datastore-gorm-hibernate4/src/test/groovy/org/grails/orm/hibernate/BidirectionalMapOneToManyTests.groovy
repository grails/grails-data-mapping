package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test
import static junit.framework.Assert.*
/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Dec 4, 2007
 */
class BidirectionalMapOneToManyTests extends AbstractGrailsHibernateTests {

    @Override
    protected getDomainClasses() {
        [StockLocation, Stockpile]
    }

    @Test
    void testModel() {
        def locClass = ga.getDomainClass(StockLocation.name)
        def spClass = ga.getDomainClass(Stockpile.name)

        assert locClass.getPropertyByName("stockpiles").association
        assert locClass.getPropertyByName("stockpiles").bidirectional
        assert locClass.getPropertyByName("stockpiles").oneToMany

        assert spClass.getPropertyByName("stockLocation").association
        assert spClass.getPropertyByName("stockLocation").bidirectional
        assert spClass.getPropertyByName("stockLocation").manyToOne
    }

    @Test
    void testUpdateBidiMap() {
        def sl = new StockLocation()

        sl.stockpiles = [one: new Stockpile(product:"MacBook", quantity:1.1 as Float)]

        assertNotNull sl.save(flush:true)

        session.clear()

        sl = StockLocation.get(1)
        assertNotNull sl

        assertEquals 1, sl.stockpiles.size()

        sl.stockpiles.two = new Stockpile(product:"MacBook Pro", quantity:2.3 as Float)
        sl.save(flush:true)

        session.clear()

        sl = StockLocation.get(1)
        assertNotNull sl

        assertEquals 2, sl.stockpiles.size()
    }
}

@Entity
class StockLocation {

    Long id
    Long version
    Map stockpiles

    static hasMany = [stockpiles:Stockpile]
}

@Entity
class Stockpile {
    Long id
    Long version
    String product
    Float quantity
    StockLocation stockLocation

    static constraints = {
        stockLocation(nullable:true)
    }
}