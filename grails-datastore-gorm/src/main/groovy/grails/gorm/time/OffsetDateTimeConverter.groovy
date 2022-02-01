package grails.gorm.time

import groovy.transform.CompileStatic
import groovy.transform.Generated

import java.time.Instant
import java.time.OffsetDateTime

/**
 * A trait to convert a {@link java.time.OffsetDateTime} to and from a long
 *
 * @author James Kleeh
 */
@CompileStatic
trait OffsetDateTimeConverter implements TemporalConverter<OffsetDateTime> {

    @Override
    @Generated
    Long convert(OffsetDateTime value) {
        value.toInstant().toEpochMilli()
    }

    @Override
    @Generated
    OffsetDateTime convert(Long value) {
        OffsetDateTime.ofInstant(Instant.ofEpochMilli(value), systemOffset)
    }

}
