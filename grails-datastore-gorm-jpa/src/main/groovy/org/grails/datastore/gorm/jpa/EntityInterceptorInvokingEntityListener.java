/* Copyright (C) 2011 SpringSource
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/


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
import org.springframework.datastore.mapping.jpa.JpaDatastore;
import org.springframework.datastore.mapping.jpa.JpaSession;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Adapts JPA events to the Datastore abstraction event API
 * 
 * @author Graeme Rocher
 * @since 1.0
 *
 */
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
									rollbackTransaction(jpaSession);
									
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

	void rollbackTransaction(JpaSession jpaSession) {
		jpaSession.getTransaction().rollback();
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
									rollbackTransaction(jpaSession);
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
