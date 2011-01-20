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

package org.springframework.datastore.mapping.jpa.query;

import java.util.List;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.datastore.mapping.jpa.JpaSession;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.query.Query;
import org.springframework.orm.jpa.JpaTemplate;

/**
 * Query implementation for JPA
 * 
 * @author Graeme Rocher
 * @since 1.0
 *
 */
public class JpaQuery extends Query{

	public JpaQuery(JpaSession session, PersistentEntity entity) {
		super(session, entity);
		
		if(session == null) {
			throw new InvalidDataAccessApiUsageException("Argument session cannot be null");
		}
		if(entity == null) {
			throw new InvalidDataAccessApiUsageException("No persistent entity specified");
		}
		
	}
	
	@Override
	public JpaSession getSession() {
		return (JpaSession) super.getSession();
	}

	@Override
	protected List executeQuery(PersistentEntity entity, Junction criteria) {
		final JpaTemplate jpaTemplate = getSession().getJpaTemplate();
		
		
		return null;
	}

}
