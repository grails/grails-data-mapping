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

import grails.gorm.CriteriaBuilder;
import groovy.lang.Closure;
import groovy.lang.MissingMethodException;
import org.springframework.core.convert.ConversionService;
import org.springframework.datastore.mapping.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of dynamic finders
 */
public abstract class DynamicFinder implements FinderMethod{

    public static final String ARGUMENT_MAX = "max";
    public static final String ARGUMENT_OFFSET = "offset";
    public static final String ARGUMENT_ORDER = "order";
    public static final String ARGUMENT_SORT = "sort";
    public static final String ORDER_DESC = "desc";
    public static final String ORDER_ASC = "asc";
    public static final String ARGUMENT_FETCH = "fetch";
    public static final String ARGUMENT_IGNORE_CASE = "ignoreCase";
    public static final String ARGUMENT_CACHE = "cache";
    public static final String ARGUMENT_LOCK = "lock";

    protected Pattern pattern;
    private Pattern[] operatorPatterns;
    private String[] operators;
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    public DynamicFinder(Pattern pattern, String[] operators) {
        this.pattern = pattern;
        this.operators = operators;
        this.operatorPatterns = new Pattern[operators.length];
        for (int i = 0; i < operators.length; i++) {
            operatorPatterns[i] = Pattern.compile("(\\w+)("+operators[i]+")(\\p{Upper})(\\w+)");
        }

    }


