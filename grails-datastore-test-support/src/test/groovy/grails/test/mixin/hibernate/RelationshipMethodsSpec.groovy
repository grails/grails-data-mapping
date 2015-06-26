package grails.test.mixin.hibernate

import grails.persistence.Entity
import grails.test.mixin.TestMixin
import grails.test.mixin.gorm.Domain
import spock.lang.Ignore
import spock.lang.Specification

@Domain([Painting, Gallery, Painter])
@TestMixin(HibernateTestMixin)
@Ignore
class RelationshipMethodsSpec extends Specification {
    void "addTo methods should be supported"() {
        given:
        Painter painter = new Painter(name: "Pieter Bruegel").save(failOnError: true)
        Gallery gallery = new Gallery(name: "Kunsthistorisches Museum").save(failOnError: true)

        when:
        Painting painting = new Painting(title: "The Hunters in the Snow", painter: painter, gallery: gallery)
        painter.addToPaintings(painting)
        gallery.addToPaintings(painting)

        then:
        assert painting.save()
        assert painting.id != null
        assert painting.painter.name == painter.name
        assert painting.gallery.name == gallery.name
        assert painter.paintings.contains(painting)
        assert gallery.paintings.contains(painting)
        
        where:
        counter << (1..10).flatten()
    }
}

@Entity
class Painting {
    String title
    static belongsTo = [painter: Painter, gallery: Gallery]
}

@Entity
class Painter {
    String name
    static hasMany = [paintings: Painting]
}

@Entity
class Gallery {
    String name
    static hasMany = [paintings: Painting]
}
