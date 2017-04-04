package org.grails.orm.hibernate.support

import spock.lang.Specification

/**
 * Created by graemerocher on 04/04/2017.
 */
class HibernateVersionSupportSpec extends Specification {

    void 'test hibernate version is at least'() {
        expect:
        !HibernateVersionSupport.isAtLeastVersion("5.2.0")
        HibernateVersionSupport.isAtLeastVersion("4.3.1")
        HibernateVersionSupport.isAtLeastVersion("4.3.1.Final")
        HibernateVersionSupport.isAtLeastVersion("4.3.11.Final")
    }
}
