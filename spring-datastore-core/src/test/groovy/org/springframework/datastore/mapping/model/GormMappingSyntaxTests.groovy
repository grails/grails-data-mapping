package org.springframework.datastore.mapping.model

import org.junit.Test
import org.springframework.datastore.mapping.model.config.GormMappingConfigurationStrategy
import javax.persistence.Entity
import org.springframework.datastore.mapping.model.types.OneToOne
import org.springframework.datastore.mapping.model.types.ManyToOne
import org.springframework.datastore.mapping.model.types.Association
import org.springframework.datastore.mapping.model.types.OneToMany

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class GormMappingSyntaxTests {

  @Test
  void testIsEntity() {

    def strategy = new GormMappingConfigurationStrategy(new TestMappedPropertyFactory())

    assert strategy.isPersistentEntity(TestEntity)
    assert strategy.isPersistentEntity(JavaEntity)
    assert !strategy.isPersistentEntity(GormMappingSyntaxTests)
  }

  @Test
  void testGetIdentity() {
    def context = new TestMappingContext()
    context.addPersistentEntity(TestEntity)
    def strategy = context.mappingSyntaxStrategy
    def id = strategy.getIdentity(TestEntity, context)

    assert id != null

    assert id.type == Long
    assert id.name == 'id'
  }

  @Test
  void testGetSimplePersistentProperties() {
    def context = new TestMappingContext()
    context.addPersistentEntity(TestEntity)
    def strategy = context.mappingSyntaxStrategy
    def props = strategy.getPersistentProperties(TestEntity,context)
    assert props.size() == 3

  }

  @Test
  void testUnidirectionalOneToOneAssociation() {
    def context = new TestMappingContext()
    context.addPersistentEntity(TestEntity)

    assert 2 == context.persistentEntities.size()

    def testEntity = context.getPersistentEntity(TestEntity.name)

    def association = testEntity.getPropertyByName("second")

    assert association != null

    assert (association instanceof OneToOne)

    OneToOne toOne = association
    assert toOne.foreignKeyInChild
    assert toOne.associatedEntity != null

    assert toOne.associatedEntity == context.getPersistentEntity(SecondEntity.name)
    assert toOne.referencedPropertyName == null
    assert toOne.bidirectional == false
    assert toOne.owningSide == true

  }

  @Test
  void testUnidirectionalOneToMany() {
    def context = new TestMappingContext()
    context.addPersistentEntity(Publisher)

    assert 3 == context.persistentEntities.size()

    def publisher = context.getPersistentEntity(Publisher.name)

    assert publisher != null

    Association oneToMany = publisher.getPropertyByName("authors")
    assert oneToMany != null
    assert !oneToMany.bidirectional
    assert !oneToMany.owningSide
    assert (oneToMany instanceof OneToMany)
  }

  @Test
  void testManyToOneAssociation() {
    def context = new TestMappingContext()
    context.addPersistentEntity(Book)

    assert 2 == context.persistentEntities.size()


    def book = context.getPersistentEntity(Book.name)

    assert book != null
    Association authorAssociation = book.getPropertyByName("author")

    assert authorAssociation != null
    assert (authorAssociation instanceof ManyToOne)
    assert authorAssociation.bidirectional
    assert !authorAssociation.owningSide

    Association inverse = authorAssociation.inverseSide
    assert inverse != null

    assert "books" == inverse.name
    assert Author == inverse.owner.javaClass
    assert inverse.inverseSide != null
    assert inverse.bidirectional
    assert (inverse instanceof OneToMany)
    assert inverse.owningSide
  }


  @Entity
  class JavaEntity {

  }
}
@grails.persistence.Entity
class Book {
    String title
    static belongsTo = [author:Author]  
}
@grails.persistence.Entity
class Author {
    String name
    static hasMany = [books:Book]
}
@grails.persistence.Entity
class Publisher {
  static hasMany = [authors:Author]
}
@grails.persistence.Entity
class TestEntity {

  String name
  String bar

  static hasOne = [second:SecondEntity]
  static transients = ['bar']
}
@grails.persistence.Entity
class SecondEntity {
  String name
  String bar

  static transients = ['bar']
}

