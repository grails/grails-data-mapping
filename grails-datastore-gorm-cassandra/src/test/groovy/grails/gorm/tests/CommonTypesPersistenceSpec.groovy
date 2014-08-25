package grails.gorm.tests

import org.grails.datastore.gorm.cassandra.mapping.BasicCassandraMappingContext
import org.grails.datastore.mapping.cassandra.uuid.UUIDUtil

import com.datastax.driver.core.DataType

/**
 * @author graemerocher
 */
class CommonTypesPersistenceSpec extends GormDatastoreSpec {

   def testMapping() {
        when:
            BasicCassandraMappingContext springCassandraMappingContext = setupClass.cassandraDatastore.mappingContext.springCassandraMappingContext                  
            def persistentEntity = springCassandraMappingContext.getExistingPersistentEntity(CommonTypes.class)          
        then:
            persistentEntity.getPersistentProperty("l").getDataType() == DataType.bigint()
            persistentEntity.getPersistentProperty("b").getDataType() == DataType.cint()
            persistentEntity.getPersistentProperty("s").getDataType() == DataType.cint()
            persistentEntity.getPersistentProperty("bool").getDataType() == DataType.cboolean()
            persistentEntity.getPersistentProperty("i").getDataType() == DataType.cint()
            persistentEntity.getPersistentProperty("url").getDataType() == DataType.text()
            persistentEntity.getPersistentProperty("date").getDataType() == DataType.timestamp()
            persistentEntity.getPersistentProperty("c").getDataType() == DataType.text()
            persistentEntity.getPersistentProperty("bd").getDataType() == DataType.decimal()
            persistentEntity.getPersistentProperty("bi").getDataType() == DataType.varint()
            persistentEntity.getPersistentProperty("d").getDataType() == DataType.cdouble()
            persistentEntity.getPersistentProperty("f").getDataType() == DataType.cfloat()
            persistentEntity.getPersistentProperty("tz").getDataType() == DataType.text()
            persistentEntity.getPersistentProperty("loc").getDataType() == DataType.text()
            persistentEntity.getPersistentProperty("cur").getDataType() == DataType.text()
            persistentEntity.getPersistentProperty("id").getDataType() == DataType.uuid()
            persistentEntity.getPersistentProperty("uuid").getDataType() == DataType.uuid()
            persistentEntity.getPersistentProperty("timeuuid").getDataType() == DataType.timeuuid()
            persistentEntity.getPersistentProperty("text").getDataType() == DataType.text()
            persistentEntity.getPersistentProperty("ascii").getDataType() == DataType.ascii()
            persistentEntity.getPersistentProperty("varchar").getDataType() == DataType.varchar()
            persistentEntity.getPersistentProperty("testEnum").getDataType() == DataType.text()
            persistentEntity.getPersistentProperty("transientBoolean") == null
            persistentEntity.getPersistentProperty("transientString") == null
            persistentEntity.getPersistentProperty("tran") == null
            persistentEntity.getPersistentProperty("service") == null
   }
   
   def testPersistBasicTypes() {
        given:
            def now = new Date()
            def cal = new GregorianCalendar()
            def uuid = UUIDUtil.randomUUID
            def timeuuid = UUIDUtil.randomTimeUUID
            def ct = new CommonTypes(
                l: 10L,
                b: 10 as byte,
                s: 10 as short,
                bool: true,
                i: 10,
                url: new URL("http://google.com"),
                date: now,
                c: cal,
                bd: 1.0,
                bi: 10 as BigInteger,
                d: 1.0 as Double,
                f: 1.0 as Float,
                tz: TimeZone.getTimeZone("GMT"),
                loc: Locale.UK,
                cur: Currency.getInstance("USD"),      
                uuid: uuid,
                timeuuid : timeuuid,
                text: "text",
                ascii: "ascii",
                varchar: "varchar"          
            )

        when:
            ct.save(flush:true)
            ct.discard()
            ct = CommonTypes.get(ct.id)

        then:
            ct
            10L == ct.l
            (10 as byte) == ct.b
            (10 as short) == ct.s
            true == ct.bool
            10 == ct.i
            new URL("http://google.com") == ct.url
            now.time == ct.date.time
            cal == ct.c
            1.0 == ct.bd
            10 as BigInteger == ct.bi
            (1.0 as Double) == ct.d
            (1.0 as Float) == ct.f
            TimeZone.getTimeZone("GMT") == ct.tz
            Locale.UK == ct.loc
            Currency.getInstance("USD") == ct.cur        
            uuid == ct.uuid
            timeuuid == ct.timeuuid
            "text" == ct.text
            "ascii" == ct.ascii
            "varchar" == ct.varchar    
    }
}

