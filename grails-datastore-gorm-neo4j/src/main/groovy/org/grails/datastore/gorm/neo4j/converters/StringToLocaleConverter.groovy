package org.grails.datastore.gorm.neo4j.converters

import org.springframework.core.convert.converter.Converter

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 01.05.11
 * Time: 23:53
 * To change this template use File | Settings | File Templates.
 */
class StringToLocaleConverter implements Converter<String, Locale> {
    Locale convert(String source) {
        def parts = source.split("_")
        switch (parts.size()) {
            case 1: return new Locale(parts[0])
            case 2: return new Locale(parts[0],parts[1])
            case 3: return new Locale(parts[0],parts[1],parts[2])
            default: return new Locale(source)
        }
    }

}
