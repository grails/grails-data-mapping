package org.grails.datastore.gorm

import grails.gorm.CassandraEntity

import org.springframework.data.cassandra.mapping.CassandraType

class GormToCassandraTest {
    public static void main(String[] a) {
        def basic = new ABasic()
        def basicWithId = new ABasicWithId()
        def basicWithPrimaryKey = new ABasicWithPrimaryKey()
        def basicCustomPrimaryKey = new ABasicCustomPrimaryKeyWithAssociation()
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
        Long oneLong
        long anotherLong
        @CassandraType(type = com.datastax.driver.core.DataType.Name.COUNTER)
        long counter
        @CassandraType(type = com.datastax.driver.core.DataType.Name.ASCII)
        String ascii
        boolean transientBoolean
        String transientString

        static transients = [
            'transientBoolean',
            'transientString'
        ]
    }

    @CassandraEntity
    static class ABasicWithPrimaryKey {
        UUID primary

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

        static mapping = {
            table "the_person"
            id name:"lastname", primaryKey:[ordinal:0, type:"partitioned"]
            firstname primaryKey:[ordinal:1, type: "clustered"]
            nickname index:true
            birthDate index:true
        }
    }
}
