package org.grails.datastore.mapping.config

import groovy.transform.CompileStatic
import groovy.transform.Internal
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.services.Service
import org.springframework.beans.factory.config.MethodInvokingFactoryBean

/**
 * Variant of {#link MethodInvokingFactoryBean} which returns the correct data service type instead of {@code java.lang.Object} so the Autowire with type works correctly.
 */
@Internal
@CompileStatic
class GormMethodInvokingFactoryBean extends MethodInvokingFactoryBean {

    @Override
    Class<?> getObjectType() {
        arguments[0] as Class<?>
    }

    @Override
    protected Object invokeWithTargetException() throws Exception {
        Object object = super.invokeWithTargetException()
        if (object) {
            ((Service) object).setDatastore((Datastore) targetObject)
        }
        object
    }
}
