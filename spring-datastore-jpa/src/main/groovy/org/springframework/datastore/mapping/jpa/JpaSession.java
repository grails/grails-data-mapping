package org.springframework.datastore.mapping.jpa;

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


import java.io.Serializable;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;

import org.springframework.datastore.mapping.core.Datastore;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.engine.EntityInterceptor;
import org.springframework.datastore.mapping.engine.Persister;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.query.Query;
import org.springframework.datastore.mapping.transactions.Transaction;

/**
 * Wraps a JPA EntityManager in the Datastore Session interface
 * 
 * @author Graeme Rocher
 * @since 1.0
 *
 */
public class JpaSession implements Session {

	private EntityManager entityManager;
	private JpaDatastore datastore;

	public JpaSession(JpaDatastore datastore, EntityManager entityManager) {
		this.entityManager = entityManager;
		this.datastore = datastore;
	}

	@Override
	public void setEntityInterceptors(List<EntityInterceptor> interceptors) {
		throw new UnsupportedOperationException("Datastore interceptors not supported, use JPA interceptors via annotations");

	}

	@Override
	public void addEntityInterceptor(EntityInterceptor interceptor) {
		throw new UnsupportedOperationException("Datastore interceptors not supported, use JPA interceptors via annotations");
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
		return entityManager.isOpen();
	}

	@Override
	public void disconnect() {
		entityManager.close();
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
		entityManager.persist(o);
		return null;
	}

	@Override
	public void refresh(Object o) {
		entityManager.refresh(o);
	}

	@Override
	public void attach(Object o) {
		entityManager.merge(o);
	}

	@Override
	public void flush() {
		entityManager.flush();
	}

	@Override
	public void clear() {
		entityManager.clear();
	}

	@Override
	public void clear(Object o) {
		// do nothing
	}

	@Override
	public boolean contains(Object o) {
		return entityManager.contains(o);
	}

	@Override
	public void setFlushMode(FlushModeType flushMode) {
		entityManager.setFlushMode(flushMode);
	}

	@Override
	public FlushModeType getFlushMode() {
		return entityManager.getFlushMode();
	}

	@Override
	public void lock(Object o) {
		entityManager.lock(o, LockModeType.WRITE);
	}

	@Override
	public void unlock(Object o) {
		// noop. Not supported in JPA

	}

	@Override
	public List<Serializable> persist(Iterable objects) {
		for (Object object : objects) {
			entityManager.persist(object);
		}
		return null;
	}

	@Override
	public <T> T retrieve(Class<T> type, Serializable key) {
		return entityManager.find(type, key);
	}

	@Override
	public <T> T proxy(Class<T> type, Serializable key) {
		return entityManager.getReference(type, key);
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
			entityManager.remove(object);
		}
	}

	@Override
	public void delete(Object obj) {
		entityManager.remove(obj);
	}

	@Override
	public List retrieveAll(Class type, Iterable keys) {
		return null;
	}

	@Override
	public List retrieveAll(Class type, Serializable... keys) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Query createQuery(Class type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getNativeInterface() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Persister getPersister(Object o) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Transaction getTransaction() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Datastore getDatastore() {
		// TODO Auto-generated method stub
		return null;
	}

}
