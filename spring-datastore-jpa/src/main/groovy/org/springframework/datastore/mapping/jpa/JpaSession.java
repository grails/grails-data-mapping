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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceException;

import org.springframework.core.convert.ConversionService;
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
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;



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
	private JpaTransactionManager transactionManager;
	private Map<Object, Map<String, Object>> entityAttributes = new ConcurrentHashMap<Object, Map<String, Object>>();
	private List<EntityInterceptor> interceptors = new ArrayList<EntityInterceptor>();
	private FlushModeType flushMode;
	private boolean connected = true;
	private TransactionStatus transaction;

	public JpaSession(JpaDatastore datastore, JpaTemplate jpaTemplate, JpaTransactionManager transactionManager) {
		this.jpaTemplate = jpaTemplate;
		this.datastore = datastore;
		this.transactionManager = transactionManager;
	}
	
	public JpaTemplate getJpaTemplate() {
		return jpaTemplate;
	}
	
	public List<EntityInterceptor> getInterceptors() {
		return interceptors;
	}

	@Override
	public void setEntityInterceptors(List<EntityInterceptor> interceptors) {
		this.interceptors  = interceptors;
	}

	@Override
	public void addEntityInterceptor(EntityInterceptor interceptor) {
		interceptors.add(interceptor);
	}

	@Override
	public void setAttribute(Object entity, String attributeName, Object value) {
		Map<String, Object> map = entityAttributes.get(entity);
		if(map == null) {
			map = new ConcurrentHashMap<String, Object>();
			entityAttributes.put(entity, map);
		}
		map.put(attributeName, value);
	}

	@Override
	public Object getAttribute(Object entity, String attributeName) {
		final Map<String, Object> map = entityAttributes.get(entity);
		if(map != null) {
			return map.get(attributeName);
		}
		return null;
	}

	@Override
	public boolean isConnected() {
		return connected;
	}

	@Override
	public void disconnect() {
		entityAttributes.clear();
		this.connected = false;
	}

	@Override
	public Transaction beginTransaction() {
		final TransactionStatus transaction = transactionManager.getTransaction(new DefaultTransactionDefinition());
		return new JpaTransaction(transactionManager, transaction);
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
	
	
	public Object merge(Object o) {
		if(o != null) {			
			final PersistentEntity persistentEntity = getMappingContext().getPersistentEntity(o.getClass().getName());
			if(persistentEntity == null) throw new InvalidDataAccessApiUsageException("Object of class ["+o.getClass()+"] is not a persistent entity");
				
			return jpaTemplate.merge(o);
		}
		else {
			throw new InvalidDataAccessApiUsageException("Object to merge cannot be null");
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
				entityAttributes.clear();
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
		this.flushMode = flushMode;
		if(flushMode == FlushModeType.AUTO) {
			jpaTemplate.setFlushEager(true);
		}
		else {
			jpaTemplate.setFlushEager(false);
		}
	}

	@Override
	public FlushModeType getFlushMode() {
		return this.flushMode;
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
		final PersistentEntity persistentEntity = getPersistentEntity(type);
		if(persistentEntity != null) {
			final ConversionService conversionService = getMappingContext().getConversionService();
			final Object id = conversionService.convert(key, persistentEntity.getIdentity().getType());			
			return jpaTemplate.find(type, id);
		}
		return null;
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
		return null;
	}

	@Override
	public Transaction getTransaction() {		
		return new JpaTransaction(transactionManager, transaction); 
	}

	@Override
	public JpaDatastore getDatastore() {
		return datastore;
	}

	public void setTransactionStatus(TransactionStatus transaction) {
		this.transaction = transaction;		
	}

}
