package org.codehaus.groovy.grails.orm.hibernate.cfg

import groovy.transform.CompileStatic

import org.codehaus.groovy.grails.orm.hibernate.HibernateGormInstanceApi
import org.codehaus.groovy.grails.orm.hibernate.HibernateGormValidationApi

@CompileStatic
class InstanceProxy {
    private instance
    private HibernateGormValidationApi validateApi
    private HibernateGormInstanceApi instanceApi

    private final Set<String> validateMethods

    InstanceProxy(instance, HibernateGormInstanceApi instanceApi, HibernateGormValidationApi validateApi) {
        this.instance = instance
        this.instanceApi = instanceApi
        this.validateApi = validateApi
        validateMethods = validateApi.methods*.name as Set<String>
        validateMethods.remove 'getValidator'
        validateMethods.remove 'setValidator'
        validateMethods.remove 'getBeforeValidateHelper'
        validateMethods.remove 'setBeforeValidateHelper'
        validateMethods.remove 'getValidateMethod'
        validateMethods.remove 'setValidateMethod'
    }

    def invokeMethod(String name, args) {
        if (validateMethods.contains(name)) {
            validateApi.invokeMethod(name, [instance, *args] as Object[])
        }
        else {
            instanceApi.invokeMethod(name, [instance, *args] as Object[])
        }
    }

    void setProperty(String name, val) {
        instanceApi.setProperty(name, val)
    }

    def getProperty(String name) {
        instanceApi.getProperty(name)
    }

    void putAt(String name, val) {
        instanceApi.setProperty(name, val)
    }

    def getAt(String name) {
        instanceApi.getProperty(name)
    }
}
