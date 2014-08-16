package grails.gorm.tests

import grails.persistence.Entity

@Entity
class Artist {
    
    String firstLetter
    String artist
    int age
    
    static mapping = {
        id name:'firstLetter', primaryKey:[ordinal:0, type:"partitioned"], generator:"assigned"
        artist primaryKey:[ordinal:1, type: "clustered"]
    }
}
