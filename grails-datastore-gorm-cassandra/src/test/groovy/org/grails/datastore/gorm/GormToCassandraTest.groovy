package org.grails.datastore.gorm

import grails.gorm.CassandraEntity

import org.springframework.data.cassandra.mapping.CassandraType

class GormToCassandraTest {
    public static void main(String[] a) {        
        println "ok"
    }
    
    static enum ATestEnum {
        V1, V2, V3
    }
    
    @CassandraEntity
    static class ABasic {        
       String name
       
       static mapping = {
            name unique:true, index:true
       }  
    }

    @CassandraEntity
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
        
        static mapping = {
            id type:"timeuuid"
            ascii type:'ascii'
            varchar type:'varchar'
            counter type:'counter'
        }
        static transients = [
            'transientBoolean',
            'transientString'
        ]
    }

    @CassandraEntity
    static class ABasicWithPrimaryKey {
        UUID primary
        long id
        static mapping = { id name:"primary" }
    }

    @CassandraEntity
    static class ABasicCustomPrimaryKeyWithAssociation {
        UUID primary
        UUID clustered
        ABasicWithPrimaryKey association

        static mapping = {
            id name:"primary", column:"pri", primaryKey:[ordinal:0, type:"partitioned"]
            clustered column:"clu", primaryKey:[ordinal:1, type: "clustered"]
        }
    }

    @CassandraEntity
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

    @CassandraEntity
    static class ABase {
        UUID id
        String value
    }

    @CassandraEntity
    static class ASub extends ABase {
        String subValue
    }
}
