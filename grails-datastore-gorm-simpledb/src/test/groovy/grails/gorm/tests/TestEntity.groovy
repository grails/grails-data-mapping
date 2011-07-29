package grails.gorm.tests

import grails.persistence.Entity

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
@Entity
class TestEntity implements Serializable {
    String id
    Long version

    String name
    Integer age = 30

    ChildEntity child

    public String toString() {
        return "TestEntity(AWS){" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", child=" + child +
                '}';
    }

    static constraints = {
        name blank: false
        child nullable: true
    }

    static mapping = {
        domain 'TestEntity'
    }
}
