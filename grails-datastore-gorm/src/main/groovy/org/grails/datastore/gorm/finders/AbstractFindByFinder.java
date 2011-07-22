package org.grails.datastore.gorm.finders;

import java.util.List;
import java.util.regex.Pattern;

import org.grails.datastore.mapping.core.Datastore;
import org.grails.datastore.mapping.core.Session;
import org.grails.datastore.mapping.core.SessionCallback;
import org.grails.datastore.mapping.query.Query;

public abstract class AbstractFindByFinder extends DynamicFinder {
    public static final String OPERATOR_OR = "Or";
    public static final String OPERATOR_AND = "And";
    public static final String[] OPERATORS = new String[]{ OPERATOR_AND, OPERATOR_OR };

    protected AbstractFindByFinder(Pattern pattern,
            Datastore datastore) {
        super(pattern, OPERATORS, datastore);
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
