package org.grails.datastore.gorm

import grails.persistence.Entity

class GormToCassandraTest {
    public static void main(String[] a) {
        
    }             
    
    @Entity
    static class ABasic {        
       String name
       Long version
       static mapping = {
            name unique:true, index:true
            version false
       }  
       
       static mapWith = "cassandra"
    }
    
    @Entity
    static class ABasicWithId {
        UUID id
        String value
        String ascii
        String varchar
        Long oneLong
        long anotherLong
        long counter
        boolean transientBoolean
        String transientString
        def service
        ATestEnum testEnum
        UUID timeuuid
        long version
        
        static mapping = {
            id type:"timeuuid"
            ascii type:'ascii'
            varchar type:'varchar'
            counter type:'counter'
            timeuuid type:"timeuuid"
            version false
        }
        static transients = [
            'transientBoolean',
            'transientString'
        ]               
    }
    
    @Entity
    static class ABasicWithPrimaryKey {
        UUID primary
        long id
        static mapping = { 
            id name:"primary" 
            version "revision_number"
        }
        static mapWith = "Cassandra"
    }
    
    @Entity
    static class ABasicCustomPrimaryKeyWithAssociation {
        UUID primary
        UUID clustered
        ABasicWithPrimaryKey association

        static mapping = {
            id name:"primary", column:"pri", primaryKey:[ordinal:0, type:"partitioned"]
            clustered column:"clu", primaryKey:[ordinal:1, type: "clustered"]
        }
        
        //static mapWith = "other"
    }

    @Entity
    static class APerson {
        String lastname
        String firstname
        String nickname
        Date birthDate
        int numberOfChildren
        List list
        Map map

        static mapping = {
            table "the_person"
            id name:"lastname", primaryKey:[ordinal:0, type:"partitioned"]
            firstname primaryKey:[ordinal:1, type: "clustered"]
            nickname index:true
            birthDate index:true
        }
    }

    @Entity
    static class ABase {        
        String value
    }

    @Entity
    static class ASub extends ABase {
        String subValue
    }
    
    @Entity
    class ATrackArtist {
        
        UUID trackId
        String track
        String artist
        String trackLengthSeconds
        String genre
        String musicFile
        
        static mapping = {
            table "track_by_artist"
            id name:"artist", primaryKey:[ordinal:0, type: "partitioned"], generator:"assigned"
            track primaryKey:[ordinal:1, type: "clustered"]
            trackId primaryKey:[ordinal:2, type:"clustered"], column:"track_id"
            version false
        }
        
        static constraints = {
        }
    }
    
    static enum ATestEnum {
        V1, V2, V3
    }
}
