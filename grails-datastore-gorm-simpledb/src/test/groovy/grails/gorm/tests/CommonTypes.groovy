package grails.gorm.tests

import grails.persistence.Entity

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
@Entity
class CommonTypes implements Serializable {
    String id
    Long version

    String name

    Long l
    Byte b
    Short s
    Boolean bool
    Integer i
    URL url
    Date date
    Calendar c
    BigDecimal bd
    BigInteger bi
    Double d
    Float f
    TimeZone tz
    Locale loc
    Currency cur

    String toString() {
        "CommonTypes{id='$id', version=$version, name='$name', l=$l, b=$b, s=$s, bool=$bool, i=$i, url=$url, date=$date, c=$c, bd=$bd, bi=$bi, d=$d, f=$f, tz=$tz, loc=$loc, cur=$cur}"
    }
}