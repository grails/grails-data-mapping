package grails.gorm.tests

import grails.persistence.Entity

/**
 * Test entity for testing AWS DynamoDB.
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

    String toString() {
        "TestEntity(AWS){id='$id', name='$name', age=$age, child=$child}"
    }

    static constraints = {
//        name blank: false
//        we change the constraint because in original ValidationSpec we are testing if we can actually save with empty string but dynamo does not allow empty strings
        name nullable: false

        child nullable: true
    }

    static mapping = {
        table 'TestEntity'
        child nullable:true
    }
}
