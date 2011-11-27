package grails.gorm.tests

import grails.persistence.Entity

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */

@Entity
class GroupWithin implements Serializable {
    String id
    Long version

    String name
    String org
    static constraints = {
        name unique:"org"
//        org index:true
    }

    public String toString() {
        return "GroupWithin{" +
                "id='" + id + '\'' +
                ", version=" + version +
                ", name='" + name + '\'' +
                ", org='" + org + '\'' +
                '}';
    }
}