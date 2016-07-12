package org.grails.datastore.gorm.multitenancy;

import grails.gorm.multitenancy.Tenants;
import org.grails.datastore.gorm.GormEnhancer;
import org.grails.datastore.mapping.core.Datastore;
import org.grails.datastore.mapping.engine.event.PersistenceEventListener;
import org.grails.datastore.mapping.engine.event.PreInsertEvent;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.model.types.TenantId;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.event.PostQueryEvent;
import org.grails.datastore.mapping.query.event.PreQueryEvent;
import org.grails.datastore.mapping.reflect.EntityReflector;
import org.springframework.context.ApplicationEvent;

import java.io.Serializable;

/**
 * An event listener that hooks into persistence events to enable discriminator based multi tenancy (ie {@link org.grails.datastore.mapping.multitenancy.MultiTenancySettings.MultiTenancyMode#MULTI}
 *
 * @author Graeme Rocher
 * @since 6.0
 */
public class MultiTenantEventListener implements PersistenceEventListener {
    protected final Datastore datastore;

    public MultiTenantEventListener(Datastore datastore) {
        this.datastore = datastore;
    }

    @Override
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return PreQueryEvent.class.isAssignableFrom(eventType) || PostQueryEvent.class.isAssignableFrom(eventType) || PreInsertEvent.class.isAssignableFrom(eventType);
    }

    @Override
    public boolean supportsSourceType(Class<?> sourceType) {
        return Datastore.class.isAssignableFrom(sourceType);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if(supportsEventType(event.getClass())) {
            Datastore datastore = (Datastore) event.getSource();
            if(event instanceof PreQueryEvent) {
                PreQueryEvent preQueryEvent = (PreQueryEvent) event;
                Query query = preQueryEvent.getQuery();

                PersistentEntity entity = query.getEntity();
                if(entity.isMultiTenant()) {
                    if(datastore == null) {
                        datastore = GormEnhancer.findDatastore(entity.getJavaClass());
                    }
                    if(supportsSourceType(datastore.getClass()) && this.datastore.equals(datastore)) {
                        TenantId tenantId = entity.getTenantId();
                        if(tenantId != null) {
                            Serializable currentId = Tenants.currentId(datastore.getClass());
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
                    if(datastore == null) {
                        datastore = GormEnhancer.findDatastore(entity.getJavaClass());
                    }
                    if(supportsSourceType(datastore.getClass()) && this.datastore.equals(datastore)) {
                        Serializable currentId = Tenants.currentId(datastore.getClass());
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

