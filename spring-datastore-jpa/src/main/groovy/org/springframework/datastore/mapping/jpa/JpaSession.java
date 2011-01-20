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

package org.springframework.datastore.mapping.jpa;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceException;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.datastore.mapping.core.Datastore;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.engine.EntityAccess;
import org.springframework.datastore.mapping.engine.EntityInterceptor;
import org.springframework.datastore.mapping.engine.Persister;
import org.springframework.datastore.mapping.jpa.query.JpaQuery;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.query.Query;
import org.springframework.datastore.mapping.transactions.Transaction;
import org.springframework.orm.jpa.JpaCallback;
import org.springframework.orm.jpa.JpaTemplate;

/**
 * Wraps a JPA EntityManager in the Datastore Session interface
 * 
 * @author Graeme Rocher
 * @since 1.0
 *
 */
public class JpaSession implements Session {

	private JpaDatastore datastore;
	private JpaTemplate jpaTemplate;

	public JpaSession(JpaDatastore datastore, JpaTemplate jpaTemplate) {
		this.jpaTemplate = jpaTemplate;
		this.datastore = datastore;
	}
	
	public JpaTemplate getJpaTemplate() {
		return jpaTemplate;
	}

	@Override
	public void setEntityInterceptors(List<EntityInterceptor> interceptors) {
		// noop
	}

	@Override
	public void addEntityInterceptor(EntityInterceptor interceptor) {
		// noop
	}

	@Override
	public void setAttribute(Object entity, String attributeName, Object value) {
		// TODO Auto-generated method stub

	}

	@Override
	public Object getAttribute(Object entity, String attributeName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isConnected() {
		return jpaTemplate.getEntityManager().isOpen();
	}

	@Override
	public void disconnect() {
		jpaTemplate.getEntityManager().close();
	}

	@Override
	public Transaction beginTransaction() {
		
		return null;
	}

	@Override
	public MappingContext getMappingContext() {
		return datastore.getMappingContext();
	}

	@Override
	public Serializable persist(Object o) {
		if(o != null) {			
			final PersistentEntity persistentEntity = getMappingContext().getPersistentEntity(o.getClass().getName());
			if(persistentEntity == null) throw new InvalidDataAccessApiUsageException("Object of class ["+o.getClass()+"] is not a persistent entity");
				
			jpaTemplate.persist(o);
			return (Serializable) new EntityAccess(persistentEntity, o).getIdentifier();
		}
		else {
			throw new InvalidDataAccessApiUsageException("Object to persist cannot be null");
		}
	}

	@Override
	public void refresh(Object o) {
		if(o != null)
			jpaTemplate.refresh(o);
	}

	@Override
	public void attach(Object o) {
		if(o != null) {			
			jpaTemplate.merge(o);
		}
	}

	@Override
	public void flush() {
		jpaTemplate.flush();
	}

	@Override
	public void clear() {
		jpaTemplate.execute(new JpaCallback<Object>() {

			@Override
			public Object doInJpa(EntityManager em)
					throws PersistenceException {
				em.clear();
				return null;
			}
		});
	}

	@Override
	public void clear(Object o) {
		// do nothing
	}

	@Override
	public boolean contains(Object o) {
		return jpaTemplate.contains(o);
	}

	@Override
	public void setFlushMode(FlushModeType flushMode) {
		jpaTemplate.getEntityManager().setFlushMode(flushMode);
	}

	@Override
	public FlushModeType getFlushMode() {
		return jpaTemplate.getEntityManager().getFlushMode();
	}

	@Override
	public void lock(final Object o) {
		jpaTemplate.execute(new JpaCallback<Object>() {

			@Override
			public Object doInJpa(EntityManager em)
					throws PersistenceException {
				em.lock(o, LockModeType.WRITE);
				return null;
			}
		});
	}

	@Override
	public void unlock(Object o) {
		// noop. Not supported in JPA

	}

	@Override
	public List<Serializable> persist(final Iterable objects) {
		return jpaTemplate.execute(new JpaCallback<List<Serializable>>() {

			@Override
			public List<Serializable> doInJpa(EntityManager em)
					throws PersistenceException {
				List<Serializable> identifiers = new ArrayList<Serializable>();
				for (Object object : objects) {
					identifiers.add(persist(object));
				}
				return identifiers;
			}
		});
		
	}

	@Override
	public <T> T retrieve(Class<T> type, Serializable key) {
		return jpaTemplate.find(type, key);
	}

	@Override
	public <T> T proxy(Class<T> type, Serializable key) {
		return jpaTemplate.getReference(type, key);
	}

	@Override
	public <T> T lock(Class<T> type, Serializable key) {
		final T obj = retrieve(type, key);
		lock(obj);
		return obj;
	}

	@Override
	public void delete(Iterable objects) {
		for (Object object : objects) {
			jpaTemplate.remove(object);
		}
	}

	@Override
	public void delete(Object obj) {
		jpaTemplate.remove(obj);
	}

	@Override
	public List retrieveAll(Class type, Iterable keys) {
		if(keys instanceof List) {
			return retrieveAll(getPersistentEntity(type), (List)keys);
		}
		else {
			List identifierList = new ArrayList();
			for (Object key : keys) {
				identifierList.add(key);
			}
			return retrieveAll(getPersistentEntity(type), identifierList);
		}
	}

	public PersistentEntity getPersistentEntity(Class type) {
		return getMappingContext().getPersistentEntity(type.getName());
	}

	@Override
	public List retrieveAll(Class type, Serializable... keys) {
		if(type != null) {			
			final PersistentEntity persistentEntity = getPersistentEntity(type);			
			if(persistentEntity != null) {				
				final List<Serializable> identifiers = Arrays.asList(keys);
				return retrieveAll(persistentEntity, identifiers);
			}
		}
		return Collections.emptyList();
	}

	public List retrieveAll(final PersistentEntity persistentEntity,
			final List<Serializable> identifiers) {
		return createQuery(persistentEntity.getJavaClass())
				.in(	persistentEntity
							.getIdentity()
							.getName(), 
							identifiers)
				.list();
	}

	@Override
	public Query createQuery(Class type) {
		return new JpaQuery(this, getPersistentEntity(type));
	}

	@Override
	public Object getNativeInterface() {
		return jpaTemplate;
	}

	@Override
	public Persister getPersister(Object o) {
		throw new UnsupportedOperationException("Method getPersister not supported");
	}

	@Override
	public Transaction getTransaction() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Datastore getDatastore() {
		return datastore;
	}

}
