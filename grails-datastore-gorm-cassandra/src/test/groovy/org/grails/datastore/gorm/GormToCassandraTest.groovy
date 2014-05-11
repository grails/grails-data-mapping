package org.grails.datastore.gorm

import grails.gorm.CassandraEntity

class GormToCassandraTest {
    public static void main(String[] a) {
        def basic = new ABasic()
        def basicWithId = new ABasicWithId()
        def basicWithPrimaryKey = new ABasicWithPrimaryKey()
        def basicCustomPrimaryKey = new ABasicCustomPrimaryKey()
        def person = new APerson()     
        println "ok"   
        
    }
    
    @CassandraEntity
    static class ABasic {
        String value
    }
    
    @CassandraEntity
    static class ABasicWithId {
        UUID id
        String value
    }
    
    @CassandraEntity
    static class ABasicWithPrimaryKey {
        UUID primary
    
        static mapping = {
            id name:"primary"
        }
    }
    
    @CassandraEntity
    static class ABasicCustomPrimaryKey {
        UUID primary
        UUID clustered
        
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
    
        static mapping = {
            table "the_person"
            lastname primaryKey:[ordinal:0, type:"partitioned"]
            firstname primaryKey:[ordinal:1, type: "clustered"]
        }
    }

}
