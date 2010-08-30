package grails.gorm.tests

import org.junit.Test

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 30, 2010
 * Time: 11:49:01 AM
 * To change this template use File | Settings | File Templates.
 */
class CommonTypesPersistenceTests {

  @Test
  void testPersistBasicTypes() {
    def now = new Date()
    def cal = new GregorianCalendar()
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
            cur: Currency.getInstance("USD")

    )
    ct.save(flush:true)
    ct.discard()


    ct = CommonTypes.get(ct.id)

    assert ct
    assert 10L == ct.l
    assert (10 as byte) == ct.b
    assert (10 as short) == ct.s
    assert true == ct.bool
    assert 10 == ct.i
    assert new URL("http://google.com") == ct.url
    assert now == ct.date
    assert cal == ct.c
    assert 1.0 == ct.bd
    assert 10 as BigInteger == ct.bi
    assert (1.0 as Double) == ct.d
    assert (1.0 as Float) == ct.f
    assert TimeZone.getTimeZone("GMT") == ct.tz
    assert Locale.UK == ct.loc
    assert Currency.getInstance("USD") == ct.cur

  }

}

class CommonTypes {
  Long id
  Long l
  Byte b
  Short s
  Boolean bool
  Integer i
  URL url
  Date date
  Calendar c
  BigDecimal bd
  BigInteger bi
  Double d
  Float f
  TimeZone tz
  Locale loc
  Currency cur
}
