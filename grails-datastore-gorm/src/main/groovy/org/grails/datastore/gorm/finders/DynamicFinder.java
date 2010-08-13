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

import groovy.lang.MissingMethodException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of dynamic finders
 */
public abstract class DynamicFinder {
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

    public boolean isMethodMatch(String methodName) {
        return this.pattern.matcher(methodName.subSequence(0, methodName.length())).find();
    }

    public Object invoke(final Class clazz, String methodName, Object[] arguments) {
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
                        totalRequiredArguments += currentExpression.argumentsRequired;
                        // populate the arguments into the GrailsExpression from the argument list
                        Object[] currentArguments = new Object[currentExpression.argumentsRequired];
                        if ((argumentCursor + currentExpression.argumentsRequired) > arguments.length) {
                            throw new MissingMethodException(methodName, clazz, arguments);
                        }

                        for (int k = 0; k < currentExpression.argumentsRequired; k++, argumentCursor++) {
                            currentArguments[k] = arguments[argumentCursor];
                        }
                        try {
                            currentExpression.setArguments(currentArguments);
                        }
                        catch (IllegalArgumentException iae) {
                            throw new MissingMethodException(methodName, clazz, arguments);
                        }
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

            if (solo.argumentsRequired > arguments.length) {
                throw new MissingMethodException(methodName,clazz,arguments);
            }

            totalRequiredArguments += solo.argumentsRequired;
            Object[] soloArgs = new Object[solo.argumentsRequired];

            System.arraycopy(arguments, 0, soloArgs, 0, solo.argumentsRequired);
            try {
                solo.setArguments(soloArgs);
            }
            catch (IllegalArgumentException iae) {
                throw new MissingMethodException(methodName,clazz,arguments);
            }
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

        return doInvokeInternalWithExpressions(clazz, methodName, remainingArguments, expressions, operatorInUse);
    }

    protected abstract Object doInvokeInternalWithExpressions(Class clazz, String methodName, Object[] remainingArguments, List<MethodExpression> expressions, String operatorInUse);

}
