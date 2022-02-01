package grails.gorm.time

import groovy.transform.CompileStatic
import groovy.transform.Generated

import java.time.Period

/**
 * A trait to convert a {@link java.time.Period} to and from a String
 *
 * @author James Kleeh
 */
@CompileStatic
trait PeriodConverter {

    @Generated
    String convert(Period value) {
        value.toString()
    }

    @Generated
    Period convert(String value) {
        Period.parse(value)
    }
}
