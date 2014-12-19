package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

class MappedByMergedWithInheritedValueTests extends AbstractGrailsHibernateTests{

    // test for GRAILS-6328
    @Test
    void testMappedByMergedWithInheritedValue() {

        def sd = SpecialDocument.newInstance(name:"My Doc", specialStatus: "special")

        sd.addToToRole(roleName:"To Role", toDocument:SpecialDocument.newInstance(name:"To Doc", specialStatus: "special").save())
        sd.addToFromRole(roleName:"From Role", fromDocument:SpecialDocument.newInstance(name:"From Doc", specialStatus: "special").save())

        sd.save(flush:true)
        assert !sd.errors.hasErrors()

        session.clear()

        sd = SpecialDocument.get(sd.id)

        assert sd != null
        assert sd.toRole.size() == 1
        assert sd.fromRole.size() == 1
    }

    @Override
    protected getDomainClasses() {
        [Document, DocDocRole, SpecialDocument]
    }
}

@Entity
class Document {
    Long id
    Long version

    String name

    Set toRole
    Set fromRole
    static hasMany = [
            toRole: DocDocRole,
            fromRole: DocDocRole
    ]

    static mappedBy = [
            toRole: 'fromDocument',
            fromRole: 'toDocument'
    ]
}

@Entity
class DocDocRole {
    Long id
    Long version

    String roleName

    Document fromDocument
    Document toDocument

    static belongsTo = [
            Document
    ]

    static mappedBy = [
            fromDocument: 'toRole',
            toDocument: 'fromRole'
    ]
}

@Entity
class SpecialDocument extends Document {
    String specialStatus

    static mappedBy = [
            //some other things
    ]
}