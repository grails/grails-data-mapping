package grails.gorm.tests

import grails.gorm.JpaEntity

@JpaEntity
class CommonTypes implements Serializable {
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
}
