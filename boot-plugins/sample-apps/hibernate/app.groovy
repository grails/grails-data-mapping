@GrabResolver(name='grailsSnapshots', root='http://repo.grails.org/grails/libs-snapshots-local')
@Grab("org.grails:gorm-hibernate4-spring-boot:1.0.0.BUILD-SNAPSHOT")
@Grab("com.h2database:h2:1.3.173")
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
        p.save()
        [firstName: p.firstName, lastName:p.lastName]
    }
}

@Entity
class Person {
    String firstName
    String lastName
}

