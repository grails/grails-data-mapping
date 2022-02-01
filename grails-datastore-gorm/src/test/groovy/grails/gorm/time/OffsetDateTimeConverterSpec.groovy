package grails.gorm.time

import spock.lang.Shared
import spock.lang.Specification

import groovy.transform.Generated

import java.lang.reflect.Method
import java.time.*

class OffsetDateTimeConverterSpec extends Specification implements OffsetDateTimeConverter {

    @Shared
    OffsetDateTime offsetDateTime

    void setupSpec() {
        TimeZone.default = TimeZone.getTimeZone("America/Los_Angeles")
        LocalTime localTime = LocalTime.of(6,5,4,3)
        LocalDate localDate = LocalDate.of(1941, 1, 5)
        LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime)
        offsetDateTime = OffsetDateTime.of(localDateTime, ZoneOffset.ofHours(-6))
    }

    void "test convert to long"() {
        expect:
        convert(offsetDateTime) == -914759696000L
    }

    void "test convert from long"() {
        given:
        OffsetDateTime converted = convert(-914759696000L)

        expect:
        converted == offsetDateTime.withNano(0).withOffsetSameInstant(ZoneOffset.ofHours(-7)) ||
                converted == offsetDateTime.withNano(0).withOffsetSameInstant(ZoneOffset.ofHours(-8))
    }

    void "test that all OffsetDateTimeConverter/TemporalConverter trait methods are marked as Generated"() {
        expect: "all OffsetDateTimeConverter methods are marked as Generated on implementation class"
        OffsetDateTimeConverter.getMethods().each { Method traitMethod ->
            assert OffsetDateTimeConverterSpec.class.getMethod(traitMethod.name, traitMethod.parameterTypes).isAnnotationPresent(Generated)
        }

        and: "all TemporalConverter methods are marked as Generated on implementation class"
        TemporalConverter.getMethods().each { Method traitMethod ->
            assert OffsetDateTimeConverterSpec.class.getMethod(traitMethod.name, traitMethod.parameterTypes).isAnnotationPresent(Generated)
        }
    }
}