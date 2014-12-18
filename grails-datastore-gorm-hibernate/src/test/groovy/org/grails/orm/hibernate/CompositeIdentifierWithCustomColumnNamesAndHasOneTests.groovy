package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test


class CompositeIdentifierWithCustomColumnNamesAndHasOneTests extends AbstractGrailsHibernateTests {

    @Test
    void testCompositeIdentifierWithCustomColumnNames() {
        def it = IntervalType.newInstance(name:"Long").save()
        assert it != null

        def iq = IntervalQuantity.newInstance(intervalType:it, quantity:5, description:"Goods")
        assert iq != null

        def di = DeliveryInstruction.newInstance(intervalType:it, intervalQuantity:iq, description:"Back door").save(flush:true)
        assert di != null

        session.clear()

        di = DeliveryInstruction.list()[0]

        assert di != null
        assert di.description == "Back door"
        assert di.intervalType != null
        assert di.intervalType.name == "Long"
        assert di.intervalQuantity != null
        assert di.intervalQuantity.quantity == 5
        assert di.intervalQuantity.intervalType.name == "Long"
    }

    @Override
    protected getDomainClasses() {
        [IntervalQuantity, IntervalType, DeliveryInstruction]
    }
}
@Entity
class IntervalType {
    Long id
    Long version

    String name
}

@Entity
class IntervalQuantity implements Serializable {
    Long id
    Long version

    IntervalType intervalType
    int quantity
    String description
    DeliveryInstruction instructions

    static mapping = {
        table   'mail_interval_quantity'
        version false
        id composite:[ 'intervalType', 'quantity' ]
        columns {
            intervalType column:'message_code'
            quantity     column:'recipient_code'
            description  column:'type'
        }
    }
}

@Entity
class DeliveryInstruction {
    Long id
    Long version

    String description
    IntervalType intervalType
    IntervalQuantity intervalQuantity
    static hasOne = [intervalQuantity:IntervalQuantity]

    static mapping = {
        table   'mail_delivery_instruction'
        version false
        id      generator:'sequence', params:[sequence:'DELIVERY_INSTR_SEQ']
        columns {
            id               column:'code'
            intervalType     column:'interval_type_code'
            description      column:'description'
        }
    }
}
