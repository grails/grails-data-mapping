package org.grails.datastore.gorm.neo4j.converters

import org.springframework.core.convert.converter.Converter

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 01.05.11
 * Time: 23:53
 * To change this template use File | Settings | File Templates.
 */
class StringToCurrencyConverter implements Converter<String, Currency> {
    Currency convert(String source) {
        Currency.getInstance(source)
    }

}
