package grails.gorm.tests

/**
 * @author graemerocher
 */
@grails.persistence.Entity
class TestEntity implements Serializable {
    Long id
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
}
