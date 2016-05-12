package org.grails.gorm.rx.events

import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ApplicationListener

/**
 * @author Graeme Rocher
 * @since 6.0
 */
interface ConfigurableApplicationEventPublisher extends ApplicationEventPublisher {

    /**
     * Adds a new application listener
     *
     * @param listener The application listener
     */
    void addApplicationListener(ApplicationListener<?> listener)
}