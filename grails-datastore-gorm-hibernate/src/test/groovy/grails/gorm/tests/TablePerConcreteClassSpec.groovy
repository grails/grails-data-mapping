package grails.gorm.tests

import grails.persistence.Entity
import org.hibernate.Session
import java.sql.Connection

/**
 */
class TablePerConcreteClassSpec extends GormDatastoreSpec{

    void "Test that table per concrete class produces correct tables"() {
        expect:"The table structure is correct"
            Payment.withSession { Session session ->

                Connection conn = session.connection()

                conn.prepareStatement("select id, version, amount from payment").execute()
                conn.prepareStatement("select id, version, amount, credit_card_type from credit_card_payment").execute()
                conn.prepareStatement("select id, version, amount, currency from cash_payment").execute()
            }
    }

    void "Test that polymorphic queries work correctly with table per concrete class"() {
        given:"Some test data with subclasses"
            def p = new Payment(amount: 1.1)
            p.save()
            new CreditCardPayment(creditCardType: 'mastercard', amount: 110).save()
            new CreditCardPayment(creditCardType: 'amex', amount: 115).save()
            new CashPayment(currency: Currency.getInstance("USD"), amount: 50).save(flush:true)
            session.clear()

        expect:"The correct results from queries"
            Payment.count() == 4
            CreditCardPayment.count() == 2
            CashPayment.count() == 1

        when:"The subclasses are loaded"
            List<CreditCardPayment> creditCardPayments = CreditCardPayment.list()
            List<CashPayment> cashPayments = CashPayment.list()
            List<Payment> allPayments = Payment.list()

        then:"The results are correct"
            creditCardPayments.size() == 2
            creditCardPayments[0].creditCardType == 'mastercard'
            creditCardPayments[0].amount== 110
            creditCardPayments[1].creditCardType == 'amex'
            creditCardPayments[1].amount== 115
            cashPayments.size() == 1
            cashPayments[0].currency == Currency.getInstance("USD")
            allPayments.size() == 4

    }

    @Override
    List getDomainClasses() {
        [Payment, CreditCardPayment, CashPayment]
    }
}

@Entity
class Payment {
    Long id
    Long version
    Double amount

    static mapping = {
        tablePerConcreteClass true
    }
}
class CreditCardPayment extends Payment {
    String creditCardType
}
class CashPayment extends Payment {
    Currency currency
}
