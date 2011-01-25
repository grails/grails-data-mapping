package org.grails.datastore.gorm.jpa;

import java.util.List;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import org.grails.datastore.gorm.events.DomainEventInterceptor;
import org.springframework.datastore.mapping.core.AbstractDatastore;
import org.springframework.datastore.mapping.core.ConnectionNotFoundException;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.engine.EntityAccess;
import org.springframework.datastore.mapping.engine.EntityInterceptor;
import org.springframework.datastore.mapping.jpa.JpaSession;
import org.springframework.datastore.mapping.model.PersistentEntity;

public class EntityInterceptorInvokingEntityListener {

	@PrePersist
	public void prePersist(Object o) {
		try {
			final Session session = AbstractDatastore.retrieveSession();
			if(session instanceof JpaSession) {
				JpaSession jpaSession = (JpaSession) session;
				final PersistentEntity entity = session.getMappingContext().getPersistentEntity(o.getClass().getName());
				if(entity != null) {
					final List<EntityInterceptor> interceptors = jpaSession.getInterceptors();
					if(interceptors != null && !interceptors.isEmpty()) {						
						final EntityAccess entityAccess = new EntityAccess(entity, o);
						for (EntityInterceptor entityInterceptor : interceptors) {
							if(!(entityInterceptor instanceof DomainEventInterceptor)) {								
								if(!entityInterceptor.beforeInsert(entity, entityAccess)) {
									// TODO Throw an exception here
									break;
								}
							}
						}					
					}
				}
			}
		} catch (ConnectionNotFoundException e) {
			// ignore, shouldn't happen
		}
	}
	
	@PreUpdate
	public void preUpdate(Object o) {
		try {
			final Session session = AbstractDatastore.retrieveSession();
			if(session instanceof JpaSession) {
				JpaSession jpaSession = (JpaSession) session;
				final PersistentEntity entity = session.getMappingContext().getPersistentEntity(o.getClass().getName());
				if(entity != null) {
					final List<EntityInterceptor> interceptors = jpaSession.getInterceptors();
					if(interceptors != null && !interceptors.isEmpty()) {						
						final EntityAccess entityAccess = new EntityAccess(entity, o);
						for (EntityInterceptor entityInterceptor : interceptors) {
							if(!(entityInterceptor instanceof DomainEventInterceptor)) {
								if(!entityInterceptor.beforeUpdate(entity, entityAccess)) {
									// TODO Throw an exception here
									break;
								}								
							}
						}					
					}
				}
			}
		} catch (ConnectionNotFoundException e) {
			// ignore, shouldn't happen
		}
	}	
}
