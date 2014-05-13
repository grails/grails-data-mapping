package grails.gorm.tests

import grails.gorm.CassandraEntity

@CassandraEntity
class Book implements Serializable {
    UUID id
    Long version
    String author
    String title
    Boolean published = false

    static mapping = {
        published index:true
        title index:true
        author index:true
    }
}