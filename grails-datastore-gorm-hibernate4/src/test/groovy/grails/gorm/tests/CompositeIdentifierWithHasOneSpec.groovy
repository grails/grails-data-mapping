package grails.gorm.tests

import grails.persistence.Entity

/**
 * Created by graemerocher on 13/02/14.
 */
class CompositeIdentifierWithHasOneSpec extends GormDatastoreSpec {

    void testCompositeIdentifierWithCustomColumnNames() {

        given:"A domain model with a composite id and hasOne"
            def it = new IntervalType(name:"Long").save()
            def iq = new IntervalQuantity(intervalType:it, quantity:5, description:"Goods")
            def di = new DeliveryInstruction(intervalType:it, intervalQuantity:iq, description:"Back door").save(flush:true)

            session.clear()

        when:"The domain is queried"
            di = DeliveryInstruction.list()[0]

        then:"It is valid"
            di != null
            di.description == "Back door"
            di.intervalType != null
            di.intervalType.name == "Long"
            di.intervalQuantity != null
            di.intervalQuantity.quantity == 5
            di.intervalQuantity.intervalType.name == "Long"
    }
    @Override
    List getDomainClasses() {
        [DeliveryInstruction, IntervalQuantity, IntervalType]
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
        table 'mail_interval_quantity'
        version false
        id composite:[ 'intervalType', 'quantity' ]
        columns {
            intervalType column:'message_code'
            quantity column:'recipient_code'
            description column:'type'
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
        table 'mail_delivery_instruction'
        version false
        id generator:'sequence', params:[sequence:'DELIVERY_INSTR_SEQ']
        columns {
            id column:'code'
            intervalType column:'interval_type_code'
            description column:'description'
        }
    }
}