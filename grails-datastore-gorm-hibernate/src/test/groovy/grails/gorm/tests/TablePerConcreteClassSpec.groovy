package grails.gorm.tests

import grails.persistence.Entity
import org.hibernate.Session
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import org.codehaus.groovy.grails.orm.hibernate.HibernateGormStaticApi
import org.codehaus.groovy.grails.orm.hibernate.HibernateDatastore

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

    void "Test that table per concrete class produces correct tables which parent is an abstract class"() {
        expect: "The table structure is correct"
            Payment.withSession { Session session ->

                Connection conn = session.connection()

                PreparedStatement stmt = conn.prepareStatement("select count(*) from INFORMATION_SCHEMA.tables where TABLE_NAME = ?")
                stmt.setString(1, "ABSTRACT_PAYMENT")
                ResultSet resultSet = stmt.executeQuery()

                assert resultSet.next()
                assert !resultSet.getInt(1)

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

    void "Test that polymorphic queries work correctly with table per concrete class which parent is an abstract class"() {
        given: "Some test data with subclasses"
            new DebitCardPayment(bank: "FooBank", amount: 23).save()
            new DebitCardPayment(bank: "BarBank", amount: 102).save()
            new OnlinePayment(service: "paypal", amount: 50).save(flush: true)
            session.clear()

        expect: "The correct results from queries"
            AbstractPayment.count() == 3
            DebitCardPayment.count() == 2
            OnlinePayment.count() == 1

        when: "The subclasses are loaded"
            List<DebitCardPayment> debitCardPayments = DebitCardPayment.list()
            List<OnlinePayment> onlinePayments = OnlinePayment.list()
            List<AbstractPayment> allPayments = AbstractPayment.list()

        then: "The results are correct"
            debitCardPayments.size() == 2
            debitCardPayments[0].bank == "FooBank"
            debitCardPayments[0].amount == 23
            debitCardPayments[1].bank == "BarBank"
            debitCardPayments[1].amount == 102
            onlinePayments.size() == 1
            onlinePayments[0].service == "paypal"
            allPayments.size() == 3
    }

    @Override
    List getDomainClasses() {
        [Payment, CreditCardPayment, CashPayment, AbstractPayment, DebitCardPayment, OnlinePayment]
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
@Entity
class CreditCardPayment extends Payment {
    String creditCardType
}
@Entity
class CashPayment extends Payment {
    Currency currency
}

@Entity
abstract class AbstractPayment {
    Long id
    Long version
    Double amount

    static mapping = {
        tablePerConcreteClass true
    }
}
@Entity
class DebitCardPayment extends AbstractPayment {
    String bank
}
@Entity
class OnlinePayment extends AbstractPayment {
    String service
}
