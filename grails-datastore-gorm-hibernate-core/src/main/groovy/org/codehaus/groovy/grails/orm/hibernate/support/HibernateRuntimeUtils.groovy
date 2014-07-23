package org.codehaus.groovy.grails.orm.hibernate.support

import groovy.transform.CompileStatic
import org.codehaus.groovy.runtime.StringGroovyMethods
import org.springframework.core.convert.ConversionService

/**
 * Utility methods used at runtime by the GORM for Hibernate implementation
 *
 * @author Graeme Rocher
 * @since 4.0
 */
@CompileStatic
class HibernateRuntimeUtils {


    static Object convertValueToType(Object passedValue, Class targetType, ConversionService conversionService) {
        // workaround for GROOVY-6127, do not assign directly in parameters before it's fixed
        Object value = passedValue
        if(targetType != null && value != null && !(value in targetType)) {
            if (value instanceof CharSequence) {
                value = value.toString()
                if(value in targetType) {
                    return value
                }
            }
            try {
                if (value instanceof Number && (targetType==Long || targetType==Integer)) {
                    if(targetType == Long) {
                        value = ((Number)value).toLong()
                    } else {
                        value = ((Number)value).toInteger()
                    }
                } else if (value instanceof String && targetType in Number) {
                    String strValue = value.trim()
                    if(targetType == Long) {
                        value = Long.parseLong(strValue)
                    } else if (targetType == Integer) {
                        value = Integer.parseInt(strValue)
                    } else {
                        value = StringGroovyMethods.asType(strValue, targetType)
                    }
                } else {
                    value = conversionService.convert(value, targetType)
                }
            } catch (e) {
                // ignore
            }
        }
        return value
    }
}
