package org.springframework.datastore.mapping.core.impl;

import org.springframework.datastore.mapping.model.PersistentEntity;

public interface PendingOperation<E, K> {

	/**
	 * @return The {@link PersistentEntity} being inserted
	 */
	public abstract PersistentEntity getEntity();

	public abstract K getNativeKey();

	public abstract E getNativeEntry();

	public abstract Runnable getPostOperation();

}