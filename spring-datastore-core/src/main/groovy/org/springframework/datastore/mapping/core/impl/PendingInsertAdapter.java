/* Copyright (C) 2010 SpringSource
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

package org.springframework.datastore.mapping.core.impl;

import org.springframework.datastore.mapping.model.PersistentEntity;

/**
 * Provides default implementation for most of the methods in the {@link PendingInsert} interafce
 *
 * @param <E> The native entry to persist
 * 
 * @author Graeme Rocher
 * @since 1.0
 */

public abstract class PendingInsertAdapter<E, K> implements PendingInsert<E, K>{

	private PersistentEntity entity;
	private K nativeKey;
	private E nativeEntry;	
	private Runnable postOperation;
	
	public PendingInsertAdapter(PersistentEntity entity, K nativeKey,
			E nativeEntry) {
		super();
		this.entity = entity;
		this.nativeKey = nativeKey;
		this.nativeEntry = nativeEntry;
	}

	public PendingInsertAdapter(PersistentEntity entity, K nativeKey,
			E nativeEntry, Runnable postOperation) {
		super();
		this.entity = entity;
		this.nativeKey = nativeKey;
		this.nativeEntry = nativeEntry;
		this.postOperation = postOperation;
	}

	@Override
	public K getNativeKey() {
		return nativeKey;
	}
	
	@Override
	public PersistentEntity getEntity() {
		return this.entity;
	}

	@Override
	public E getNativeEntry() {
		return this.nativeEntry;
	}

	@Override
	public Runnable getPostOperation() {
		return this.postOperation;
	}

}
