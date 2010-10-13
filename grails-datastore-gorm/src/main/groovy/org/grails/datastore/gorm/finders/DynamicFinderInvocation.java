package org.grails.datastore.gorm.finders;

import groovy.lang.Closure;
import org.springframework.datastore.mapping.query.Query;

import java.util.List;
/**
 * Value object used to construct all the information necessary to invoke a dynamic finder
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class DynamicFinderInvocation {

    private Class javaClass;
    private String methodName;
    private Object[] arguments;
    private List<MethodExpression> expressions;
    private Closure criteria;
    private String operator;

    public DynamicFinderInvocation(Class javaClass, String methodName, Object[] arguments, List<MethodExpression> expressions, Closure criteria, String operator) {
        this.javaClass = javaClass;
        this.methodName = methodName;
        this.arguments = arguments;
        this.expressions = expressions;
        this.criteria = criteria;
        this.operator = operator;
    }

    public Class getJavaClass() {
        return javaClass;
    }

    public String getMethodName() {
        return methodName;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public List<MethodExpression> getExpressions() {
        return expressions;
    }

    public Closure getCriteria() {
        return criteria;
    }

    public String getOperator() {
        return operator;
    }
}


