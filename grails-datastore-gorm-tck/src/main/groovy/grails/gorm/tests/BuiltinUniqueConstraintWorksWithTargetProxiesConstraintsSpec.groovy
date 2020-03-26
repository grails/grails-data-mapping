package grails.gorm.tests

import grails.persistence.Entity
import org.grails.datastore.mapping.proxy.EntityProxy

class BuiltinUniqueConstraintWorksWithTargetProxiesConstraintsSpec extends GormDatastoreSpec {

    void "modified domains object works as expected"() {
        given: "I have a Domain OBJECT"
        final Referenced object = new Referenced(name: "object").save(failOnError: true)
        assert !(object instanceof EntityProxy)
        
        and: "I have another Domain OBJECT with the same name"
        final Referenced another = new Referenced(name: "object")
        assert !(object instanceof EntityProxy)

        when: "I try to validate the another object"
        another.validate()

        then: "another should have an error on name because it is duplicated"
        another.hasErrors()
        another.errors.hasFieldErrors("name")
        another.errors.getFieldError("name").codes.contains("unique.name")

        cleanup:
        object?.delete(flush: true)
    }

    void "modified Referenced domains object works as expected"() {
        given: "I have a Domain OBJECT"
        final Referenced object = new Referenced(name: "object").save(failOnError: true)
        assert !(object instanceof EntityProxy)
        and: "a root referencing it"
        final Root parent = new Root(ref: object).save(failOnError: true)
        assert !(parent instanceof EntityProxy)

        and: "I have another Domain OBJECT with the same name"
        final Referenced anotherReferenced = new Referenced(name: "object")
        assert !(object instanceof EntityProxy)
        final Root anotherRoot = new Root(ref: anotherReferenced)
        assert !(parent instanceof EntityProxy)

        when: "I try to validate the another object"
        anotherRoot.validate()

        then: "another should have an error on name because it is duplicated"
        anotherRoot.hasErrors()
        anotherRoot.errors.hasFieldErrors("Referenced.name")
        anotherRoot.errors.getFieldError("Referenced.name").codes.contains("unique.name")

        cleanup:
        object?.delete(flush: true)
        parent?.delete(flush: true)
    }

    void "unmodified Referenced proxies object doesnt fail unique constraint checking"() {
        given: "I have a Domain OBJECT"
        Long ReferencedId, parentId
        Root.withNewSession {
            Root.withNewTransaction {
                final Referenced object = new Referenced(name: "object").save(failOnError: true)
                final Root parent = new Root(ref: object).save(failOnError: true)

                ReferencedId = object.id
                parentId = parent.id
            }
        }
        and:
        int tries = 20
        while (!Root.exists(parentId) && !Referenced.exists(ReferencedId) && tries-- > 0) {
            sleep(50)
        }

        and: "I access the parent, forcing the Referenced to be initialized"

        def parent = Root.findAll()[0]
        assert parent.ref instanceof EntityProxy
        parent.ref.name == "object"

        when: "I try to validate the the parent (which then tries to validate the Referenced)"
        parent.validate()

        then: "parent.Referenced should not have any errors!"
        !parent.hasErrors()
        !parent.errors.hasFieldErrors("Referenced.name")
        !parent.errors.getFieldError("Referenced.name").codes.contains("unique.name")

        cleanup:

        Root.withNewSession {
            Root.withNewTransaction {
                Referenced.get(ReferencedId)?.delete(flush: true)
                Root.get(parentId)?.delete(flush: true)
            }
        }
        tries = 20
        while (Root.exists(parentId) && Referenced.exists(ReferencedId) && tries-- > 0) {
            sleep(50)
        }
    }

    @Override
    List getDomainClasses() {
        [Referenced, Root]
    }
}

@Entity
class Referenced {

    String name

    static constraints = {
        name nullable: false, unique: true
    }
}

@Entity
class Root {

    Referenced ref

    static constraints = {
        ref nullable: false
    }

    static mapping = {
        ref lazy: false
        id generator: 'snowflake'
    }
}