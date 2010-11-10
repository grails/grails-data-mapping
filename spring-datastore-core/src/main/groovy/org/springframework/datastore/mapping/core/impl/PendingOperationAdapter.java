/* Copyright (C) 2010 SpringSource
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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.springframework.datastore.mapping.model.PersistentEntity;

/**
 * Base implementation of the {@link PendingOperation} interface
 * 
 * @author Graeme Rocher
 *
 * @param <E> The native entity type (examples could be Row, Document etc.)
 * @param <K> The native key
 */
public abstract class PendingOperationAdapter<E, K> implements PendingOperation<E, K> {

	protected PersistentEntity entity;
	protected K nativeKey;
	protected E nativeEntry;
	private List<PendingOperation<E, K>> pendingOperations = new LinkedList<PendingOperation<E, K>>();
	private List<PendingOperation<E, K>> preOperations = new LinkedList<PendingOperation<E, K>>();

	

	public PendingOperationAdapter(PersistentEntity entity,
			K nativeKey, E nativeEntry) {
		super();
		this.entity = entity;
		this.nativeKey = nativeKey;
		this.nativeEntry = nativeEntry;
	}
	
	@Override
	public List<PendingOperation<E, K>> getPreOperations() {
		return Collections.unmodifiableList(preOperations);
	}

	@Override
	public void addPreOperation(PendingOperation<E, K> preOperation) {
		preOperations.add(preOperation);
	}

	@Override
	public List<PendingOperation<E, K>> getCascadeOperations() {
		return Collections.unmodifiableList(pendingOperations);
	}

	@Override
	public void addCascadeOperation(PendingOperation<E, K> pendingOperation) {
		pendingOperations.add(pendingOperation);		
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


}