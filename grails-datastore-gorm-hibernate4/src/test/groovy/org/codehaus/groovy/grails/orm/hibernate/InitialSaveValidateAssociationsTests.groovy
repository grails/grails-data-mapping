package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

import static junit.framework.Assert.*

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class InitialSaveValidateAssociationsTests extends AbstractGrailsHibernateTests {

    @Test
    void testDontSaveInvalidAssociationWithInitialSave() {
        def  album1 = InitialSaveValidateAssociationsAlbum.newInstance(title:"Sam's Town")
        album1.addToSongs(InitialSaveValidateAssociationsSong.newInstance(title:"Sam's Town", duration:-1))
        album1.save(flush:true)

        session.clear()

        assertEquals 0, InitialSaveValidateAssociationsAlbum.count()
        assertEquals 0, InitialSaveValidateAssociationsSong.count()
    }

    @Override
    protected getDomainClasses() {
        [InitialSaveValidateAssociationsAlbum, InitialSaveValidateAssociationsSong]
    }
}

@Entity
class InitialSaveValidateAssociationsAlbum {
    Long id
    Long version

    String title
    List songs
    static hasMany = [songs:InitialSaveValidateAssociationsSong]
}

@Entity
class InitialSaveValidateAssociationsSong {
    Long id
    Long version

    String title
    Integer duration
    static belongsTo = [InitialSaveValidateAssociationsAlbum]
    static constraints = {
        title blank:false
        duration(min:1)
    }
}
