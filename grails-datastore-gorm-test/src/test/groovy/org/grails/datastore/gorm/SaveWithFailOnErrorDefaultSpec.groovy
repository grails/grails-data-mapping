package org.grails.datastore.gorm

import grails.gorm.tests.GormDatastoreSpec
import grails.validation.ValidationException
import org.codehaus.groovy.grails.commons.GrailsDomainConfigurationUtil
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.grails.datastore.mapping.annotation.Entity
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class SaveWithFailOnErrorDefaultSpec extends GormDatastoreSpec {
    @Override
    List getDomainClasses() {
        [TestProduct]
    }

    def setup() {
        def validator = [supports: {Class cls -> true},
                validate: {Object target, Errors errors ->
                    def constrainedProperties = GrailsDomainConfigurationUtil.evaluateConstraints(TestProduct)
                    for (ConstrainedProperty cp in constrainedProperties.values()) {
                        cp.validate(target, target[cp.propertyName], errors)
                    }
                }] as Validator

        final context = session.datastore.mappingContext
        final entity = context.getPersistentEntity(TestProduct.name)
        context.addEntityValidator(entity, validator)
    }

    void "test save with fail on error default"() {
        when: "A product is saved with fail on error default true"
            TestProduct.currentGormInstanceApi().failOnError = true
            def p = new TestProduct()
            p.save()

        then:"Validation exception thrown"
            thrown(ValidationException)

        when: "A product is saved with fail on error default false"
            TestProduct.currentGormInstanceApi().failOnError = false
            p = new TestProduct()
            def result = p.save()

        then:"The save returns false"
            !result
    }

    void "test override fail on error default"() {
        when: "A product is saved with fail on error override to false"
            TestProduct.currentGormInstanceApi().failOnError = true
            def p = new TestProduct()
            def result = p.save(failOnError: false, flush: true)

        then:"The save returns false"
            !result

        when: "A product is saved with fail on error override to true"
            TestProduct.currentGormInstanceApi().failOnError = false
            p = new TestProduct()
            p.save(failOnError: true)

        then:"Validation exception thrown"
            thrown(ValidationException)
    }
}

@Entity
class TestProduct {
    Long id
    String name

    static constraints = {
        //noinspection GroovyAssignabilityCheck
        name(nullable: false)
    }
}
