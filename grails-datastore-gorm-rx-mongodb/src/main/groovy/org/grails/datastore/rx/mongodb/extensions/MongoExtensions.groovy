package org.grails.datastore.rx.mongodb.extensions

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

/**
 * Extensions to the MongoDB reactive driver
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class MongoExtensions {

    @CompileDynamic
    public static <T> T mapToObject(Class<T> targetType, Map<String,Object> values) {
        T t = targetType.newInstance()
        for(String name in values.keySet()) {
            if(t.respondsTo(name)) {
                t."$name"( values.get(name) )
            }
        }
        return t
    }
}
