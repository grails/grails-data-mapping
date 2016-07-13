package org.grails.gorm.rx.events

import grails.gorm.rx.multitenancy.Tenants
import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.mapping.core.Datastore
import org.grails.datastore.mapping.engine.event.PersistenceEventListener
import org.grails.datastore.mapping.engine.event.PreInsertEvent
import org.grails.datastore.mapping.model.PersistentEntity
import org.grails.datastore.mapping.model.types.TenantId
import org.grails.datastore.mapping.query.Query
import org.grails.datastore.mapping.query.event.PostQueryEvent
import org.grails.datastore.mapping.query.event.PreQueryEvent
import org.grails.datastore.mapping.reflect.EntityReflector
import org.grails.datastore.rx.RxDatastoreClient
import org.grails.gorm.rx.api.RxGormEnhancer
import org.springframework.context.ApplicationEvent
import org.springframework.util.Assert

/**
 * Multi tenant event listener for RxGORM
 *
 * @author Graeme Rocher
 * @since 6.0
 */
@CompileStatic
class MultiTenantEventListener implements PersistenceEventListener {
    protected final RxDatastoreClient datastoreClient;

    public MultiTenantEventListener(RxDatastoreClient datastoreClient) {
        this.datastoreClient = datastoreClient;
    }

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return PreQueryEvent.class.isAssignableFrom(eventType) || PostQueryEvent.class.isAssignableFrom(eventType) || PreInsertEvent.class.isAssignableFrom(eventType);
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return RxDatastoreClient.class.isAssignableFrom(sourceType);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if(supportsEventType(event.getClass())) {
            RxDatastoreClient datastoreClient = (RxDatastoreClient) event.getSource();
            Assert.notNull(datastoreClient, "Datastore client should never be null from source event")
            if(event instanceof PreQueryEvent) {
                PreQueryEvent preQueryEvent = (PreQueryEvent) event;
                Query query = preQueryEvent.getQuery();

                PersistentEntity entity = query.getEntity();
                if(entity.isMultiTenant()) {
                    if(supportsSourceType(datastoreClient.getClass()) && this.datastoreClient.equals(datastoreClient)) {
                        TenantId tenantId = entity.getTenantId();
                        if(tenantId != null) {
                            Serializable currentId = Tenants.currentId(datastoreClient.getClass());
                            query.eq(tenantId.getName(), currentId );
                        }
                    }
                }
            }
            else if(event instanceof PreInsertEvent) {
                PreInsertEvent preInsertEvent = (PreInsertEvent) event;
                PersistentEntity entity = preInsertEvent.getEntity();
                if(entity.isMultiTenant()) {
                    TenantId tenantId = entity.getTenantId();
                    EntityReflector reflector = entity.getReflector();
                    if(supportsSourceType(datastoreClient.getClass()) && this.datastoreClient.equals(datastoreClient)) {
                        Serializable currentId = Tenants.currentId(datastoreClient.getClass());
                        reflector.setProperty(preInsertEvent.getEntityObject(), tenantId.getName(), currentId);
                    }
                }
            }
        }
    }

    @Override
    public int getOrder() {
        return DEFAULT_ORDER;
    }
}


