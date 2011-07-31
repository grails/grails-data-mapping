package grails.gorm.tests

import org.grails.datastore.mapping.simpledb.model.types.SimpleDBTypeConverterRegistrar

import spock.lang.Ignore

/**
 * Tests back and forth conversion to String of numeric and other datatypes.
 *
 * @author Roman Stepanenko
 */
class StringConversionSpec extends GormDatastoreSpec {
    void "Test byte conversion"() {
        given:
            def fromString = SimpleDBTypeConverterRegistrar.STRING_TO_BYTE_CONVERTER
            def toString = SimpleDBTypeConverterRegistrar.BYTE_TO_STRING_CONVERTER

        expect:
            fromString.convert(toString.convert(0 as byte)) == 0 as byte
            fromString.convert(toString.convert(1 as byte)) == 1 as byte
            fromString.convert(toString.convert(-1 as byte)) == -1 as byte
            fromString.convert(toString.convert(10 as byte)) == 10 as byte
            fromString.convert(toString.convert(109 as byte)) == 109 as byte
            fromString.convert(toString.convert(-23 as byte)) == -23 as byte
            fromString.convert(toString.convert(-123 as byte)) == -123 as byte
            fromString.convert(toString.convert(Byte.MAX_VALUE as byte)) == Byte.MAX_VALUE as byte
            fromString.convert(toString.convert(Byte.MAX_VALUE-1 as byte)) == Byte.MAX_VALUE-1 as byte
            fromString.convert(toString.convert(Byte.MAX_VALUE-2 as byte)) == Byte.MAX_VALUE-2 as byte
            fromString.convert(toString.convert(Byte.MAX_VALUE-10 as byte)) == Byte.MAX_VALUE-10 as byte
            fromString.convert(toString.convert(Byte.MIN_VALUE as byte)) == Byte.MIN_VALUE as byte
            fromString.convert(toString.convert(Byte.MIN_VALUE+1 as byte)) == Byte.MIN_VALUE+1 as byte
            fromString.convert(toString.convert(Byte.MIN_VALUE+2 as byte)) == Byte.MIN_VALUE+2 as byte
            fromString.convert(toString.convert(Byte.MIN_VALUE+30 as byte)) == Byte.MIN_VALUE+30 as byte

            //test that for short numeric string conversion happens as is
            fromString.convert("1") == 1 as byte
            fromString.convert("-1") == -1 as byte
            fromString.convert("01") == 1 as byte
            fromString.convert("001") == 1 as byte
            fromString.convert("10") == 10 as byte
            fromString.convert("-10") == -10 as byte
            fromString.convert("010") == 10 as byte
            fromString.convert("102") == 102 as byte
            fromString.convert("-102") == -102 as byte
    }

    void "Test short conversion"() {
        given:
            def fromString = SimpleDBTypeConverterRegistrar.STRING_TO_SHORT_CONVERTER
            def toString = SimpleDBTypeConverterRegistrar.SHORT_TO_STRING_CONVERTER

        expect:
            fromString.convert(toString.convert(0 as short)) == 0 as short
            fromString.convert(toString.convert(-1 as short)) == -1 as short
            fromString.convert(toString.convert(1 as short)) == 1 as short
            fromString.convert(toString.convert(10 as short)) == 10 as short
            fromString.convert(toString.convert(109 as short)) == 109 as short
            fromString.convert(toString.convert(-23 as short)) == -23 as short
            fromString.convert(toString.convert(-123 as short)) == -123 as short
            fromString.convert(toString.convert(Short.MAX_VALUE as short)) == Short.MAX_VALUE as short
            fromString.convert(toString.convert(Short.MAX_VALUE-1 as short)) == Short.MAX_VALUE-1 as short
            fromString.convert(toString.convert(Short.MAX_VALUE-2 as short)) == Short.MAX_VALUE-2 as short
            fromString.convert(toString.convert(Short.MAX_VALUE-10 as short)) == Short.MAX_VALUE-10 as short
            fromString.convert(toString.convert(Short.MIN_VALUE as short)) == Short.MIN_VALUE as short
            fromString.convert(toString.convert(Short.MIN_VALUE+1 as short)) == Short.MIN_VALUE+1 as short
            fromString.convert(toString.convert(Short.MIN_VALUE+2 as short)) == Short.MIN_VALUE+2 as short
            fromString.convert(toString.convert(Short.MIN_VALUE+30 as short)) == Short.MIN_VALUE+30 as short


            //test that for short numeric string conversion happens as is
            fromString.convert("1") == 1 as short
            fromString.convert("-1") == -1 as short
            fromString.convert("01") == 1 as short
            fromString.convert("001") == 1 as short
            fromString.convert("10") == 10 as short
            fromString.convert("-10") == -10 as short
            fromString.convert("010") == 10 as short
            fromString.convert("102") == 102 as short
            fromString.convert("-102") == -102 as short
    }

