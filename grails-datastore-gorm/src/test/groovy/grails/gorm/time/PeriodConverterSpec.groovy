package grails.gorm.time

import spock.lang.Shared
import spock.lang.Specification

import groovy.transform.Generated

import java.lang.reflect.Method
import java.time.Period

class PeriodConverterSpec extends Specification implements PeriodConverter {

    @Shared
    Period period

    void setupSpec() {
        period = Period.of(1941, 1, 5)
    }

    void "test convert to long"() {
        expect:
        convert(period) == 'P1941Y1M5D'
    }

    void "test convert from long"() {
        expect:
        convert('P1941Y1M5D') == period
    }

    void "test that all PeriodConverter trait methods are marked as Generated"() {
        expect: "all PeriodConverter methods are marked as Generated on implementation class"
        PeriodConverter.getMethods().each { Method traitMethod ->
            assert PeriodConverterSpec.class.getMethod(traitMethod.name, traitMethod.parameterTypes).isAnnotationPresent(Generated)
        }
    }
}
