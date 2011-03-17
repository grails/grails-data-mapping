package org.springframework.datastore.mapping.config.groovy

import org.springframework.beans.MutablePropertyValues
import org.springframework.datastore.mapping.reflect.NameUtils
import org.springframework.validation.DataBinder

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class MappingConfigurationBuilder {

    Object target
    Map properties = [:]
    Class propertyClass

    MappingConfigurationBuilder(target, Class propertyClass) {
        this.target = target
        this.propertyClass = propertyClass
        propertyClass.metaClass.propertyMissing = { String name, val -> }
    }

    def invokeMethod(String name, args) {
        if (args.size() > 0) {

            def setterName = NameUtils.getSetterName(name)
            if (target.respondsTo(setterName)) {
                target[name] = args.size() == 1 ? args[0] : args
            }
            else {
                if (args[0] instanceof Map) {
                    def instance = propertyClass.newInstance()
                    def binder = new DataBinder(instance)
                    binder.bind(new MutablePropertyValues(args[0]))
                    properties[name] = instance
                }
            }
        }
    }

    void evaluate(Closure callable) {
        if (callable) {
            callable.delegate = this
            callable.resolveStrategy = Closure.DELEGATE_ONLY
            callable.call()
        }
    }
}
