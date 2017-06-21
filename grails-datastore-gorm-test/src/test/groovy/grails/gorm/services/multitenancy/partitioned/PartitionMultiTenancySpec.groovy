package grails.gorm.services.multitenancy.partitioned


import grails.gorm.MultiTenant
import grails.gorm.annotation.Entity
import grails.gorm.multitenancy.CurrentTenant
import grails.gorm.services.Service
import grails.gorm.transactions.ReadOnly
import grails.gorm.transactions.Transactional
import org.grails.datastore.mapping.config.Settings
import org.grails.datastore.mapping.multitenancy.MultiTenancySettings
import org.grails.datastore.mapping.multitenancy.exceptions.TenantNotFoundException
import org.grails.datastore.mapping.multitenancy.resolvers.SystemPropertyTenantResolver
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class PartitionMultiTenancySpec extends Specification {

    @Shared @AutoCleanup SimpleMapDatastore datastore = new SimpleMapDatastore(
            [(Settings.SETTING_MULTI_TENANCY_MODE): MultiTenancySettings.MultiTenancyMode.DISCRIMINATOR,
             (Settings.SETTING_MULTI_TENANT_RESOLVER): new SystemPropertyTenantResolver(),
             (Settings.SETTING_DB_CREATE): "create-drop"],
            getClass().getPackage()
    )
    @Shared IBookService bookDataService = datastore.getService(IBookService)

    void 'Test partitioned multi-tenancy with GORM services'() {
        when:"When there is no tenant"
        Book.count()

        then:"You still get an exception"
        thrown(TenantNotFoundException)

        when:"But look you can change tenant"
        System.setProperty(SystemPropertyTenantResolver.PROPERTY_NAME, "foo")

        BookService bookService = new BookService()

        then:
        bookService.countBooks() == 0
        bookDataService.countBooks()== 0

        when:"And the new @CurrentTenant transformation deals with the details for you!"
        bookService.saveBook("The Stand")
        bookService.saveBook("The Shining")

        then:
        bookService.countBooks() == 2
        bookDataService.countBooks()== 2
        bookService.findBooks("The Shining")[0].title == "The Shining"

        when:"Swapping to another schema and we get the right results!"
        System.setProperty(SystemPropertyTenantResolver.PROPERTY_NAME, "bar")

        then:
        bookService.countBooks() == 0
        bookDataService.countBooks()== 0
    }
}

@Entity
class Book implements MultiTenant<Book> {
    String tenantId
    String title
}

@CurrentTenant
@Transactional
class BookService {

    void saveBook(String title) {
        new Book(title:title).save()
    }

    @ReadOnly
    int countBooks() {
        Book.count()
    }

    @ReadOnly
    List<Book> findBooks(String title) {
        (List<Book>)Book.withCriteria {
            eq('title', title)
        }
    }

}

@CurrentTenant
@Service(Book)
interface IBookService {

    Book saveBook(String title)

    Integer countBooks()
}