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
package org.grails.datastore.gorm.finders;

import groovy.lang.Closure;
import org.springframework.datastore.mapping.core.Datastore;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.query.Query;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Finder used to return a single result
 */
public class FindByFinder extends DynamicFinder{

    private static final String OPERATOR_OR = "Or";
    private static final String OPERATOR_AND = "And";
    private static final String METHOD_PATTERN = "(findBy)([A-Z]\\w*)";
    private static final String[] OPERATORS = new String[]{ OPERATOR_AND, OPERATOR_OR };
    
    Datastore datastore;

    public FindByFinder(Datastore datastore) {
        super(Pattern.compile(METHOD_PATTERN), OPERATORS);
        this.datastore = datastore;
    }

    @Override
    protected Object doInvokeInternalWithExpressions(Class clazz, String methodName, Object[] remainingArguments, List<MethodExpression> expressions, Closure additionalCriteria, String operatorInUse) {

        Session currentSession = datastore.getCurrentSession();

        Query q = currentSession.createQuery(clazz);
        applyAdditionalCriteria(q, additionalCriteria);
        configureQueryWithArguments(clazz, q, remainingArguments);

        if(operatorInUse != null && operatorInUse.equals(OPERATOR_OR)) {
            if (firstExpressionIsRequiredBoolean()) {
                MethodExpression expression = expressions.remove(0);
                q.add(expression.createCriterion());
            }

            Query.Junction disjunction = q.disjunction();

            for (MethodExpression expression : expressions) {
                disjunction.add(expression.createCriterion());
            }

        }
        else {
            for (MethodExpression expression : expressions) {
                q.add( expression.createCriterion() );
            }
        }

        q.max(1);
        List results = q.list();
        if(!results.isEmpty()) return results.get(0);
        return null;
    }

    private boolean firstExpressionIsRequiredBoolean() {
        return false;
    }

}
