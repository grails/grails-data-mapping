package grails.gorm.tests

import grails.gorm.JpaEntity 
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

@JpaEntity
class CommonTypes implements Serializable{
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
