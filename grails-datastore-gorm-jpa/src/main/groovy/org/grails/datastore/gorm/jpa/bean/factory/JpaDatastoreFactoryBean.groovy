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


package org.grails.datastore.gorm.jpa.bean.factory

import javax.persistence.EntityManagerFactory 
import org.springframework.beans.factory.FactoryBean 
import org.springframework.context.ApplicationContext 
import org.springframework.context.ApplicationContextAware 
import org.springframework.datastore.mapping.jpa.JpaDatastore 
import org.springframework.datastore.mapping.model.MappingContext 
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager 

class JpaDatastoreFactoryBean implements FactoryBean<JpaDatastore>,ApplicationContextAware{

	EntityManagerFactory entityManagerFactory
	MappingContext mappingContext
	ApplicationContext applicationContext
	
	@Override
	public JpaDatastore getObject() throws Exception {
		def transactionManager = applicationContext.getBean(JpaTransactionManager)
		return new JpaDatastore( mappingContext, entityManagerFactory, transactionManager )
	}

	@Override
	public Class<?> getObjectType() { JpaDatastore }

	@Override
	public boolean isSingleton() { true }

	
}
