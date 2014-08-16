package grails.gorm.tests

import grails.persistence.Entity

@groovy.transform.EqualsAndHashCode(includes=["lastName", "firstName"])
@Entity
class PersonLastNamePartitionKey  {      
    
    String lastName
    String firstName    
    Integer age = 0
    String location   

    static mapping = {
        id name:"lastName", primaryKey:[ordinal:0, type:"partitioned"], generator:"assigned", column: "surname"       
        firstName index:true, primaryKey:[ordinal:1, type: "clustered"]   
        age index:true, primaryKey:[ordinal:2, type: "clustered"], column: "person_age"
        location index:true  
    }

  
}