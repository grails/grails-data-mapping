package grails.gorm.time

import spock.lang.Shared
import spock.lang.Specification

import groovy.transform.Generated

import java.lang.reflect.Method
import java.time.*

class ZonedDateTimeConverterSpec extends Specification implements ZonedDateTimeConverter {

    @Shared
    ZonedDateTime zonedDateTime

    void setupSpec() {
        TimeZone.default = TimeZone.getTimeZone("America/Los_Angeles")
        LocalTime localTime = LocalTime.of(6,5,4,3)
        LocalDate localDate = LocalDate.of(1941, 1, 5)
        LocalDateTime localDateTime = LocalDateTime.of(localDate, localTime)
        zonedDateTime = ZonedDateTime.of(localDateTime, ZoneOffset.ofHours(-6))
    }

    void "test convert to long"() {
        expect:
        convert(zonedDateTime) == -914759696000L
    }

    void "test convert from long"() {
        given:
        ZonedDateTime converted = convert(-914759696000L)

        expect:
        converted == zonedDateTime.withNano(0).withZoneSameInstant(ZoneOffset.ofHours(-7)) || converted == zonedDateTime.withNano(0).withZoneSameInstant(ZoneOffset.ofHours(-8))
    }

    void "test that all ZonedDateTimeConverter/TemporalConverter trait methods are marked as Generated"() {
        expect: "all ZonedDateTimeConverter methods are marked as Generated on implementation class"
        ZonedDateTimeConverter.getMethods().each { Method traitMethod ->
            assert ZonedDateTimeConverterSpec.class.getMethod(traitMethod.name, traitMethod.parameterTypes).isAnnotationPresent(Generated)
        }

        and: "all TemporalConverter methods are marked as Generated on implementation class"
        TemporalConverter.getMethods().each { Method traitMethod ->
            assert ZonedDateTimeConverterSpec.class.getMethod(traitMethod.name, traitMethod.parameterTypes).isAnnotationPresent(Generated)
        }
    }
}