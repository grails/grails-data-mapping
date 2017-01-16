package grails.gorm.annotation.multitenancy

import grails.gorm.multitenancy.TenantService
import org.grails.datastore.mapping.core.Datastore
import spock.lang.Specification

/**
 * Created by graemerocher on 16/01/2017.
 */
class CurrentTenantTransformSpec extends Specification {

    void "test @CurrentTenant transforms a service and makes a method that is wrapped in current tenant handling"() {
        given:"A service with @CurrentTenant applied as the class level"
        def bookService = new GroovyShell().evaluate('''
import grails.gorm.multitenancy.CurrentTenant
class BookService {
    @CurrentTenant
    List listBooks() {
        return ["The Stand"]
    }
}
new BookService()

''')
        when:"the list books method is invoked"
        def result = bookService.listBooks()

        then:"An exception was thrown because GORM is not setup"
        thrown(IllegalStateException)

    }
}
