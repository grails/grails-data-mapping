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

import org.springframework.datastore.mapping.jpa.JpaDatastore;
import org.springframework.datastore.mapping.web.support.OpenSessionInViewInterceptor;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

public class JpaOpenSessionInViewInterceptor extends
		OpenSessionInViewInterceptor {

	private static final String TRANSACTION_STATUS_ATTRIBUTE = "org.grails.gorm.jpa.TRANSACTION_STATUS";

	@Override
	public void preHandle(WebRequest webRequest) throws Exception {
	
		super.preHandle(webRequest);
		if(hasSessionBound()) {
            JpaDatastore datastore = (JpaDatastore) getDatastore();
            
            final JpaTransactionManager transactionManager = datastore.getTransactionManager();            
            final TransactionStatus transaction = transactionManager.getTransaction(new DefaultTransactionDefinition());			
            webRequest.setAttribute(TRANSACTION_STATUS_ATTRIBUTE, transaction, RequestAttributes.SCOPE_REQUEST);
		}
	}
	
	@Override
	public void postHandle(WebRequest webRequest, ModelMap modelMap)
			throws Exception {
		
		super.postHandle(webRequest, modelMap);
		TransactionStatus transaction = (TransactionStatus) webRequest.getAttribute(TRANSACTION_STATUS_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        JpaDatastore datastore = (JpaDatastore) getDatastore();
        
        final JpaTransactionManager transactionManager = datastore.getTransactionManager();            

        if(transaction != null && !transaction.isRollbackOnly()) {
        	transactionManager.commit(transaction);
        }
	}
}
