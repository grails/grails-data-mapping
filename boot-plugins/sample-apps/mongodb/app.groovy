@Grab("org.grails:gorm-mongodb-spring-boot:1.0.0.RC1")
import grails.persistence.*
import org.springframework.transaction.annotation.*


@RestController
class PersonController {
    @RequestMapping("/")
    List<Person> home() {
        Person.list().collect { [firstName: it.firstName, lastName:it.lastName] }
    }    

    @Transactional
    @RequestMapping("/create")
    Map createPerson() {
        def p = new Person(firstName:"Bart", lastName:"Simpson")
        p.save(flush:true)
        [firstName: p.firstName, lastName:p.lastName]
    }
}

@Entity
class Person {
    String firstName
    String lastName
}