    void "Test integer conversion"() {
        given:
            def fromString = SimpleDBTypeConverterRegistrar.STRING_TO_INTEGER_CONVERTER
            def toString = SimpleDBTypeConverterRegistrar.INTEGER_TO_STRING_CONVERTER

        expect:
            fromString.convert(toString.convert(0 as int)) == 0 as int
            fromString.convert(toString.convert(1 as int)) == 1 as int
            fromString.convert(toString.convert(-1 as int)) == -1 as int
            fromString.convert(toString.convert(10 as int)) == 10 as int
            fromString.convert(toString.convert(109 as int)) == 109 as int
            fromString.convert(toString.convert(-23 as int)) == -23 as int
            fromString.convert(toString.convert(-123 as int)) == -123 as int
            fromString.convert(toString.convert(Integer.MAX_VALUE as int)) == Integer.MAX_VALUE as int
            fromString.convert(toString.convert(Integer.MAX_VALUE-1 as int)) == Integer.MAX_VALUE-1 as int
            fromString.convert(toString.convert(Integer.MAX_VALUE-2 as int)) == Integer.MAX_VALUE-2 as int
            fromString.convert(toString.convert(Integer.MAX_VALUE-10 as int)) == Integer.MAX_VALUE-10 as int
            fromString.convert(toString.convert(Integer.MIN_VALUE as int)) == Integer.MIN_VALUE as int
            fromString.convert(toString.convert(Integer.MIN_VALUE+1 as int)) == Integer.MIN_VALUE+1 as int
            fromString.convert(toString.convert(Integer.MIN_VALUE+2 as int)) == Integer.MIN_VALUE+2 as int
            fromString.convert(toString.convert(Integer.MIN_VALUE+30 as int)) == Integer.MIN_VALUE+30 as int

            fromString.convert(toString.convert(Short.MAX_VALUE as short)) == Short.MAX_VALUE as short
            fromString.convert(toString.convert(Short.MAX_VALUE-1 as short)) == Short.MAX_VALUE-1 as short
            fromString.convert(toString.convert(Short.MAX_VALUE-2 as short)) == Short.MAX_VALUE-2 as short
            fromString.convert(toString.convert(Short.MAX_VALUE-10 as short)) == Short.MAX_VALUE-10 as short
            fromString.convert(toString.convert(Short.MIN_VALUE as short)) == Short.MIN_VALUE as short
            fromString.convert(toString.convert(Short.MIN_VALUE+1 as short)) == Short.MIN_VALUE+1 as short
            fromString.convert(toString.convert(Short.MIN_VALUE+2 as short)) == Short.MIN_VALUE+2 as short
            fromString.convert(toString.convert(Short.MIN_VALUE+30 as short)) == Short.MIN_VALUE+30 as short

            //test that for short numeric string conversion happens as is
            fromString.convert("1") == 1 as int
            fromString.convert("-1") == -1 as int
            fromString.convert("01") == 1 as int
            fromString.convert("001") == 1 as int
            fromString.convert("10") == 10 as int
            fromString.convert("-10") == -10 as int
            fromString.convert("010") == 10 as int
            fromString.convert("102") == 102 as int
            fromString.convert("-102") == -102 as int
    }

    void "Test long conversion"() {
        given:
            def fromString = SimpleDBTypeConverterRegistrar.STRING_TO_LONG_CONVERTER
            def toString = SimpleDBTypeConverterRegistrar.LONG_TO_STRING_CONVERTER

        expect:
            fromString.convert(toString.convert(0 as long)) == 0 as long
            fromString.convert(toString.convert(-1 as long)) == -1 as long
            fromString.convert(toString.convert(1 as long)) == 1 as long
            fromString.convert(toString.convert(10 as long)) == 10 as long
            fromString.convert(toString.convert(109 as long)) == 109 as long
            fromString.convert(toString.convert(-23 as long)) == -23 as long
            fromString.convert(toString.convert(-123 as long)) == -123 as long
            fromString.convert(toString.convert(Long.MAX_VALUE as long)) == Long.MAX_VALUE as long
            fromString.convert(toString.convert(Long.MAX_VALUE-1 as long)) == Long.MAX_VALUE-1 as long
            fromString.convert(toString.convert(Long.MAX_VALUE-2 as long)) == Long.MAX_VALUE-2 as long
            fromString.convert(toString.convert(Long.MAX_VALUE-10 as long)) == Long.MAX_VALUE-10 as long
            fromString.convert(toString.convert(Long.MIN_VALUE as long)) == Long.MIN_VALUE as long
            fromString.convert(toString.convert(Long.MIN_VALUE+1 as long)) == Long.MIN_VALUE+1 as long
            fromString.convert(toString.convert(Long.MIN_VALUE+2 as long)) == Long.MIN_VALUE+2 as long
            fromString.convert(toString.convert(Long.MIN_VALUE+30 as long)) == Long.MIN_VALUE+30 as long

            fromString.convert(toString.convert(Integer.MAX_VALUE as int)) == Integer.MAX_VALUE as int
            fromString.convert(toString.convert(Integer.MAX_VALUE-1 as int)) == Integer.MAX_VALUE-1 as int
            fromString.convert(toString.convert(Integer.MAX_VALUE-2 as int)) == Integer.MAX_VALUE-2 as int
            fromString.convert(toString.convert(Integer.MAX_VALUE-10 as int)) == Integer.MAX_VALUE-10 as int
            fromString.convert(toString.convert(Integer.MIN_VALUE as int)) == Integer.MIN_VALUE as int
            fromString.convert(toString.convert(Integer.MIN_VALUE+1 as int)) == Integer.MIN_VALUE+1 as int
            fromString.convert(toString.convert(Integer.MIN_VALUE+2 as int)) == Integer.MIN_VALUE+2 as int
            fromString.convert(toString.convert(Integer.MIN_VALUE+30 as int)) == Integer.MIN_VALUE+30 as int
        
            //test that for short numeric string conversion happens as is
            fromString.convert("1") == 1 as long
            fromString.convert("-1") == -1 as long
            fromString.convert("01") == 1 as long
            fromString.convert("001") == 1 as long
            fromString.convert("10") == 10 as long
            fromString.convert("-10") == -10 as long
            fromString.convert("010") == 10 as long
            fromString.convert("102") == 102 as long
            fromString.convert("-102") == -102 as long
    }
}