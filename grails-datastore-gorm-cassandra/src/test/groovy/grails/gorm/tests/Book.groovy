package grails.gorm.tests

import grails.gorm.CassandraEntity

@CassandraEntity
class Book implements Serializable {    
    Long version
    String author
    String title
    Boolean published = false

    static mapping = {
        id name:'author', primaryKey:[ordinal:0, type:"partitioned"], generator:"assigned"   
        title index:true, primaryKey:[ordinal:1, type: "clustered"]
        published index:true               
    }
}