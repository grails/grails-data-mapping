package grails.gorm.tests

import grails.persistence.Entity

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */

@Entity
class UniqueGroup implements Serializable {
    String id
    Long version

    String name
    static constraints = {
        name unique: true
    }

    public String toString() {
        return "UniqueGroup{" +
                "id='" + id + '\'' +
                ", version=" + version +
                ", name='" + name + '\'' +
                '}';
    }
}