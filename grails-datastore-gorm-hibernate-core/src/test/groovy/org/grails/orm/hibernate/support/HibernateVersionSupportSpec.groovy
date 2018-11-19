package org.grails.orm.hibernate.support

import spock.lang.Specification

/**
 * Created by graemerocher on 04/04/2017.
 */
class HibernateVersionSupportSpec extends Specification {

    void 'test hibernate version is at least'() {
        expect:
        !HibernateVersionSupport.isAtLeastVersion("5.4.0")
        HibernateVersionSupport.isAtLeastVersion("5.3.0")
    }
}
