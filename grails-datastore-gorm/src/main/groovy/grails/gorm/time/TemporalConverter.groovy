package grails.gorm.time

import groovy.transform.Generated

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

trait TemporalConverter<T> {

    abstract Long convert(T value)

    abstract T convert(Long value)

    @Generated
    ZoneOffset getSystemOffset() {
        Instant instant = Instant.now()
        ZoneId systemZone = ZoneId.systemDefault()
        systemZone.rules.getOffset(instant)
    }
}