    public void setPattern(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    public boolean isMethodMatch(String methodName) {
        return this.pattern.matcher(methodName.subSequence(0, methodName.length())).find();
    }

    public Object invoke(final Class clazz, String methodName, Closure additionalCriteria, Object[] arguments) {
        List expressions = new ArrayList();
        if (arguments == null) arguments = EMPTY_OBJECT_ARRAY;
        Matcher match = pattern.matcher(methodName);
        // find match
        match.find();

        String[] queryParameters;
        int totalRequiredArguments = 0;
        // get the sequence clauses
        final String querySequence;
        int groupCount = match.groupCount();
        if (groupCount == 6) {
            String booleanProperty = match.group(3);
            if (booleanProperty == null) {
                booleanProperty = match.group(6);
                querySequence = null;
            }
            else {
                querySequence = match.group(5);
            }
            Boolean arg = Boolean.TRUE;
            if (booleanProperty.matches("Not[A-Z].*")) {
                booleanProperty = booleanProperty.substring(3);
                arg = Boolean.FALSE;
            }
            MethodExpression booleanExpression = MethodExpression.create(clazz, booleanProperty );
            booleanExpression.setArguments(new Object[]{arg});
            expressions.add(booleanExpression);
        }
        else {
            querySequence = match.group(2);
        }
        // if it contains operator and split
        boolean containsOperator = false;
        String operatorInUse = null;
        if (querySequence != null) {
            for (int i = 0; i < operators.length; i++) {
                Matcher currentMatcher = operatorPatterns[i].matcher(querySequence);
                if (currentMatcher.find()) {
                    containsOperator = true;
                    operatorInUse = operators[i];

                    queryParameters = new String[2];
                    queryParameters[0] = currentMatcher.group(1);
                    queryParameters[1] = currentMatcher.group(3) + currentMatcher.group(4);

                    // loop through query parameters and create expressions
                    // calculating the number of arguments required for the expression
                    int argumentCursor = 0;
                    for (String queryParameter : queryParameters) {
                        MethodExpression currentExpression = MethodExpression.create(clazz, queryParameter);
                        final int requiredArgs = currentExpression.getArgumentsRequired();
                        totalRequiredArguments += requiredArgs;
                        // populate the arguments into the GrailsExpression from the argument list
                        Object[] currentArguments = new Object[requiredArgs];
                        if ((argumentCursor + requiredArgs) > arguments.length) {
                            throw new MissingMethodException(methodName, clazz, arguments);
                        }

                        for (int k = 0; k < requiredArgs; k++, argumentCursor++) {
                            currentArguments[k] = arguments[argumentCursor];
                        }
                        currentExpression.setArguments(currentArguments);
                        // add to list of expressions
                        expressions.add(currentExpression);
                    }
                    break;
                }
            }
        }
        // otherwise there is only one expression
        if (!containsOperator && querySequence != null) {
            MethodExpression solo = MethodExpression.create(clazz,querySequence );

            final int requiredArguments = solo.getArgumentsRequired();
            if (requiredArguments  > arguments.length) {
                throw new MissingMethodException(methodName,clazz,arguments);
            }

            totalRequiredArguments += requiredArguments;
            Object[] soloArgs = new Object[requiredArguments];

            System.arraycopy(arguments, 0, soloArgs, 0, requiredArguments);
            solo.setArguments(soloArgs);
            expressions.add(solo);
        }

        // if the total of all the arguments necessary does not equal the number of arguments
        // throw exception
        if (totalRequiredArguments > arguments.length) {
            throw new MissingMethodException(methodName,clazz,arguments);
        }

        // calculate the remaining arguments
        Object[] remainingArguments = new Object[arguments.length - totalRequiredArguments];
        if (remainingArguments.length > 0) {
            for (int i = 0, j = totalRequiredArguments; i < remainingArguments.length; i++,j++) {
                remainingArguments[i] = arguments[j];
            }
        }

        return doInvokeInternalWithExpressions(clazz, methodName, remainingArguments, expressions, additionalCriteria,operatorInUse);
    }

    public Object invoke(final Class clazz, String methodName, Object[] arguments) {
        return invoke(clazz, methodName, null, arguments);
    }

    protected abstract Object doInvokeInternalWithExpressions(Class clazz, String methodName, Object[] remainingArguments, List<MethodExpression> expressions, Closure additionalCriteria, String operatorInUse);

    public static void populateArgumentsForCriteria(Class<?> targetClass, Query q, Map argMap) {
        if(argMap != null) {
            Integer maxParam = null;
            Integer offsetParam = null;
            final ConversionService conversionService = q.getSession().getMappingContext().getConversionService();
            if (argMap.containsKey(ARGUMENT_MAX)) {
                maxParam = conversionService.convert(argMap.get(ARGUMENT_MAX),Integer.class);
            }
            if (argMap.containsKey(ARGUMENT_OFFSET)) {
                offsetParam = conversionService.convert(argMap.get(ARGUMENT_OFFSET),Integer.class);
            }
            String orderParam = (String)argMap.get(ARGUMENT_ORDER);

            final String sort = (String)argMap.get(ARGUMENT_SORT);
            final String order = ORDER_DESC.equalsIgnoreCase(orderParam) ? ORDER_DESC : ORDER_ASC;
            final int max = maxParam == null ? -1 : maxParam;
            final int offset = offsetParam == null ? -1 : offsetParam;
            if (max > -1) {
                q.max(max);
            }
            if (offset > -1) {
                q.offset(offset);
            }
            if (sort != null) {
                if (ORDER_DESC.equals(order)) {
                    q.order(Query.Order.desc(sort));
                }
                else {
                    q.order(Query.Order.asc(sort));
                }
            }
            
        }
    }


    protected void configureQueryWithArguments(Class clazz, Query query, Object[] arguments) {
        if (arguments.length > 0) {
            if (arguments[0] instanceof Map<?, ?>) {
               Map<?, ?> argMap = (Map<?, ?>) arguments[0];
               populateArgumentsForCriteria(clazz, query, argMap);
            }
        }
    }

    protected void applyAdditionalCriteria(Query query, Closure additionalCriteria) {
        if(additionalCriteria != null) {
            CriteriaBuilder builder = new CriteriaBuilder(query.getEntity().getJavaClass(), query.getSession().getDatastore(), query);
            builder.build(additionalCriteria);
        }
    }
}
