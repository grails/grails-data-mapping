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

import java.util.List;
import java.util.regex.Pattern;

import org.grails.datastore.gorm.SessionCallback;
import org.springframework.datastore.mapping.core.Datastore;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.query.Query;

/**
 * Finder used to return a single result
 */
public class FindByFinder extends DynamicFinder {

    private static final String OPERATOR_OR = "Or";
    private static final String OPERATOR_AND = "And";
    private static final String METHOD_PATTERN = "(findBy)([A-Z]\\w*)";
    private static final String[] OPERATORS = new String[]{ OPERATOR_AND, OPERATOR_OR };

    public FindByFinder(final Datastore datastore) {
        super(Pattern.compile(METHOD_PATTERN), OPERATORS, datastore);
    }

    @Override
    protected Object doInvokeInternal(final DynamicFinderInvocation invocation) {
        return execute(new SessionCallback<Object>() {
            public Object doInSession(final Session session) {
                Query q = buildQuery(invocation, session);
                return invokeQuery(q);
            }
        });
    }

    protected Object invokeQuery(Query q) {
        q.max(1);

        List results = q.list();
        if (results.isEmpty()) {
            return null;
        }

        return results.get(0);
    }

    private boolean firstExpressionIsRequiredBoolean() {
        return false;
    }

    public Query buildQuery(DynamicFinderInvocation invocation, Session session) {
        final Class clazz = invocation.getJavaClass();
        Query q = session.createQuery(clazz);
        applyAdditionalCriteria(q, invocation.getCriteria());
        configureQueryWithArguments(clazz, q, invocation.getArguments());

        final String operatorInUse = invocation.getOperator();

        if (operatorInUse != null && operatorInUse.equals(OPERATOR_OR)) {
            if (firstExpressionIsRequiredBoolean()) {
                MethodExpression expression = invocation.getExpressions().remove(0);
                q.add(expression.createCriterion());
            }

            Query.Junction disjunction = q.disjunction();

            for (MethodExpression expression : invocation.getExpressions()) {
                disjunction.add(expression.createCriterion());
            }
        }
        else {
            for (MethodExpression expression : invocation.getExpressions()) {
                q.add( expression.createCriterion() );
            }
        }
        return q;
    }
}
