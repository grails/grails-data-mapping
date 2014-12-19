package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

class CompositeIdentifierWithCustomColumnNamesTests extends AbstractGrailsHibernateTests {

    @Test
    void testCompositeIdentifierWithCustomColumnNames() {
        def it = CompositeIdentifierWithCustomColumnNamesIntervalType.newInstance(name:"Long").save()
        assert it != null

        def iq = CompositeIdentifierWithCustomColumnNamesIntervalQuantity.newInstance(intervalType:it, quantity:5, description:"Goods").save()
        assert iq != null

        def di = CompositeIdentifierWithCustomColumnNamesDeliveryInstruction.newInstance(intervalType:it, intervalQuantity:iq, description:"Back door").save(flush:true)
        assert di != null

        session.clear()

        di = CompositeIdentifierWithCustomColumnNamesDeliveryInstruction.list()[0]

        assert di != null
        assert di.description == "Back door"
        assert di.intervalType != null
        assert di.intervalType.name == "Long"
        assert di.intervalQuantity != null
        assert di.intervalQuantity.quantity == 5
        assert di.intervalQuantity.intervalType.name == "Long"

        def rs = session.connection().createStatement().executeQuery('select interval_type, interval_quantity from mail_delivery_instruction')
        assert rs.next()
    }
    @Override
    protected getDomainClasses() {
        [CompositeIdentifierWithCustomColumnNamesIntervalQuantity, CompositeIdentifierWithCustomColumnNamesIntervalType, CompositeIdentifierWithCustomColumnNamesDeliveryInstruction]
    }
}
@Entity
class CompositeIdentifierWithCustomColumnNamesIntervalType {
    Long id
    Long version

    String name
}

@Entity
class CompositeIdentifierWithCustomColumnNamesIntervalQuantity implements Serializable {

    Long id
    Long version

    CompositeIdentifierWithCustomColumnNamesIntervalType intervalType
    int quantity
    String description

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
class CompositeIdentifierWithCustomColumnNamesDeliveryInstruction {

    Long id
    Long version

    String description
    CompositeIdentifierWithCustomColumnNamesIntervalType intervalType
    CompositeIdentifierWithCustomColumnNamesIntervalQuantity intervalQuantity

    static mapping = {
        table   'mail_delivery_instruction'
        version false
        id      generator:'sequence', params:[sequence:'DELIVERY_INSTR_SEQ']
        columns {
            id               column:'code'
            intervalType     column:'interval_type_code'
            intervalQuantity {
                column name:'interval_type'
                column name:'interval_quantity'
            }
            description      column:'description'
        }
    }
}