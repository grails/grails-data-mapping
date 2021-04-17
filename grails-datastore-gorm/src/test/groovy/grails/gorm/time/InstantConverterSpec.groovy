package grails.gorm.time

import spock.lang.Shared
import groovy.transform.Generated
import spock.lang.Specification

import java.lang.reflect.Method
import java.time.Instant

class InstantConverterSpec extends Specification implements InstantConverter {

    @Shared
    Instant instant

    void setupSpec() {
        instant = Instant.ofEpochMilli(100)
    }

    void "test convert to long"() {
        expect:
        convert(instant) == 100L
    }

    void "test convert from long"() {
        expect:
        convert(100L) == instant
    }

    void "test that all InstantConverter trait methods are marked as Generated"() {
        expect: "all InstantConverter methods are marked as Generated on implementation class"
        InstantConverter.getMethods().each { Method traitMethod ->
            assert InstantConverterSpec.class.getMethod(traitMethod.name, traitMethod.parameterTypes).isAnnotationPresent(Generated)
        }
    }
}
