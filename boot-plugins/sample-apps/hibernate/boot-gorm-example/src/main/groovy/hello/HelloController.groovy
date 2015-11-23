package hello

import org.springframework.web.bind.annotation.*
import org.springframework.transaction.annotation.*

@RestController
class HelloController {

    @RequestMapping("/")
    @Transactional
    String home() {
        def p = Person.findByFirstName("Homer")
        if( !p ) {
            p = new Person(firstName:"Homer", lastName:"Simpson")
            p.save()                
        }
        return "Hello ${p.firstName}!"
    }
}

