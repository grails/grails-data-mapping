package grails.gorm.tests

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 8/5/11
 * Time: 11:17 AM
 * To change this template use File | Settings | File Templates.
 */
class OneToOneSpec extends GormDatastoreSpec{

    def "Test persist and retrieve unidirectional many-to-one"() {
        given:"A domain model with a many-to-one"
            def person = new Person(firstName:"Fred", lastName: "Flintstone")
            def pet = new Pet(name:"Dino", owner:person)
            person.save()
            pet.save(flush:true)
            session.clear()

        when:"The association is queried"
            pet = Pet.findByName("Dino")

        then:"The domain model is valid"

            pet != null
            pet.name == "Dino"
            pet.owner != null
            pet.owner.firstName == "Fred"

    }
}
