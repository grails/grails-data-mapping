package grails.gorm.rx.mongodb

import grails.gorm.rx.mongodb.domains.Face
import grails.gorm.rx.mongodb.domains.Nose
import org.grails.datastore.mapping.model.types.OneToOne

class OneToOneSpec extends RxGormSpec {

    @Override
    List<Class> getDomainClasses() {
        return [Face, Nose]
    }

    def "Test persist and retrieve one-to-one with inverse key"() {
        given:"A domain model with a one-to-one"
        def face = new Face(name:"Joe")
        def nose = new Nose(hasFreckles: true, face:face)
        face.nose = nose
        face.save(flush:true).toBlocking().first()

        when:"The association is queried"
        face = Face.get(face.id).toBlocking().first()
        def association = Face.gormPersistentEntity.getPropertyByName('nose')
        then:"The domain model is valid"
        association instanceof OneToOne
        association.bidirectional
        association.associatedEntity.javaClass == Nose
        face != null
        face.nose.toString()
        face.nose.id == nose.id
        face.nose != null
        face.nose.hasFreckles == true

        when:"The inverse association is queried"
        nose = Nose.get(nose.id).toBlocking().first()

        then:"The domain model is valid"
        nose != null
        nose.hasFreckles == true
        nose.face != null
        nose.face.name == "Joe"
    }
}

