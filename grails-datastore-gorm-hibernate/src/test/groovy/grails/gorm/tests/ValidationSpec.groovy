package grails.gorm.tests

import grails.persistence.Entity
import org.springframework.transaction.support.TransactionSynchronizationManager
import spock.lang.Ignore
import spock.lang.Issue

/**
 * Tests validation semantics.
 */
class ValidationSpec extends GormDatastoreSpec {

    @Override
    List getDomainClasses() {
        return [ClassWithListArgBeforeValidate, ClassWithNoArgBeforeValidate,
                ClassWithOverloadedBeforeValidate, ValidatingParent, ValidatingChild]
    }

    void "Test validate() method"() {
        // test assumes name cannot be blank
        given:
            def t

        when:
            t = new TestEntity(name:"")
            boolean validationResult = t.validate()
            def errors = t.errors

        then:
            !validationResult
            t.hasErrors()
            errors != null
            errors.hasErrors()

        when:
            t.clearErrors()

        then:
            !t.hasErrors()
    }

    @Ignore// This test is not possible within the TCK because the data binding APIs are not present
    void 'Test that the binding rejected value is retained after validation'() {
        when:
            def t = new TestEntity()
            t.age = null
            t.properties = [age: 'bad value']

        then:
            t.errors.errorCount == 1

        when:
            def ageError = t.errors.getFieldError('age')

        then:
            'bad value' == ageError.rejectedValue

        when:
            t.validate()
            ageError = t.errors.getFieldError('age')

        then:
            'bad value' == ageError.rejectedValue
    }

    void "Test that validate is called on save()"() {

        given:
            def t

        when:
            t = new TestEntity(name:"")

        then:
            t.save() == null
            t.hasErrors() == true
            0 == TestEntity.count()

        when:
            t.clearErrors()
            t.name = "Bob"
            t.age = 45
            t.child = new ChildEntity(name:"Fred")
            t = t.save()

        then:
            t != null
            1 == TestEntity.count()
    }

    void "Test beforeValidate gets called on save()"() {
        given:
            def entityWithNoArgBeforeValidateMethod
            def entityWithListArgBeforeValidateMethod
            def entityWithOverloadedBeforeValidateMethod

        when:
            entityWithNoArgBeforeValidateMethod = new ClassWithNoArgBeforeValidate()
            entityWithListArgBeforeValidateMethod = new ClassWithListArgBeforeValidate()
            entityWithOverloadedBeforeValidateMethod = new ClassWithOverloadedBeforeValidate()
            entityWithNoArgBeforeValidateMethod.save()
            entityWithListArgBeforeValidateMethod.save()
            entityWithOverloadedBeforeValidateMethod.save()

        then:
            1 == entityWithNoArgBeforeValidateMethod.noArgCounter
            1 == entityWithListArgBeforeValidateMethod.listArgCounter
            1 == entityWithOverloadedBeforeValidateMethod.noArgCounter
            0 == entityWithOverloadedBeforeValidateMethod.listArgCounter
    }

    void "Test beforeValidate gets called on validate()"() {
        given:
            def entityWithNoArgBeforeValidateMethod
            def entityWithListArgBeforeValidateMethod
            def entityWithOverloadedBeforeValidateMethod

        when:
            entityWithNoArgBeforeValidateMethod = new ClassWithNoArgBeforeValidate()
            entityWithListArgBeforeValidateMethod = new ClassWithListArgBeforeValidate()
            entityWithOverloadedBeforeValidateMethod = new ClassWithOverloadedBeforeValidate()
            entityWithNoArgBeforeValidateMethod.validate()
            entityWithListArgBeforeValidateMethod.validate()
            entityWithOverloadedBeforeValidateMethod.validate()

        then:
            1 == entityWithNoArgBeforeValidateMethod.noArgCounter
            1 == entityWithListArgBeforeValidateMethod.listArgCounter
            1 == entityWithOverloadedBeforeValidateMethod.noArgCounter
            0 == entityWithOverloadedBeforeValidateMethod.listArgCounter
    }

    void "Test beforeValidate gets called on validate() and passing a list of field names to validate"() {
        given:
            def entityWithNoArgBeforeValidateMethod
            def entityWithListArgBeforeValidateMethod
            def entityWithOverloadedBeforeValidateMethod

        when:
            entityWithNoArgBeforeValidateMethod = new ClassWithNoArgBeforeValidate()
            entityWithListArgBeforeValidateMethod = new ClassWithListArgBeforeValidate()
            entityWithOverloadedBeforeValidateMethod = new ClassWithOverloadedBeforeValidate()
            entityWithNoArgBeforeValidateMethod.validate(['name'])
            entityWithListArgBeforeValidateMethod.validate(['name'])
            entityWithOverloadedBeforeValidateMethod.validate(['name'])

        then:
            1 == entityWithNoArgBeforeValidateMethod.noArgCounter
            1 == entityWithListArgBeforeValidateMethod.listArgCounter
            0 == entityWithOverloadedBeforeValidateMethod.noArgCounter
            1 == entityWithOverloadedBeforeValidateMethod.listArgCounter
            ['name'] == entityWithOverloadedBeforeValidateMethod.propertiesPassedToBeforeValidate
    }

    void "Test that validate works without a bound Session"() {

        given:
            def t

        when:
            session.disconnect()
            if (TransactionSynchronizationManager.hasResource(session.datastore)) {
                TransactionSynchronizationManager.unbindResource(session.datastore)
            }

            t = new TestEntity(name:"")

        then:
            !session.datastore.hasCurrentSession()
            t.save() == null
            t.hasErrors() == true
            1 == t.errors.allErrors.size()
            0 == TestEntity.count()

        when:
            t.clearErrors()
            t.name = "Bob"
            t.age = 45
            t.child = new ChildEntity(name:"Fred")
            t = t.save(flush: true)

        then:
            !session.datastore.hasCurrentSession()
            t != null
            1 == TestEntity.count()
    }

    @Issue('https://github.com/grails/grails-core/issues/2274')
    def "Test that beforeValidate fires for cascading child classes"() {
        when:"A save is called on a parent with a child entity"
        def p = new ValidatingParent()

        def child = new ValidatingChild()
        p.addToChildren(child)
        p.save flush:true

        then:"The beforeValidate() is called on the child"
        ValidatingParent.count() == 1
        ValidatingChild.count() == 1
        child.name == 'hard coded in beforeValidate'
    }
}

@Entity
class ValidatingParent {
    Long id
    Long version
    Set children
    static hasMany = [children:ValidatingChild]
}

@Entity
class ValidatingChild {
    Long id
    Long version
    ValidatingParent parent
    static belongsTo = [parent:ValidatingParent]

    String name

    def beforeValidate() {
        name = 'hard coded in beforeValidate'
    }
}
