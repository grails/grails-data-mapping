package org.codehaus.groovy.grails.orm.hibernate.cfg

import org.codehaus.groovy.grails.orm.hibernate.HibernateGormInstanceApi
import org.codehaus.groovy.grails.orm.hibernate.HibernateGormValidationApi

class InstanceProxy {
    private instance
    private HibernateGormValidationApi validateApi
    private HibernateGormInstanceApi instanceApi

    private final Set<String> validateMethods

    InstanceProxy(instance, HibernateGormInstanceApi instanceApi, HibernateGormValidationApi validateApi) {
        this.instance = instance
        this.instanceApi = instanceApi
        this.validateApi = validateApi
        validateMethods = validateApi.methods*.name
        validateMethods.remove 'getValidator'
        validateMethods.remove 'setValidator'
        validateMethods.remove 'getBeforeValidateHelper'
        validateMethods.remove 'setBeforeValidateHelper'
        validateMethods.remove 'getValidateMethod'
        validateMethods.remove 'setValidateMethod'
    }

    def invokeMethod(String name, args) {
        if (validateMethods.contains(name)) {
            validateApi."$name"(instance, *args)
        }
        else {
            instanceApi."$name"(instance, *args)
        }
    }

    void setProperty(String name, val) {
        instanceApi."$name" = val
    }

    def getProperty(String name) {
        instanceApi."$name"
    }

    void putAt(String name, val) {
        instanceApi."$name" = val
    }

    def getAt(String name) {
        instanceApi."$name"
    }
}
