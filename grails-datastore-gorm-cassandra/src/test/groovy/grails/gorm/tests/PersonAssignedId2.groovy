package grails.gorm.tests

import grails.gorm.CassandraEntity
import grails.persistence.Entity

@groovy.transform.EqualsAndHashCode(includes=["lastName", "firstName"])
@CassandraEntity
class PersonAssignedId2  {      
    
    String lastName
    String firstName    
    String location   

    static mapping = {
        id name:"lastName", primaryKey:[ordinal:0, type:"partitioned"], generator:"assigned"       
        firstName index:true, primaryKey:[ordinal:1, type: "clustered"]   
        location index:true     
    }

  
}