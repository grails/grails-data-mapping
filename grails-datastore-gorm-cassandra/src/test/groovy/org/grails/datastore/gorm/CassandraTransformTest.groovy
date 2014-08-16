package org.grails.datastore.gorm

import grails.persistence.Entity

import org.springframework.cassandra.core.PrimaryKeyType
import org.springframework.data.annotation.Transient
import org.springframework.data.cassandra.mapping.CassandraType
import org.springframework.data.cassandra.mapping.Column
import org.springframework.data.cassandra.mapping.Indexed
import org.springframework.data.cassandra.mapping.PrimaryKeyColumn
import org.springframework.data.cassandra.mapping.Table

import com.datastax.driver.core.DataType.Name

class CassandraTransformTest extends GroovyTestCase {

    void testBasicEntity() {
        def tableanno = Basic.class.getAnnotation(Table)
        assert tableanno != null
        def field = Basic.class.getDeclaredField("id")
        assert field.getType() == UUID
        assert field != null        
        def anno = field.getAnnotation(PrimaryKeyColumn)
        assert anno != null
        field = Basic.class.getDeclaredField("version")
        assertNonTransientField(field)
        
        assertNonTransientField(Basic.class.getDeclaredField("errors"))
        assertTransientField(Basic.class.getDeclaredField("tran"))
        assertTransientField(Basic.class.getDeclaredField("service"))
                
    }
    
    void testBasicMappedWithCassandra() {
        def tableanno = BasicMappedWithCassandra.class.getAnnotation(Table)
        assert tableanno != null
    }
    
    void testBasicMappedWithOther() {
        def tableanno = BasicMappedWithOther.class.getAnnotation(Table)
        assert tableanno != null
    }
    
    void testBasicWithIdAndTypes() {
        def field = BasicWithIdAndTypes.class.getDeclaredField("id")
        assert field != null
        def anno = field.getAnnotation(PrimaryKeyColumn)
        assert anno != null
        assert anno.type() == PrimaryKeyType.PARTITIONED
        
        assertCassandraTypeAnnotation(field, Name.TIMEUUID)
        assertCassandraTypeAnnotation(BasicWithIdAndTypes.class.getDeclaredField("ascii"), Name.ASCII)
        assertCassandraTypeAnnotation(BasicWithIdAndTypes.class.getDeclaredField("varchar"), Name.VARCHAR)
        assertCassandraTypeAnnotation(BasicWithIdAndTypes.class.getDeclaredField("value"), Name.TEXT)
        assertCassandraTypeAnnotation(BasicWithIdAndTypes.class.getDeclaredField("oneLong"), Name.BIGINT)
        assertCassandraTypeAnnotation(BasicWithIdAndTypes.class.getDeclaredField("anotherLong"), Name.BIGINT)
        assertCassandraTypeAnnotation(BasicWithIdAndTypes.class.getDeclaredField("counter"), Name.COUNTER)
        assertNonTransientField(BasicWithIdAndTypes.class.getDeclaredField("testEnum"))
        
        field = BasicWithIdAndTypes.class.getDeclaredField("varchar")
        anno = field.getAnnotation(Column)
        assert anno != null
        assert anno.value() == "varcharcustom"
        
        field = BasicWithIdAndTypes.class.getDeclaredField("oneLong")
        anno = field.getAnnotation(Column)
        assert anno != null
        assert anno.value() == "oneLongObject"
        
        
        assertTransientField(BasicWithIdAndTypes.class.getDeclaredField('transientBoolean'))        
        assertTransientField(BasicWithIdAndTypes.class.getDeclaredField('transientString'))  
        field = BasicWithIdAndTypes.class.getDeclaredField('version')  
        assertTransientField(field)
        assert java.lang.reflect.Modifier.isTransient(field.getModifiers()) == true                       
    }

    void testBasicWithPrimaryKey() {
        def field = BasicWithPrimaryKey.class.getDeclaredField("primary")
        assert field != null
        def anno = field.getAnnotation(PrimaryKeyColumn)
        assert anno != null
        
        field = BasicWithPrimaryKey.class.getDeclaredField("id")
        assertTransientField(field)
        assert java.lang.reflect.Modifier.isTransient(field.getModifiers()) == true
        
        field = BasicWithPrimaryKey.class.getDeclaredField("version")
        assertNonTransientField(field)
        anno = field.getAnnotation(Column)
        assert anno != null
        assert anno.value() == "revision_number"
        
    }
    
    void testBasicWithCustomPrimaryKeyUndefined() {
        def field = BasicWithCustomPrimaryKeyUndefined.class.getDeclaredField("primary")
        assert field != null
        def anno = field.getAnnotation(PrimaryKeyColumn)
        assert anno != null
        
        field = BasicWithCustomPrimaryKeyUndefined.class.getDeclaredField("id")
        assertTransientField(field)
        assert java.lang.reflect.Modifier.isTransient(field.getModifiers()) == true
        
        field = BasicWithCustomPrimaryKeyUndefined.class.getDeclaredField("version")
        assertNonTransientField(field)     
        anno = field.getAnnotation(Column)
        assert anno != null
        assert anno.value() == "revision_number"
    }
    
