package grails.gorm.tests

import grails.persistence.Entity

@Entity
class SimpleWidgetDefaultOrderName implements Serializable {    
    String category
    String name
    
    static mapping = {
        id name:"category", primaryKey:[ordinal:0, type:"partitioned"], generator:"assigned"       
        name primaryKey:[ordinal:1, type: "clustered"]
        sort name:"desc"
    }
}