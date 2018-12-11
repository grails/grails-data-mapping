package grails.gorm.validation

import org.grails.datastore.gorm.validation.constraints.registry.DefaultValidatorRegistry
import org.grails.datastore.mapping.core.connections.ConnectionSourceSettings
import org.grails.datastore.mapping.keyvalue.mapping.config.GormKeyValueMappingFactory
import org.grails.datastore.mapping.keyvalue.mapping.config.KeyValueMappingContext
import org.grails.datastore.mapping.model.MappingContext
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.config.GormMappingConfigurationStrategy
import org.grails.datastore.mapping.validation.ValidationErrors
import org.grails.datastore.mapping.validation.ValidatorRegistry
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import spock.lang.Issue
import spock.lang.Shared
import spock.lang.Specification

import javax.persistence.Entity
import javax.persistence.Transient

class PersistentEntityValidatorSpec extends Specification {
    @Shared Validator authorValidator

    void setupSpec() {
        MappingContext mappingContext = new KeyValueMappingContext("test")
        mappingContext.mappingFactory = new GormKeyValueMappingFactory("test")
        mappingContext.syntaxStrategy = new GormMappingConfigurationStrategy(mappingContext.mappingFactory)

        PersistentEntity authorEntity = mappingContext.addPersistentEntity(Author)
        mappingContext.addPersistentEntity(Book)
        mappingContext.addPersistentEntity(Publisher)

        ValidatorRegistry registry = new DefaultValidatorRegistry(mappingContext, new ConnectionSourceSettings())
        authorValidator = registry.getValidator(authorEntity)
    }

    // beforeValidate on the initial save is part of the GormValidationApi doValidate() call
    @Issue('https://github.com/grails/grails-data-mapping/issues/1102')
    def "validation of root object does NOT trigger beforeValidate here"() {
        Author author = new Author()
        Errors errors = new ValidationErrors(author)

        when:
        authorValidator.validate(author, errors)

        then:
        errors.hasErrors()
        errors.getFieldErrors('name')
    }

    @Issue('https://github.com/grails/grails-data-mapping/issues/1102')
    def "cascading validation triggers beforeValidate callback on to-many association"() {
        Author author = new Author(name: 'Author', books: [new Book()])
        Errors errors = new ValidationErrors(author)

        when:
        authorValidator.validate(author, errors)

        then:
        !errors.hasErrors()
        author.books.first()
        author.books.first().name == "name"
    }

    @Issue('https://github.com/grails/grails-data-mapping/issues/1102')
    def "cascading validation triggers beforeValidate callback on to-one association"() {
        Author author = new Author(name: 'Author', publisher: new Publisher())
        Errors errors = new ValidationErrors(author)

        when:
        authorValidator.validate(author, errors)

        then:
        !errors.hasErrors()
        author.publisher.name == "name"
    }

    @Issue('https://github.com/grails/grails-data-mapping/issues/1106')
    def "validation does not cascade when cascadeValidate is false and not owning side"() {
        Author author = new Author(name: 'Author', anotherPublisher: new Publisher(), publisher: new Publisher())
        Errors errors = new ValidationErrors(author)

        when:
        authorValidator.validate(author, errors)

        then: "validate was not called for this association"
        !errors.hasErrors()
        !author.anotherPublisher.name

        and: "validate was called for this one since the default for cascadeValidate is true"
        author.publisher.name == "name"
    }

    @Issue('https://github.com/grails/grails-data-mapping/issues/1106')
    def "validation cascades regardless of cascadeValidate when owning side"() {
        Author author = new Author(name: 'Author', books: [new Book()])
        Errors errors = new ValidationErrors(author)

        when:
        authorValidator.validate(author, errors)

        then: "validate is called for books"
        !errors.hasErrors()
        author.books.first()
        author.books.first().name == "name"
    }

    @Issue('https://github.com/grails/grails-data-mapping/issues/1106')
    def "validation does not cascade if not owner and cascade options are not PERSIST or MERGE"() {
        Author author = new Author(name: 'Author', nonCascadePublisher: new Publisher())
        Errors errors = new ValidationErrors(author)

        when:
        authorValidator.validate(author, errors)

        then: "validate is not called for publisher"
        !errors.hasErrors()
        !author.nonCascadePublisher.name
    }
}

@Entity
class Author {
    String name

    Publisher publisher
    Publisher anotherPublisher
    Publisher nonCascadePublisher

    Set<Book> books

    static hasMany = [books: Book]

    static constraints = {
        publisher(nullable: true)
        anotherPublisher(nullable: true)
        nonCascadePublisher(nullable: true)
    }

    static mapping = {
        books(cascadeValidate: false) // This will be ignored since Author.isOwningSide of books
        anotherPublisher(cascadeValidate: false)
        nonCascadePublisher(cascade: 'none') // This will also prevent cascading validation
    }
}

@Entity
class Book {
    String name
    static belongsTo = [author: Author]

    def beforeValidate() {
        name = "name"
    }
}

@Entity
class Publisher {
    String name


    def beforeValidate() {
        name = "name"
    }

    // some of the persistent entity validator logic relies on errors being present
    @Transient
    Errors errors
    Errors getErrors() {
        if(errors == null) {
            errors = new ValidationErrors(this)
        }
        errors
    }
}
