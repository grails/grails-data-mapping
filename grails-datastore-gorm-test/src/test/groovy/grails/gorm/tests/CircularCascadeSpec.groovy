/*
 * Copyright 2017 original authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package grails.gorm.tests

import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import grails.gorm.annotation.Entity
import grails.gorm.validation.CascadingValidator
import grails.gorm.validation.PersistentEntityValidator
import groovy.transform.NotYetImplemented
import org.grails.core.DefaultGrailsDomainClass
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.datastore.gorm.validation.constraints.eval.DefaultConstraintEvaluator
import org.grails.datastore.gorm.validation.constraints.registry.DefaultConstraintRegistry
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.validation.GrailsDomainClassValidator
import org.springframework.context.MessageSource
import org.springframework.validation.Errors
import spock.lang.Issue
import spock.lang.Specification

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class CircularCascadeSpec extends GormDatastoreSpec {

    @Issue('https://github.com/grails/grails-data-mapping/issues/967')
    @NotYetImplemented
    void "test circular cascade does not stackoverflow"() {
        given:
        SchoolPerson splinter = new SchoolPerson(name: 'Master Splinter')
        PersistentEntity entity = session.datastore.getMappingContext().getPersistentEntity(SchoolPerson.name)
        def validator = new GrailsDomainClassValidator()
        GrailsApplication grailsApplication = new DefaultGrailsApplication(SchoolPerson)
        grailsApplication.initialise()
        validator.setDomainClass(grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, SchoolPerson.name))
        validator.setGrailsApplication(grailsApplication)
        validator.setMessageSource(Mock(MessageSource))

        session.datastore.mappingContext.addEntityValidator(
                entity,
                new CascadingValidator() {
                    @Override
                    void validate(Object obj, Errors errors, boolean cascade) {
                        validator.validate(obj, errors, cascade)
                    }

                    @Override
                    boolean supports(Class<?> clazz) {
                        return validator.supports(clazz)
                    }

                    @Override
                    void validate(Object target, Errors errors) {
                        validator.validate(target, errors)
                    }
                }
        )

        SchoolPerson leo = new SchoolPerson(name: 'Leonardo')
        SchoolPerson donnie = new SchoolPerson(name: 'Donatello')
        SchoolPerson mikey = new SchoolPerson(name: 'Michelangelo')
        SchoolPerson raph = new SchoolPerson(name: 'Raphael')

        splinter.addToStudents(leo)
        splinter.addToStudents(donnie)
        splinter.addToStudents(mikey)
        splinter.addToStudents(raph)

        leo.peers = [donnie, mikey, raph]
        donnie.peers = [leo, mikey, raph]
        mikey.peers = [leo, donnie, raph]
        raph.peers = [leo, donnie, mikey]

        expect:
        splinter.save(failOnError: true)
    }

    @Issue('https://github.com/grails/grails-data-mapping/issues/967')
    void "test circular cascade does not stackoverflow with persistent entity validator"() {
        given:
        SchoolPerson splinter = new SchoolPerson(name: 'Master Splinter')
        PersistentEntity entity = session.datastore.getMappingContext().getPersistentEntity(SchoolPerson.name)
        def messageSource = Mock(MessageSource)
        messageSource.getMessage(_,_, _, _) >> 'test'
        def evaluator = new DefaultConstraintEvaluator(new DefaultConstraintRegistry(messageSource), session.datastore.mappingContext, null)
        session.datastore.getMappingContext().addEntityValidator(entity, new PersistentEntityValidator(entity, messageSource, evaluator))
        SchoolPerson leo = new SchoolPerson(name: 'Leonardo')
        SchoolPerson donnie = new SchoolPerson(name: 'Donatello')
        SchoolPerson mikey = new SchoolPerson(name: 'Michelangelo')
        SchoolPerson raph = new SchoolPerson(name: 'Raphael')

        splinter.addToStudents(leo)
        splinter.addToStudents(donnie)
        splinter.addToStudents(mikey)
        splinter.addToStudents(raph)

        leo.peers = [donnie, mikey, raph]
        donnie.peers = [leo, mikey, raph]
        mikey.peers = [leo, donnie, raph]
        raph.peers = [leo, donnie, mikey]

        expect:
        splinter.save(failOnError: true)
    }

    @Override
    List getDomainClasses() {
        [SchoolPerson]
    }
}

@Entity
class SchoolPerson {
    String name

    static belongsTo = [master: SchoolPerson]

    static hasMany = [students: SchoolPerson, peers: SchoolPerson]
    static mappedBy = [students: 'none', peers: 'none']

    static constraints = {
        master nullable: true
    }
    static mapping = {
        peers cascade: 'none'
    }
}
