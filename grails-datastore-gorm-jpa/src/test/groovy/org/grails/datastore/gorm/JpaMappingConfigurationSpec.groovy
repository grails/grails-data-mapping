package org.grails.datastore.gorm

import grails.gorm.JpaEntity

import javax.persistence.OneToMany
import javax.persistence.OneToOne

import org.grails.datastore.mapping.jpa.config.JpaMappingConfigurationStrategy
import org.grails.datastore.mapping.jpa.config.JpaMappingContext
import org.grails.datastore.mapping.jpa.config.JpaMappingFactory

import spock.lang.Specification

class JpaMappingConfigurationSpec extends Specification {

    void "Test that a JPA entity is detected as such"() {

        when:
            def configStrategy = new JpaMappingConfigurationStrategy()

        then:
            configStrategy.isPersistentEntity(JpaDomain.class) == true
            configStrategy.isPersistentEntity(JpaMappingConfigurationSpec) == false
    }

    void "Test persistent properties are valid"() {
        when:
            def configStrategy = new JpaMappingConfigurationStrategy(new JpaMappingFactory())
            final context = new JpaMappingContext()
            context.addPersistentEntity(JpaDomain)
            def properties = configStrategy.getPersistentProperties(JpaDomain, context).sort { it.name }

        then:
            properties.size() == 3

            properties[0] instanceof org.grails.datastore.mapping.model.types.OneToMany
            properties[0].name == "many"

            properties[1].name == "name"
            properties[2].name == "other"
            properties[2] instanceof org.grails.datastore.mapping.model.types.OneToOne
    }
}

@JpaEntity
class JpaDomain {
    Long id

    String name

    @OneToOne
    JpaOther other

    @OneToMany
    Set<JpaOther> many
}

@JpaEntity
class JpaOther {
    Long id

    String name
}
