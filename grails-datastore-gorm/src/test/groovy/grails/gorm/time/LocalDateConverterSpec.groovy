package grails.gorm.time

import spock.lang.Shared
import spock.lang.Specification

import groovy.transform.Generated

import java.lang.reflect.Method
import java.time.LocalDate

class LocalDateConverterSpec extends Specification implements LocalDateConverter {

    @Shared
    LocalDate localDate

    void setupSpec() {
        localDate = LocalDate.of(1941, 1, 5)
    }

    void "test convert to long"() {
        expect:
        convert(localDate) == -914803200000L
    }

    void "test convert from long"() {
        expect:
        convert(-914803200000L) == localDate
    }

    void "test that all LocalDateConverter/TemporalConverter trait methods are marked as Generated"() {
        expect: "all LocalDateConverter methods are marked as Generated on implementation class"
        LocalDateConverter.getMethods().each { Method traitMethod ->
            assert LocalDateConverterSpec.class.getMethod(traitMethod.name, traitMethod.parameterTypes).isAnnotationPresent(Generated)
        }

        and: "all TemporalConverter methods are marked as Generated on implementation class"
        TemporalConverter.getMethods().each { Method traitMethod ->
            assert LocalDateConverterSpec.class.getMethod(traitMethod.name, traitMethod.parameterTypes).isAnnotationPresent(Generated)
        }
    }
}
