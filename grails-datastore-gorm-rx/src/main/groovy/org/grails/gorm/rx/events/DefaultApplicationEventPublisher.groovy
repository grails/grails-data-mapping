package org.grails.gorm.rx.events

import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener

/**
 * Simple implementation that just iterates over a sequence of listeners
 *
 * @author Graeme Rocher
 * @since 6.0
 */
class DefaultApplicationEventPublisher implements ConfigurableApplicationEventPublisher {

    private List<ApplicationListener> applicationListeners = []
    @Override
    void publishEvent(ApplicationEvent event) {
        for(listener in applicationListeners) {
            listener.onApplicationEvent(event)
        }
    }

    @Override
    void addApplicationListener(ApplicationListener<?> listener) {
        applicationListeners.add(listener)
    }
}
