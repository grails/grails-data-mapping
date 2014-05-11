package org.grails.datastore.gorm

import grails.gorm.CassandraEntity

import org.springframework.cassandra.core.PrimaryKeyType
import org.springframework.data.cassandra.mapping.PrimaryKeyColumn
import org.springframework.data.cassandra.mapping.Table

class CassandraTransformTest extends GroovyTestCase {

    void testBasicAnnotatedEntity() {
        def tableAnn = Basic.class.getAnnotation(Table)
        assert tableAnn != null
        def field = Basic.class.getDeclaredField("id")
        assert field != null
        def ann = field.getAnnotation(PrimaryKeyColumn)
        assert ann != null

        field = BasicWithId.class.getDeclaredField("id")
        assert field != null
        ann = field.getAnnotation(PrimaryKeyColumn)
        assert ann != null
        assert ann.type() == PrimaryKeyType.PARTITIONED
        
        shouldFail {
            Basic.class.getDeclaredField("version")
        }
    }

    void testBasicWithPrimaryField() {
        def field = BasicWithPrimaryField.class.getDeclaredField("primary")
        assert field != null
        def ann = field.getAnnotation(PrimaryKeyColumn)
        assert ann != null

        shouldFail {
            BasicWithPrimaryField.class.getDeclaredField("id")
        }
    }

    void testBasicWithCustomPrimaryKeyAndColumnNames() {
        def field = BasicWithCustomPrimaryKey.class.getDeclaredField("primary")
        assert field != null
        def ann = field.getAnnotation(PrimaryKeyColumn)
        assert ann != null
        assert ann.name() == "pri"
        assert ann.ordinal() == 0
        assert ann.type() == PrimaryKeyType.PARTITIONED

        field = BasicWithCustomPrimaryKey.class.getDeclaredField("clustered")
        assert field != null
        ann = field.getAnnotation(PrimaryKeyColumn)
        assert ann != null
        assert ann.name() == "clu"
        assert ann.ordinal() == 1
        assert ann.type() == PrimaryKeyType.CLUSTERED

        shouldFail {
            BasicWithCustomPrimaryKey.class.getDeclaredField("id")
        }
    }

    void testCustomTableMappingAndPrimaryKeyMapping() {
        Table tableAnn = Person.class.getAnnotation(Table)
        assert tableAnn != null
        assert tableAnn.value() == "the_person"
        def field = Person.class.getDeclaredField("lastname")
        assert field != null
        def ann = field.getAnnotation(PrimaryKeyColumn)
        assert ann != null
        assert ann.ordinal() == 0
        assert ann.type() == PrimaryKeyType.PARTITIONED

        field = Person.class.getDeclaredField("firstname")
        assert field != null
        ann = field.getAnnotation(PrimaryKeyColumn)
        assert ann != null
        assert ann.ordinal() == 1
        assert ann.type() == PrimaryKeyType.CLUSTERED

        shouldFail {
            Person.class.getDeclaredField("id")
        }
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
}

@CassandraEntity
class BasicWithPrimaryField {
    UUID primary

    static mapping = {  id name:"primary"  }
}

@CassandraEntity
class BasicWithCustomPrimaryKey {
    UUID primary
    UUID clustered

    static mapping = {
        id name:"primary", column:"pri", primaryKey:[ordinal:0, type:"partitioned"]
        clustered column:"clu", primaryKey:[ordinal:1, type: "clustered"]
    }
}

@CassandraEntity
class Person {
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
