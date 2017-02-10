package grails.gorm.services

import grails.gorm.annotation.Entity
import grails.gorm.validation.PersistentEntityValidator
import grails.validation.ValidationException
import org.grails.datastore.gorm.validation.constraints.eval.DefaultConstraintEvaluator
import org.grails.datastore.gorm.validation.constraints.registry.DefaultConstraintRegistry
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import org.springframework.context.support.StaticMessageSource
import spock.lang.AutoCleanup
import spock.lang.Specification

/**
 * Created by graemerocher on 06/02/2017.
 */
class ServiceImplSpec extends Specification {

    @AutoCleanup SimpleMapDatastore datastore = new SimpleMapDatastore(
        Product
    )

    void "test list products"() {
        given:
        Product p1 = new Product(name: "Apple", type:"Fruit").save(flush:true)
        Product p2 = new Product(name: "Orange", type:"Fruit").save(flush:true)
        ProductService productService = datastore.getService(ProductService)

        expect:
        productService.listWithArgs(max:1).size() == 1
        productService.listProducts().size() == 2
        productService.listMoreProducts().length == 2
        productService.findEvenMoreProducts().iterator().hasNext()
        productService.findByName("Apple").iterator().hasNext()
        productService.findProducts("Apple", "Fruit").iterator().hasNext()
        !productService.findProducts("Apple", "Devices").iterator().hasNext()
        !productService.findByName("Banana").iterator().hasNext()
        productService.findProducts("Apple").iterator().hasNext()
        !productService.findProducts("Banana").iterator().hasNext()
        productService.getByName("Apple") != null
        productService.getByName("Apple").name == "Apple"
        productService.getByName("Banana") == null
        p1.name == productService.get(p1.id)?.name
        productService.get(100) == null
        productService.find("Apple", "Fruit") != null
        productService.find("Orange", "Fruit").name == "Orange"
        productService.find("Apple", "Fruit", [max:2]) != null
        productService.find("Apple", "Device") == null
    }

    void "test delete by id implementation"() {
        given:
        Product p1 = new Product(name: "Apple", type:"Fruit").save(flush:true)
        Product p2 = new Product(name: "Orange", type:"Fruit").save(flush:true)
        ProductService productService = datastore.getService(ProductService)

        when:
        Product found = productService.get(p1.id)

        then:
        found != null

        when:
        Product deleted = productService.deleteProduct(found.id)

        then:
        deleted != null
        productService.get(found.id) == null

    }

    void "test delete by parameter query implementation"() {
        given:
        Product p1 = new Product(name: "Apple", type:"Fruit").save(flush:true)
        Product p2 = new Product(name: "Orange", type:"Fruit").save(flush:true)
        ProductService productService = datastore.getService(ProductService)

        when:
        Product found = productService.get(p1.id)

        then:
        found != null

        when:
        Product deleted = productService.delete("Apple")

        then:
        deleted != null
        productService.getByName(deleted.name) == null

    }

    void "test delete all implementation"() {
        given:
        Product p1 = new Product(name: "Apple", type:"Fruit").save(flush:true)
        Product p2 = new Product(name: "Orange", type:"Fruit").save(flush:true)
        ProductService productService = datastore.getService(ProductService)

        when:
        Product found = productService.get(p1.id)

        then:
        found != null

        when:
        Number deleted = productService.deleteProducts("Apple")

        then:
        deleted == 1
        productService.get(p1.id) == null

    }

    void "test delete with void return type"() {
        given:
        Product p1 = new Product(name: "Apple", type:"Fruit").save(flush:true)
        Product p2 = new Product(name: "Orange", type:"Fruit").save(flush:true)
        ProductService productService = datastore.getService(ProductService)

        when:
        Product found = productService.get(p1.id)

        then:
        found != null

        when:
        Number deleted = productService.remove(p1.id)

        then:
        deleted == 1
        productService.get(p1.id) == null
    }

    void "test save entity"() {
        given:
        ProductService productService = datastore.getService(ProductService)

        when:
        productService.saveProduct("Pineapple", "Fruit")

        then:
        productService.find("Pineapple", "Fruit") != null
    }

