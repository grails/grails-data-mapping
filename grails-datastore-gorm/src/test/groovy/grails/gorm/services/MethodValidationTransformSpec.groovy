package grails.gorm.services

import grails.gorm.annotation.Entity
import grails.validation.ValidationException
import org.grails.datastore.gorm.validation.javax.services.ValidatedService
import org.springframework.transaction.PlatformTransactionManager
import spock.lang.Specification

import javax.validation.ConstraintViolationException
import javax.validation.ParameterNameProvider
import javax.validation.constraints.NotNull

/**
 * Created by graemerocher on 14/02/2017.
 */
class MethodValidationTransformSpec extends Specification {

    void "test simple validated property"() {
        when:"The service transform is applied to an interface it can't implement"
        Class service = new GroovyClassLoader().parseClass('''
import grails.gorm.services.*
import grails.gorm.annotation.Entity
import javax.validation.constraints.*

@Service(Foo)
interface MyService {

    @grails.gorm.transactions.NotTransactional
    Foo find(@NotNull String title) throws javax.validation.ConstraintViolationException
    
    @grails.gorm.transactions.NotTransactional
    Foo findAgain(@NotNull @org.hibernate.validator.constraints.NotBlank String title)
}
@Entity
class Foo {
    String title
}

''')

        then:
        service.isInterface()
        println service.classLoader.loadedClasses

        when:"the impl is obtained"
        Class impl = service.classLoader.loadClass("\$MyServiceImplementation")

        then:"The impl is valid"
        org.grails.datastore.mapping.services.Service.isAssignableFrom(impl)
        ValidatedService.isAssignableFrom(impl)

        when:"The parameter data is obtained"
        ParameterNameProvider parameterNameProvider = service.classLoader.loadClass("\$MyServiceImplementation\$ParameterNameProvider").newInstance()
        def instance = impl.newInstance()

        then:"It is correct"
        parameterNameProvider != null
        parameterNameProvider.getParameterNames(impl.getMethod("find", String)) == ["title"]
        instance.parameterNameProvider != null
        instance.parameterNameProvider.getParameterNames(impl.getMethod("find", String)) == ["title"]
        instance.validatorFactory != null


        when:
        instance.find(null)

        then:
        def e = thrown( ConstraintViolationException)
        e.constraintViolations.size() == 1
        e.constraintViolations.first().messageTemplate == '{javax.validation.constraints.NotNull.message}'
        e.constraintViolations.first().propertyPath.toString() == 'find.title'

        when:
        instance.findAgain("")

        then:
        def e2 = thrown( ValidationException )
        e2.message
        e2.errors.hasErrors()
        e2.errors.hasFieldErrors('title')
        e2.errors.getFieldValue('title') == ""
    }
}

//@Service(Foo)
//interface MyService {
//
//    Foo find(@NotNull String title)
//}
//@Entity
//class Foo {
//    String title
//}
