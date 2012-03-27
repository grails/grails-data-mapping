package grails.gorm.tests

import org.hibernate.FetchMode;
import grails.persistence.Entity

class JoinCriteriaSpec extends GormDatastoreSpec {

    @Override
    List getDomainClasses() {
        [AclClass, AclObjectIdentity]
    }

    def "check if a criteria join get the expected results"() {
        given:
        def aclc = new AclClass(className:'classname1').save(flush:true)
        def aclObjId = new AclObjectIdentity('aclClass':aclc, objectId:1L).save(flush:true)
        session.clear()

        when:
        def theAclClass = AclObjectIdentity.createCriteria().get {
            eq('objectId', 1L)
            aclClass { eq('className', 'classname1') }
        }

        then:
        theAclClass
    }
}

@Entity
class AclClass {
    Long id
    Long version
    String className

    @Override
    String toString() {
        "AclClass id $id, className $className"
    }
}

@Entity
class AclObjectIdentity {

    Long id
    Long version

    Long objectId
    AclClass aclClass

    @Override
    String toString() {
        "AclObjectIdentity id $id, aclClass $aclClass.className, " +
                "objectId $objectId, entriesInheriting $entriesInheriting"
    }
}


