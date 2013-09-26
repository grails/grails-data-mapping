package org.grails.datastore.gorm.neo4j

import org.grails.datastore.gorm.validation.constraints.UniqueConstraint
import org.grails.datastore.mapping.core.Session

import javax.persistence.FlushModeType

class UniqueConstraintPatcher {

    /**
     * there is a bug in {@link org.grails.datastore.gorm.validation.constraints.UniqueConstraint} (part of grails-datastore-gorm-VERSION.jar
     * with version <=1.1.9.
     * as a workaround we patch this using metaprogramming
     */
    void patch() {
        log.warn "patching of UniqueConstraint is required"
        UniqueConstraint.metaClass.withManualFlushMode = { Closure callable ->

            delegate.constraintOwningClass.withSession { Session s ->
                final flushMode = s.getFlushMode()
                try {
                    s.setFlushMode(FlushModeType.COMMIT)
                    callable.call(s)
                }
                finally {
                    s.setFlushMode(flushMode)
                }
            }
        }
        assert checkIfPatchWorks() == true
    }

    /**
     * check if {@UniqueConstraint#withManualFlushMode} works correctly
     * if buggy, flushMode is not reset to AUTO value
     */
    boolean checkIfPatchWorks() {
        boolean patchWorks = true
        FlushModeType flushModeType = FlushModeType.AUTO

        DummyOwningClass.session = [
                getFlushMode: {->
                    flushModeType
                },
                setFlushMode: { FlushModeType fmt ->
                    flushModeType = fmt
                }
        ] as Session

        def uniqueConstraint = new UniqueConstraint(null)
        uniqueConstraint.owningClass = DummyOwningClass
        uniqueConstraint.withManualFlushMode { s ->
            if (s.getFlushMode() != FlushModeType.COMMIT) {
                patchWorks = false
            }
        }
        return patchWorks
    }

    class DummyOwningClass {
        static Session session

        static def withSession(Closure closure) {
            closure.call(session)
        }
    }

}
