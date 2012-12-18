package org.grails.datastore.gorm.mongo

import grails.gorm.tests.Nose
import grails.gorm.tests.Face
import grails.gorm.tests.Pet
import grails.gorm.tests.GormDatastoreSpec

import grails.gorm.tests.Person
import com.mongodb.DBRef

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 8/16/11
 * Time: 5:07 PM
 * To change this template use File | Settings | File Templates.
 */
class OneToOneIntegritySpec extends GormDatastoreSpec {

    static {
        TEST_CLASSES  << Face << Nose
    }

    def "Test persist and retrieve unidirectional many-to-one"() {
        given:"A domain model with a many-to-one"
            def person = new Person(firstName:"Fred", lastName: "Flintstone")
            def pet = new Pet(name:"Dino", owner:person)
            person.save()
            pet.save(flush:true)
            session.clear()

        when:"The association is queried"
            pet = Pet.findByName("Dino")

        then:"The domain model is valid"
            pet != null
            pet.name == "Dino"
            pet.owner != null
            pet.owner.firstName == "Fred"

        when:"The low level API is accessed"
            def petDbo = Pet.collection.findOne(name:"Dino")
            def ownerRef =petDbo.owner
        then:"check the state is valid"
            petDbo != null
            ownerRef == person.id

    }

    def "Test persist and retrieve one-to-one with inverse key"() {
        given:"A domain model with a one-to-one"
            def face = new Face(name:"Joe")
            def nose = new Nose(hasFreckles: true, face:face)
            face.nose = nose
            face.save(flush:true)
            session.clear()

        when:"The association is queried"
            face = Face.get(face.id)

        then:"The domain model is valid"

            face != null
            face.nose != null
            face.nose.hasFreckles == true

        when:"The inverse association is queried"
            session.clear()
            nose = Nose.get(nose.id)

        then:"The domain model is valid"
            nose != null
            nose.hasFreckles == true
            nose.face != null
            nose.face.name == "Joe"

        when:"The low level API is accessed"
            def noseDbo = Nose.collection.findOne(hasFreckles:true)
            def faceRef = noseDbo.face
        then:"check the state is valid"
            noseDbo != null
            noseDbo.hasFreckles == true
            faceRef == face.id


    }
}