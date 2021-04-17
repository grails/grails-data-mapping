package grails.gorm.time

import spock.lang.Shared
import spock.lang.Specification

import groovy.transform.Generated

import java.lang.reflect.Method
import java.time.LocalTime

class LocalTimeConverterSpec extends Specification implements LocalTimeConverter {

    @Shared
    LocalTime localTime

    void setupSpec() {
        localTime = LocalTime.of(6,5,4,3)
    }

    void "test convert to long"() {
        expect:
        convert(localTime) == 21904000000003L
    }

    void "test convert from long"() {
        expect:
        convert(21904000000003L) == localTime
    }

    void "test that all LocalTimeConverter/TemporalConverter trait methods are marked as Generated"() {
        expect: "all LocalTimeConverter methods are marked as Generated on implementation class"
        LocalTimeConverter.getMethods().each { Method traitMethod ->
            assert LocalTimeConverterSpec.class.getMethod(traitMethod.name, traitMethod.parameterTypes).isAnnotationPresent(Generated)
        }

        and: "all TemporalConverter methods are marked as Generated on implementation class"
        TemporalConverter.getMethods().each { Method traitMethod ->
            assert LocalTimeConverterSpec.class.getMethod(traitMethod.name, traitMethod.parameterTypes).isAnnotationPresent(Generated)
        }
    }
}