package org.grails.datastore.mapping.cassandra

import grails.gorm.tests.Artist
import grails.gorm.tests.GormDatastoreSpec

import org.springframework.dao.DuplicateKeyException

class BasicPersistenceCompositeKeySpec extends GormDatastoreSpec {  
    
    void testBasicPersistenceOperations() {
        when: "read non existent"
        	Artist artist1 = session.retrieve(Artist, [firstLetter: "A", artist: "artist"])

        then:
            artist1 == null
        
        when: "save and flush"
            artist1 = new Artist(firstLetter: "N", artist: "Natalie Cole", age: 30)   
            session.persist(artist1)
            artist1 = new Artist(firstLetter: "N", artist: "Nat King", age: 35)
            session.persist(artist1)
            artist1 = new Artist(firstLetter: "R", artist: "Ram Jam", age: 40)
            session.persist(artist1)
            artist1 = new Artist(firstLetter: "E", artist: "Earl King", age: 45)
            session.persist(artist1)
            session.flush()    
            session.clear()
            Artist artist2 = session.retrieve(Artist, [firstLetter:"E"])
			
        then:    
            artist2 != null
            artist2.firstLetter == "E"
            artist2.artist == "Earl King"
            artist2.age == 45
        
        when: "read more than one row with partial key"
            artist2 = session.retrieve(Artist, [firstLetter:"N"])
        
        then:
            thrown DuplicateKeyException
            
        when: "read with complete composite key"
            artist2 = session.retrieve(Artist, [firstLetter: "N", artist: "Nat King"])
        
        then:
            artist2 != null
            artist2.firstLetter == "N"
            artist2.artist == "Nat King"
            artist2.age == 35
                   
        when: "update no flush"          
            artist1.age = 46
            artist2.age = 36
            session.persist(artist1)
            session.persist(artist2)
            def artist1cached = session.retrieve(Artist, [firstLetter: artist1.firstLetter, artist: artist1.artist])
            def artist2cached = session.retrieve(Artist, [firstLetter: artist2.firstLetter, artist: artist2.artist])
			
        then:
            artist1cached == artist1
            artist2cached == artist2
            
        when: "flush and persist to db and read again"
            session.flush()
            session.clear()
            artist1 = session.retrieve(Artist, [firstLetter: artist1.firstLetter, artist: artist1.artist])
            artist2 = session.retrieve(Artist, [firstLetter: artist2.firstLetter, artist: artist2.artist])
            
        then:
            artist1 != null
            artist1.firstLetter == "E"
            artist1.artist == "Earl King"
            artist1.age == 46
        
            artist2 != null            
            artist2.firstLetter == "N"
            artist2.artist == "Nat King"
            artist2.age == 36
        
        when: "delete"
            session.delete(artist1)
            session.delete(artist2)
            session.flush()
            session.clear()
            artist1 = session.retrieve(Artist, [firstLetter: artist1.firstLetter, artist: artist1.artist])
            artist2 = session.retrieve(Artist, [firstLetter: artist2.firstLetter, artist: artist2.artist])
			
        then:
             artist1 == null
             artist2 == null
        
    }
}
