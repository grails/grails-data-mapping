package grails.gorm.tests

import grails.persistence.Entity

/**
 * @author graemerocher
 */
@Entity
class TestEntity implements Serializable {
    UUID id
    Long version
    String name
    Integer age = 30

    ChildEntity child

    static mapping = {
        name index:true
        age index:true, nullable:true
        child index:true, nullable:true
    }

    static constraints = {
        name blank:false
        child nullable:true
    }
    
    static transients = ['child']
}
