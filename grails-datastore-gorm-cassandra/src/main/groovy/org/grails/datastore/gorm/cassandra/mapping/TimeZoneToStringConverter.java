package org.grails.datastore.gorm.cassandra.mapping;

import java.util.TimeZone;

import org.springframework.core.convert.converter.Converter;

public class TimeZoneToStringConverter implements Converter<TimeZone, String> {
	public String convert(TimeZone source) {
		return source.getID();
	}
}