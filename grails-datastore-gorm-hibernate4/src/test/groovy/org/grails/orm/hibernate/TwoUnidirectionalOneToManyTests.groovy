package org.grails.orm.hibernate

import grails.persistence.Entity

import org.junit.Test


class TwoUnidirectionalOneToManyTests extends AbstractGrailsHibernateTests {

    @Test
    void testTwoUniOneToManys() {
        def mailing = ga.getDomainClass(Mailing.name).newInstance()
        def recipient = ga.getDomainClass(Recipient.name).newInstance()
        def doc1 = ga.getDomainClass(MailingDocument.name).newInstance()
        def doc2 = ga.getDomainClass(MailingDocument.name).newInstance()

        doc1.filename = "file1.txt"
        doc2.filename = "file2.txt"

        mailing.addToDocuments(doc1)
        mailing.save(true)

        recipient.addToDocuments(doc2)
        recipient.save(true)
    }

    @Override
    protected getDomainClasses() {
        [Mailing, Recipient, MailingDocument]
    }
}

@Entity
class Mailing {
    Long id
    Long version
    Set documents
    static hasMany = [documents:MailingDocument]
}
@Entity
class Recipient {
    Long id
    Long version
    Set documents
    static hasMany = [documents:MailingDocument]
}
@Entity
class MailingDocument {
    Long id
    Long version
    String filename
}

