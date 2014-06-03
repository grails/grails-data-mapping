package grails.gorm.tests;

import grails.gorm.CassandraEntity

@CassandraEntity
class CommonTypes implements Serializable {
    
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
    byte[] ba
}