    void "test save invalid entity"() {
        given:
        def mappingContext = datastore.mappingContext
        def entity = mappingContext.getPersistentEntity(Product.name)
        def messageSource = new StaticMessageSource()
        def evaluator = new DefaultConstraintEvaluator(new DefaultConstraintRegistry(messageSource), mappingContext, Collections.emptyMap())
        mappingContext.addEntityValidator(
                entity,
            new PersistentEntityValidator(entity, messageSource, evaluator)
        )
        ProductService productService = datastore.getService(ProductService)

        when:
        productService.saveProduct("", "Fruit")

        then:
        thrown(ValidationException)
    }

    void "test abstract class service impl"() {
        given:
        AnotherProductService productService = (AnotherProductService)datastore.getService(AnotherProductInterface)

        when:
        Product p = productService.saveProduct("Apple", "Fruit")

        then:
        p.id != null
        productService.get(p.id) != null

        when:
        productService.delete(p.id)

        then:
        productService.get(p.id) == null
        productService.getByName("blah").name == "BLAH"

    }

    void "test update one method"() {
        given:
        ProductService productService = datastore.getService(ProductService)

        when:
        productService.saveProduct("Tomato", "Vegetable")

        then:
        productService.find("Tomato", "Vegetable") != null

        when:
        Product product = productService.find("Tomato", "Vegetable")
        productService.updateProduct(product.id, "Fruit")

        then:
        productService.find("Tomato", "Vegetable") == null
        productService.find("Tomato", "Fruit") != null

    }

    void 'test property projection'() {
        given:
        ProductService productService = datastore.getService(ProductService)

        when:
        Product p = productService.saveProduct("Tomato", "Vegetable")

        then:
        productService.findProductType(p.id) == "Vegetable"
    }

    void 'test property projection return all types'() {
        given:
        ProductService productService = datastore.getService(ProductService)

        when:
        productService.saveProduct("Carrot", "Vegetable")
        productService.saveProduct("Pumpkin", "Vegetable")
        productService.saveProduct("Tomato", "Fruit")

        then:
        productService.listProductName("Vegetable").size() == 2
    }

    void "test @where annotation"() {
        given:
        ProductService productService = datastore.getService(ProductService)

        when:
        productService.saveProduct("Carrot", "Vegetable")
        productService.saveProduct("Pumpkin", "Vegetable")
        productService.saveProduct("Tomato", "Fruit")

        Product p = productService.searchByType("Veg%")

        then:
        p != null
        p.name == 'Carrot'
        productService.searchByType("Stuf%") == null

    }

    void "test @query annotation"() {
        given:
        ProductService productService = datastore.getService(ProductService)

        when:
        productService.searchWithQuery("Veg%")

        then:
        def e = thrown(UnsupportedOperationException)
        e.message == "String-based queries like [find] are currently not supported in this implementation of GORM. Use criteria instead."

        when:
        productService.searchAllWithQuery("Veg%")

        then:
        e = thrown(UnsupportedOperationException)
        e.message == "String-based queries like [findAll] are currently not supported in this implementation of GORM. Use criteria instead."

    }
}

@Entity
class Product {
    String name
    String type

    static constraints = {
        name blank:false
    }
}

interface AnotherProductInterface {
    Product saveProduct(String name, String type)

    Number delete(Serializable id)
}

@Service(Product)
abstract class AnotherProductService implements AnotherProductInterface{

    abstract Product get(Serializable id)

    Product getByName(String name) {
        return new Product(name:name.toUpperCase())
    }
}

@Service(Product)
interface ProductService {

    @Query("from ${Product p} where $p.name like $pattern")
    Product searchWithQuery(String pattern)

    @Query("from ${Product p} where $p.name like $pattern")
    List<Product> searchAllWithQuery(String pattern)

    @Where({ type ==~ pattern })
    Product searchByType(String pattern)

    List<String> listProductName(String type)

    String findProductType(Serializable id)

    Product updateProduct(Serializable id, String type)

    Product saveProduct(String name, String type)

    Number deleteProducts(String name)

    Product delete(String name)

    Number remove(Serializable id)

    Product deleteProduct(Serializable id)

    Product get(Serializable id)

    Product getByName(String name)

    Product find(String name, String type)

    Product find(String name, String type, Map args)

    List<Product> findProducts(String name)

    List<Product> findProducts(String name, String type)

    List<Product> listWithArgs(Map args)

    List<Product> listProducts()

    Product[] listMoreProducts()

    Iterable<Product> findEvenMoreProducts()

    Iterable<Product> findByName(String n)
}
