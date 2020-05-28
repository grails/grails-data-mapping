package org.grails.datastore.mapping.config

import groovy.transform.Internal
import org.springframework.beans.factory.config.MethodInvokingFactoryBean

/**
 * Variant of {#link MethodInvokingFactoryBean} which returns the correct data service type instead of {@code java.lang.Object} so the Autowire with type works correctly.
 */
@Internal
class GormMethodInvokingFactoryBean extends MethodInvokingFactoryBean {

    @Override
    Class<?> getObjectType() {
        arguments[0] as Class<?>
    }
}
