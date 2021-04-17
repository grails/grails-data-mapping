package grails.gorm.time

import groovy.transform.CompileStatic
import groovy.transform.Generated

import java.time.LocalTime

/**
 * A trait to convert a {@link LocalTime} to and from a long
 *
 * @author James Kleeh
 */
@CompileStatic
trait LocalTimeConverter implements TemporalConverter<LocalTime> {

    @Override
    @Generated
    Long convert(LocalTime value) {
        value.toNanoOfDay()
    }

    @Override
    @Generated
    LocalTime convert(Long value) {
        LocalTime.ofNanoOfDay(value)
    }
}