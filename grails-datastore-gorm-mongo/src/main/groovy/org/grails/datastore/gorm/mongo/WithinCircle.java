
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

package org.grails.datastore.gorm.mongo;

import java.util.Collection;
import java.util.List;

import org.grails.datastore.gorm.finders.MethodExpression;
import org.springframework.datastore.mapping.query.Query.Criterion;

/**
 * A dynamic finder method expression that adds the ability to query within a circle
 * 
 * @author Graeme Rocher
 * @since 1.0
 *
 */
public class WithinCircle extends MethodExpression {


	public WithinCircle(Class<?> targetClass, String propertyName) {
		super(targetClass, propertyName);
	}

	@Override
	public Criterion createCriterion() {
		return new org.springframework.datastore.mapping.mongo.query.MongoQuery.WithinCircle(propertyName, (List) arguments[0]);
	}

    @Override
    public void setArguments(Object[] arguments) {
        if(arguments.length == 0 || !(arguments[0] instanceof List))
            throw new IllegalArgumentException("Only a list of elements is supported in a 'WithinCircle' query");
        
        Collection argument = (Collection) arguments[0];
        if(argument.size() != 2) {
        	throw new IllegalArgumentException("A 'WithinCircle' query requires a two dimensional list of values");
        }
        
        super.setArguments(arguments);
    }	

}
