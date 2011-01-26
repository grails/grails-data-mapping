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


package org.grails.datastore.gorm.jpa.support;


import org.grails.datastore.gorm.support.DatastorePersistenceContextInterceptor;
import org.springframework.datastore.mapping.core.Datastore;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.transactions.Transaction;

/**
 * 
 * @author Graeme Rocher
 * @since 1.0
 *
 */
public class JpaPersistenceContextInterceptor extends
		DatastorePersistenceContextInterceptor {

	
	private Transaction transaction;
	public JpaPersistenceContextInterceptor(Datastore datastore) {
		super(datastore);
	}

	@Override
	public void init() {
		
		super.init();
		final Session currentSession = datastore.getCurrentSession();
		this.transaction = currentSession.getTransaction();
	}
	@Override
	public void flush() {
		try {
			super.flush();
			transaction.commit();
		} catch (Exception e) {
			transaction.rollback();
		}
	}



}
