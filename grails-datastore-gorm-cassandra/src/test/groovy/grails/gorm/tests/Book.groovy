package grails.gorm.tests

import grails.persistence.Entity

import org.springframework.data.cassandra.mapping.CassandraType

import com.datastax.driver.core.DataType

@Entity
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