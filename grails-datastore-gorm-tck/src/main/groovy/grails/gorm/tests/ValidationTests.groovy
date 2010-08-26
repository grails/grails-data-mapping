package grails.gorm.tests

import org.junit.Test
import org.springframework.validation.Errors

/**
 * Abstract base class for testing validation semantics
 */
abstract class ValidationTests extends AbstractGormTests{



  @Test
  void testValidateMethod() {
    // test assumes name cannot be blank
    def t = new TestEntity(name:"")

    assert !t.validate()
    assert t.hasErrors()
    Errors errors = t.errors

    assert errors
    assert errors.hasErrors()

    t.clearErrors()

    assert !t.hasErrors()
  }

  @Test
  void testValidateOnSave() {

    def t = new TestEntity(name:"")

    assert !t.save()
    assert t.hasErrors()

    assert 0 == TestEntity.count()

    t.clearErrors()

    t.name = "Bob"
    t.age = 45
    t.child = new ChildEntity(name:"Fred")

    t = t.save()
    assert t

    assert 1 == TestEntity.count()
    
  }
}
