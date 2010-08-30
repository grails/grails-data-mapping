package grails.gorm.tests

import org.springframework.validation.Errors

/**
 * Abstract base class for testing validation semantics
 */
class ValidationSpec extends GormDatastoreSpec{




  void "Test validate() method"() {
    // test assumes name cannot be blank
    given:
      def t

    when:
      t = new TestEntity(name:"")
      Errors errors = t.errors

    then:
      t.validate() == false
      t.hasErrors() == true
      errors != null
      errors.hasErrors() == true

    when:
      t.clearErrors()

    then:
      t.hasErrors() == false
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
}