    void testBasicWithCustomPrimaryKeyAndAssociation() {
        def field = BasicWithCustomPrimaryKeyAndAssociation.class.getDeclaredField("primary")
        assert field != null
        def anno = field.getAnnotation(PrimaryKeyColumn)
        assert anno != null
        assert anno.name() == "pri"
        assert anno.ordinal() == 0
        assert anno.type() == PrimaryKeyType.PARTITIONED

        field = BasicWithCustomPrimaryKeyAndAssociation.class.getDeclaredField("clustered")
        assert field != null
        anno = field.getAnnotation(PrimaryKeyColumn)
        assert anno != null
        assert anno.name() == "clu"
        assert anno.ordinal() == 1
        assert anno.type() == PrimaryKeyType.CLUSTERED

        assertTransientField(BasicWithCustomPrimaryKeyAndAssociation.class.getDeclaredField("association"))

        assertTransientField(BasicWithCustomPrimaryKeyAndAssociation.class.getDeclaredField("id"))
        
    }

    void testCustomTableAndPrimaryKeyMapping() {
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

        assertTransientField(Person.class.getDeclaredField("id"))
                
    }
    
    void testAssociationCollectionAndMap() {        
        assertTransientField(Simple.class.getDeclaredField("address"))
        
        assertNonTransientField(Simple.class.getDeclaredField("dateCreated"))   
        assertNonTransientField(Simple.class.getDeclaredField("list"))
        assertNonTransientField(Simple.class.getDeclaredField("collection"))
        assertNonTransientField(Simple.class.getDeclaredField("arrayList"))
        assertNonTransientField(Simple.class.getDeclaredField("set"))
        assertNonTransientField(Simple.class.getDeclaredField("map"))
        assertNonTransientField(Simple.class.getDeclaredField("hashMap"))
    }
    
    void testInheritanceMapping() {
        def field = Sub.class.getSuperclass().getDeclaredField("id")
        assert field != null
        def anno = field.getAnnotation(PrimaryKeyColumn)
        assert anno != null
        assert anno.type() == PrimaryKeyType.PARTITIONED                     
    }
    
    private assertCassandraTypeAnnotation(def field, Name name) {
        assert field != null
        def anno = field.getAnnotation(CassandraType)
        assert anno != null
        assert anno.type() == name
    }
    
    private assertNonTransientField(def field) {        
        assert field != null
        def anno = field.getAnnotation(Transient)
        assert anno == null
    }
    
    private assertTransientField(def field) {
        assert field != null
        def anno = field.getAnnotation(Transient)
        assert anno != null
    }
}

@Entity
class Basic {
    String value
    transient tran    
    def service
}

@Entity
class BasicMappedWithCassandra {
    static mapWith = "cassandra"
}

@Entity
class BasicMappedWithOther {
    static mapWith = "other"
}

@Entity
class BasicWithIdAndTypes {
    UUID id
    String value
    String ascii
    String varchar
    Long oneLong
    long anotherLong   
    long counter
    TestEnum testEnum
    boolean transientBoolean
    String transientString
    long version
    
    static mapping = {
        id type:"timeuuid"
        ascii type:'ascii'
        varchar type:'varchar', column:'varcharcustom'
        counter type:'counter'
        oneLong column: 'oneLongObject'      
        version false 
    }
    
    static transients = ['transientBoolean', 'transientString']
}

@Entity
class BasicWithPrimaryKey {
    UUID primary   
    static mapping = {  
        id name:"primary", column:"primary_key"   
        version "revision_number"         
    }
}

@Entity
class BasicWithCustomPrimaryKeyUndefined {
    long version
    static mapping = {
        id name:"primary", column:"primary_key"      
        version "revision_number"
    }
}

@Entity
class BasicWithCustomPrimaryKeyAndAssociation {
    UUID primary
    UUID clustered
    BasicWithPrimaryKey association

    static mapping = {
        id name:"primary", column:"pri", primaryKey:[ordinal:0, type:"partitioned"]
        clustered column:"clu", primaryKey:[ordinal:1, type: "clustered"]
    }
}

@Entity
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

@Entity
class Simple {
    Long id
    Long version

    String name
    Integer age
    Address address
    Person person
    Date dateCreated
    List list
    Collection collection
    ArrayList arrayList
    Set set
    Map map
    HashMap hashMap
    
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

@Entity
class Base {
    UUID id
    String value
}

@Entity
class Sub extends Base {
    String subValue
}

enum TestEnum {
    A,B,Z
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
