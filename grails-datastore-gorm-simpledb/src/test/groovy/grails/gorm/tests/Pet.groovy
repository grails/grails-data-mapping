package grails.gorm.tests

import grails.persistence.Entity

/**
 * Test entity for testing AWS SimpleDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */

@Entity
class Pet implements Serializable {
    String id
    Long version

    String name
    Date birthDate = new Date()
    PetType type = new PetType(name: "Unknown")
    Person owner
    Integer age

    public String toString() {
        return "Pet{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", birthDate=" + birthDate +
                ", type=" + type +
                ", owner=" + owner +
                ", age=" + age +
                '}';
    }


    static constraints = {
        owner nullable:true
        age nullable: true
    }
}