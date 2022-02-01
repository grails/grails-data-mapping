package grails.gorm.time

import groovy.transform.CompileStatic
import groovy.transform.Generated

import java.time.Instant

/**
 * A trait to convert a {@link java.time.Instant} to and from a long
 *
 * @author James Kleeh
 */
@CompileStatic
trait InstantConverter {

    @Generated
    Long convert(Instant value) {
        value.toEpochMilli()
    }

    @Generated
    Instant convert(Long value) {
        Instant.ofEpochMilli(value)
    }
}
