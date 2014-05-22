package org.grails.datastore.gorm

import grails.gorm.CassandraEntity

import org.springframework.cassandra.core.PrimaryKeyType
import org.springframework.data.annotation.Transient
import org.springframework.data.cassandra.mapping.CassandraType
import org.springframework.data.cassandra.mapping.Indexed
import org.springframework.data.cassandra.mapping.PrimaryKeyColumn
import org.springframework.data.cassandra.mapping.Table

import com.datastax.driver.core.DataType.Name

class CassandraTransformTest extends GroovyTestCase {

    void testBasicAnnotatedEntity() {
        def tableanno = Basic.class.getAnnotation(Table)
        assert tableanno != null
        def field = Basic.class.getDeclaredField("id")
        assert field != null
        def anno = field.getAnnotation(PrimaryKeyColumn)
        assert anno != null

        shouldFail {
            Basic.class.getDeclaredField("version")
        }
    }

    void testBasicWithIdAndTypes() {
        def field = BasicWithId.class.getDeclaredField("id")
        assert field != null
        def anno = field.getAnnotation(PrimaryKeyColumn)
        assert anno != null
        assert anno.type() == PrimaryKeyType.PARTITIONED
        
        assertCassandraTypeAnnotation(field, Name.TIMEUUID)
        assertCassandraTypeAnnotation(BasicWithId.class.getDeclaredField("ascii"), Name.ASCII)
        assertCassandraTypeAnnotation(BasicWithId.class.getDeclaredField("varchar"), Name.VARCHAR)
        assertCassandraTypeAnnotation(BasicWithId.class.getDeclaredField("value"), Name.TEXT)
        assertCassandraTypeAnnotation(BasicWithId.class.getDeclaredField("oneLong"), Name.BIGINT)
        assertCassandraTypeAnnotation(BasicWithId.class.getDeclaredField("anotherLong"), Name.BIGINT)
        assertCassandraTypeAnnotation(BasicWithId.class.getDeclaredField("counter"), Name.COUNTER)
        
        
        field = BasicWithId.class.getDeclaredField('transientBoolean')
        assert field != null
        anno = field.getAnnotation(Transient)
        assert anno != null
        field = BasicWithId.class.getDeclaredField('transientString')
        assert field != null
        anno = field.getAnnotation(Transient)
        assert anno != null
                
    }

    void testBasicWithPrimaryKey() {
        def field = BasicWithPrimaryKey.class.getDeclaredField("primary")
        assert field != null
        def anno = field.getAnnotation(PrimaryKeyColumn)
        assert anno != null

        shouldFail {
            BasicWithPrimaryKey.class.getDeclaredField("id")
        }
    }

    void testBasicWithCustomPrimaryKeyColumnNamesAndAssociation() {
        def field = BasicCustomPrimaryKeyWithAssociation.class.getDeclaredField("primary")
        assert field != null
        def anno = field.getAnnotation(PrimaryKeyColumn)
        assert anno != null
        assert anno.name() == "pri"
        assert anno.ordinal() == 0
        assert anno.type() == PrimaryKeyType.PARTITIONED

        field = BasicCustomPrimaryKeyWithAssociation.class.getDeclaredField("clustered")
        assert field != null
        anno = field.getAnnotation(PrimaryKeyColumn)
        assert anno != null
        assert anno.name() == "clu"
        assert anno.ordinal() == 1
        assert anno.type() == PrimaryKeyType.CLUSTERED

        field = BasicCustomPrimaryKeyWithAssociation.class.getDeclaredField("association")
        assert field != null
        anno = field.getAnnotation(Transient)
        assert anno != null

        shouldFail {
            BasicCustomPrimaryKeyWithAssociation.class.getDeclaredField("id")
        }
    }

    void testCustomTableMappingAndPrimaryKeyMapping() {
        Table tableanno = Person.class.getAnnotation(Table)
        assert tableanno != null
        assert tableanno.value() == "the_person"
        def field = Person.class.getDeclaredField("lastname")
        assert field != null
        def anno = field.getAnnotation(PrimaryKeyColumn)
        assert anno != null
        assert anno.ordinal() == 0
        assert anno.type() == PrimaryKeyType.PARTITIONED

        field = Person.class.getDeclaredField("firstname")
        assert field != null
        anno = field.getAnnotation(PrimaryKeyColumn)
        assert anno != null
        assert anno.ordinal() == 1
        assert anno.type() == PrimaryKeyType.CLUSTERED

        field = Person.class.getDeclaredField("nickname")
        assert field != null
        anno = field.getAnnotation(Indexed)
        assert anno != null
        anno = field.getAnnotation(CassandraType)
        assert anno != null

        field = Person.class.getDeclaredField("birthDate")
        assert field != null
        anno = field.getAnnotation(Indexed)
        assert anno != null


        shouldFail {
            Person.class.getDeclaredField("id")
        }
    }
    
    private assertCassandraTypeAnnotation(def field, Name name) {
        assert field != null
        def anno = field.getAnnotation(CassandraType)
        assert anno != null
        assert anno.type() == name
    }
}

@CassandraEntity
class Basic {
    String value
}

@CassandraEntity
class BasicWithId {
    UUID id
    String value
    String ascii
    String varchar
    Long oneLong
    long anotherLong   
    long counter        
    boolean transientBoolean
    String transientString
    
    static mapping = {
        id type:"timeuuid"
        ascii type:'ascii'
        varchar type:'varchar'
        counter type:'counter'
       
    }
    static transients = ['transientBoolean', 'transientString']
}

@CassandraEntity
class BasicWithPrimaryKey {
    UUID primary

    static mapping = {  
        id name:"primary"          
    }
}

@CassandraEntity
class BasicCustomPrimaryKeyWithAssociation {
    UUID primary
    UUID clustered
    BasicWithPrimaryKey association

    static mapping = {
        id name:"primary", column:"pri", primaryKey:[ordinal:0, type:"partitioned"]
        clustered column:"clu", primaryKey:[ordinal:1, type: "clustered"]
    }
}

@CassandraEntity
class Person {
    String lastname
    String firstname
    @CassandraType(type = com.datastax.driver.core.DataType.Name.ASCII)
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

class Simple {
    Long id
    Long version

    String name
    Integer age
    Address address
    Person person
    Date dateCreated

    static transients = ['age']
    static embedded = ["address"]

    def beforeInsert() {
    }

    def afterInsert() {
    }

    def afterLoad() {
    }

    def beforeUpdate() {
    }

    def afterUpdate() {
    }

    def beforeDelete() {
    }

    def afterDelete() {
    }
}

class Address {
}


class Person2 {
    Long id

    Set pets
    Set addresses
    Set contracts
    Car car
    static hasOne = [car:Car]
    static hasMany = [pets:Pet,addresses:Address, contracts:Contract]
}


class Contract {
    Set people
    static hasMany = [people:Person]
    static belongsTo = Person
}


class Car {
    Person owner
    Integer doors = 4
    static belongsTo = [owner:Person]
}


class Pet {
    Person owner
    static belongsTo = [ owner: Person ]
}


class Force {
    String myId
    Long version
    String name

    static constraints = {
        name size:5..15, nullable:true
    }

    static mapping = {
        table "the_force"
        id name:"myId", generator:"assigned"

        name column:"the_name"
        version false
    }
}
