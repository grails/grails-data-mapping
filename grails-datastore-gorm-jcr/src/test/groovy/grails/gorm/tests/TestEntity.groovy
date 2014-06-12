package grails.gorm.tests

/**
 * Override from GORM TCK to test String based Id
 *
 * @author Erawat Chamanont
 * @since 1.0
 */
@grails.persistence.Entity
class TestEntity implements Serializable {
    String id //for supporting String datatype such as JCR
    String name
    Integer age

    ChildEntity child

    static mapping = {
        name index:true
        age index:true
        child index:true
    }
}
