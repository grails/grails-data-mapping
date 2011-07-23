package grails.gorm.tests

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
class ChildEntity {
    String id
    String name

    public String toString() {
        return "ChildEntity{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    static belongsTo = [TestEntity]

    static mapping = {
        domain 'ChildEntity'
    }
}
