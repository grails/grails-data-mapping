package grails.gorm.tests

import grails.persistence.Entity

@Entity
class CommonTypes implements Serializable {
    UUID id
    Long version
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
    UUID uuid
    UUID timeuuid
    String text
    String ascii
    String varchar
    TestEnum testEnum
    boolean transientBoolean
    String transientString
    transient long tran
    def service
        
    static mapping = {
        timeuuid type:"timeuuid"
        ascii type:'ascii'
        varchar type:'varchar'               
    }
    
    static transients = [
        'transientBoolean',
        'transientString'
    ]
}