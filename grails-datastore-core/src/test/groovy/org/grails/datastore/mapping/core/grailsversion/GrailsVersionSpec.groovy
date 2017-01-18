package org.grails.datastore.mapping.core.grailsversion

import spock.lang.Specification

/**
 * Created by jameskleeh on 1/17/17.
 */
class GrailsVersionSpec extends Specification {

    void "test compareTo"() {
        expect:
        new GrailsVersion(greater) > new GrailsVersion(lesser)

        where:
        greater | lesser
        "3.0.0" | "2.99.99.BUILD-SNAPSHOT"
        "3.0.0" | "2.99.99"
        "3.0.1" | "3.0.1.BUILD-SNAPSHOT"
        "3.1.2" | "3.1.1"
        "3.2.2" | "3.1.2"
        "4.1.1" | "3.1.1"
        "3.0.0.RC2" | "3.0.0.RC1"
        "3.0.0.M3" | "3.0.0.M2"
        "3.0.0.RC1" | "3.0.0.M9"
        "3.0.0.BUILD-SNAPSHOT" | "3.0.0.RC9"
    }

    void "test compareTo equal"() {
        expect:
        new GrailsVersion(left) == new GrailsVersion(right)

        where:
        left    | right
        "3.0.0" | "3.0.0"
        "3.0.0.M2" | "3.0.0.M2"
        "3.0.0.RC2" | "3.0.0.RC2"
        "3.0.0.BUILD-SNAPSHOT" | "3.0.0.BUILD-SNAPSHOT"
    }

    void "test illegal argument to constructor"() {
        when:
        new GrailsVersion("x")

        then:
        thrown(IllegalArgumentException)

        when:
        new GrailsVersion("3.1.x")

        then:
        thrown(NumberFormatException)

        when:
        new GrailsVersion("3.x.1")

        then:
        thrown(NumberFormatException)

        when:
        new GrailsVersion("x.1.1")

        then:
        thrown(NumberFormatException)

        when:
        new GrailsVersion("3.1.1.Mu")

        then:
        thrown(IllegalArgumentException)

        when:
        new GrailsVersion("3.1.1.RCu")

        then:
        thrown(IllegalArgumentException)
    }

}