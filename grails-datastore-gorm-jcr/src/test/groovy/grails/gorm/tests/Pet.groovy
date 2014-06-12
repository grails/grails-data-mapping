import grails.gorm.tests.Person
import grails.gorm.tests.PetType

@grails.persistence.Entity
class Pet implements Serializable {
    String id
    String name
    Date birthDate = new Date()
    PetType type
    Person owner
}
