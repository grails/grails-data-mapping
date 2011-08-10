package org.grails.datastore.mapping.model

import static org.junit.Assert.*

import javax.persistence.Entity

import org.junit.Test
import org.grails.datastore.mapping.model.config.GormMappingConfigurationStrategy
import org.grails.datastore.mapping.model.types.Association
import org.grails.datastore.mapping.model.types.ManyToOne
import org.grails.datastore.mapping.model.types.OneToMany
import org.grails.datastore.mapping.model.types.OneToOne

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

    @Test
    void testIndexedProperty() {
        def context = new TestMappingContext()
        context.addPersistentEntity(EntityWithIndexedProperty)

        assertEquals 1, context.persistentEntities.size()

        assertNotNull context.getPersistentEntity(EntityWithIndexedProperty.name)

        assertEquals 3, context.mappingSyntaxStrategy.getPersistentProperties(EntityWithIndexedProperty, context).size()
    }

    @Entity
    class JavaEntity {}
}

@grails.persistence.Entity
class Book {
    Long id
    String title
    Author author
    static belongsTo = [author:Author]
}

@grails.persistence.Entity
class Author {
    Long id
    String name
    Set books
    static hasMany = [books:Book]
}

@grails.persistence.Entity
class Publisher {
    Long id
    Set authors
    static hasMany = [authors:Author]
}

@grails.persistence.Entity
class TestEntity {
    Long id
    Long version
    String name
    String bar

    SecondEntity second
    static hasOne = [second:SecondEntity]
    static transients = ['bar']
}

@grails.persistence.Entity
class SecondEntity {
    Long id
    String name
    String bar

    static transients = ['bar']
}

@grails.persistence.Entity
class EntityWithIndexedProperty {
    Long id
    Long version
    String name
    String bar

    String getSectionContent(int section) {}
    void setSectionContent(int section, String content) {}
}
