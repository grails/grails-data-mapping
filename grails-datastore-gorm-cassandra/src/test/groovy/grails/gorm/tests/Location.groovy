package grails.gorm.tests



import grails.gorm.CassandraEntity

@CassandraEntity
class Location implements Serializable {
    UUID id
    Long version
    String name
    String code = "DEFAULT"

    def namedAndCode() {
        "$name - $code"
    }

    static mapping = {
        name index:true
        code index:true
    }
}
