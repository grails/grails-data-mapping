package hello

import org.springframework.web.bind.annotation.*

@RestController
class HelloController {

    @RequestMapping("/")
    String home() {
        def p = Person.findByFirstName("Homer")
        if( !p ) {
            Person.withTransaction {
                p = new Person(firstName:"Homer", lastName:"Simpson")
                p.save()                
            }
        }
        return "Hello ${p.firstName}!"
    }
}

