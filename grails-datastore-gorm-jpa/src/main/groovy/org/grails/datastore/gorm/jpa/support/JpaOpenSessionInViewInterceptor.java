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

import javax.persistence.EntityManagerFactory;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.datastore.mapping.jpa.JpaSession;
import org.springframework.datastore.mapping.transactions.SessionHolder;
import org.springframework.datastore.mapping.web.support.OpenSessionInViewInterceptor;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.WebRequest;

/**
 * 
 * @author graemerocher
 * @since 1.0
 */
public class JpaOpenSessionInViewInterceptor extends OpenSessionInViewInterceptor implements ApplicationContextAware, Ordered {

	private PlatformTransactionManager transactionManager;
	private TransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
	private TransactionStatus transaction;
	private OpenEntityManagerInViewInterceptor openEntityManagerInViewInterceptor = new OpenEntityManagerInViewInterceptor(); 
	
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}
	
	
	public void setTransactionDefinition(TransactionDefinition transactionDefinition) {
		this.transactionDefinition = transactionDefinition;
		
	}

	@Override
	public void preHandle(WebRequest webRequest) throws Exception {
		openEntityManagerInViewInterceptor.preHandle(webRequest);
		SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(getDatastore());
		JpaSession session = null;
		if(sessionHolder == null) {
	        session = (JpaSession) getDatastore().connect();
	        sessionHolder = new SessionHolder(session);
	        TransactionSynchronizationManager.bindResource(getDatastore(), sessionHolder);
		}
		else {
			session = (JpaSession) sessionHolder.getSession();
		}
		
		this.transaction = transactionManager.getTransaction(this.transactionDefinition );
		session.setTransactionStatus(transaction);
	}


	@Override
	public void afterCompletion(WebRequest webRequest, Exception e)
			throws Exception {
		super.afterCompletion(webRequest, e);
		openEntityManagerInViewInterceptor.afterCompletion(webRequest, e);		
		if(transaction != null && !transaction.isCompleted()) {
			if(e != null) {
				   transactionManager.rollback(transaction);
			}
			else {
				transactionManager.commit(transaction);			
			}			
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.transactionManager = applicationContext.getBean(JpaTransactionManager.class);		
		openEntityManagerInViewInterceptor.setEntityManagerFactory(applicationContext.getBean(EntityManagerFactory.class));
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	
}
