package grails.gorm.time

import groovy.transform.CompileStatic
import groovy.transform.Generated

import java.time.LocalTime
import java.time.OffsetTime
import java.time.ZoneOffset

/**
 * A trait to convert a {@link java.time.OffsetTime} to and from a long
 *
 * @author James Kleeh
 */
@CompileStatic
trait OffsetTimeConverter implements TemporalConverter<OffsetTime> {

    @Override
    @Generated
    Long convert(OffsetTime value) {
        value.withOffsetSameInstant(ZoneOffset.UTC).toLocalTime().toNanoOfDay()
    }

    @Override
    @Generated
    OffsetTime convert(Long value) {
        OffsetTime.of(LocalTime.ofNanoOfDay(value), ZoneOffset.UTC).withOffsetSameInstant(systemOffset)
    }
}