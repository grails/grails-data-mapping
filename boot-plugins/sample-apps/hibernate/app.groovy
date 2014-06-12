@Grab("org.grails:gorm-hibernate4-spring-boot:1.0.0.RC2")
@Grab("com.h2database:h2:1.3.173")
import grails.persistence.*
import org.springframework.transaction.annotation.*


@RestController
class PersonController {
    @RequestMapping("/")
    List home() {
        def results = Person.list().collect { [firstName: it.firstName, lastName:it.lastName] }
        println "RESULTS = $results"
        return results
    }    

    @Transactional
    @RequestMapping("/create")
    Map createPerson() {
        def p = new Person(firstName:"Bart", lastName:"Simpson")
        p.save()
        [firstName: p.firstName, lastName:p.lastName]
    }

    @PostConstruct
    void init() {
        Person.withTransaction {
            new Person(firstName:"Homer", lastName:"Simpson").save()    
        }        
    }
}

@Entity
class Person {
    String firstName
    String lastName
}

