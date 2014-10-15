package grails.gorm.tests

import org.grails.datastore.gorm.cassandra.mapping.BasicCassandraMappingContext
import org.grails.datastore.mapping.cassandra.utils.UUIDUtil

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
		
		when: "update property"
    		now = new Date()
    		cal = new GregorianCalendar()
    		uuid = UUIDUtil.randomUUID
    		timeuuid = UUIDUtil.randomTimeUUID
		 	CommonTypes.updateProperty(ct.id, "l", 11L)
			CommonTypes.updateProperty(ct.id, "b", 11 as byte)
			CommonTypes.updateProperty(ct.id, "s", 11)
			CommonTypes.updateProperty(ct.id, "bool", false)
			CommonTypes.updateProperty(ct.id, "i", 11)
			CommonTypes.updateProperty(ct.id, "url", new URL("http://www.amazon.com"))
			CommonTypes.updateProperty(ct.id, "date", now)
			CommonTypes.updateProperty(ct.id, "c", cal)
			CommonTypes.updateProperty(ct.id, "bd", 1.1)
			CommonTypes.updateProperty(ct.id, "bi", 11)
			CommonTypes.updateProperty(ct.id, "d", 1.1)
			CommonTypes.updateProperty(ct.id, "f", 1.1)
			CommonTypes.updateProperty(ct.id, "tz", TimeZone.getTimeZone("CET"))
			CommonTypes.updateProperty(ct.id, "loc", Locale.ITALIAN)
			CommonTypes.updateProperty(ct.id, "cur", Currency.getInstance("EUR"))
			CommonTypes.updateProperty(ct.id, "uuid", uuid)
			CommonTypes.updateProperty(ct.id, "timeuuid", timeuuid)
			CommonTypes.updateProperty(ct.id, "text", "newtext")
			CommonTypes.updateProperty(ct.id, "ascii", "newascii")
			CommonTypes.updateProperty(ct.id, "varchar", "newvarchar", [flush:true])
			
			ct.discard()
			ct = CommonTypes.get(ct.id)
			 
		then:
			11L == ct.l
            (11 as byte) == ct.b
            (11 as short) == ct.s
            false == ct.bool
            11 == ct.i
            new URL("http://www.amazon.com") == ct.url
            now.time == ct.date.time
            cal == ct.c
            1.1 == ct.bd
            11 as BigInteger == ct.bi
            (1.1 as Double) == ct.d
            (1.1 as Float) == ct.f
            TimeZone.getTimeZone("CET") == ct.tz
            Locale.ITALIAN == ct.loc
            Currency.getInstance("EUR") == ct.cur        
            uuid == ct.uuid
            timeuuid == ct.timeuuid
            "newtext" == ct.text
            "newascii" == ct.ascii
            "newvarchar" == ct.varchar
    }
}

