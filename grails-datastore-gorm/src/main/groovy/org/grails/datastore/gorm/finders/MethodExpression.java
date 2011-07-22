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

import java.util.Arrays;
import java.util.Collection;

import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.Query.Criterion;
import org.grails.datastore.mapping.query.Restrictions;
import org.springframework.util.Assert;

/**
 *  Method expression used to evaluate a dynamic finder.
 */
public abstract class MethodExpression {

    protected String propertyName;
    protected Object[] arguments;
    protected int argumentsRequired = 1;
    protected Class<?> targetClass;

    public abstract Query.Criterion createCriterion();

    protected MethodExpression(Class<?> targetClass, String propertyName) {
        this.propertyName = propertyName;
        this.targetClass = targetClass;
    }

    public int getArgumentsRequired() {
        return argumentsRequired;
    }

    public void setArguments(Object[] arguments) {
        this.arguments = arguments;
    }

    public Object[] getArguments() {
        return Arrays.copyOf(arguments, arguments.length);
    }

    public static class GreaterThan extends MethodExpression {
        public GreaterThan(Class<?> targetClass, String propertyName) {
            super(targetClass, propertyName);
        }

        @Override
        public Query.Criterion createCriterion() {
            return Restrictions.gt(propertyName, arguments[0]);
        }
    }

    public static class GreaterThanEquals extends MethodExpression {
        public GreaterThanEquals(Class<?> targetClass, String propertyName) {
            super(targetClass, propertyName);
        }

        @Override
        public Query.Criterion createCriterion() {
            return Restrictions.gte(propertyName, arguments[0]);
        }
    }

    public static class LessThan extends MethodExpression {
        public LessThan(Class<?> targetClass, String propertyName) {
            super(targetClass, propertyName);
        }

        @Override
        public Query.Criterion createCriterion() {
            return Restrictions.lt(propertyName, arguments[0]);
        }
    }

    public static class LessThanEquals extends MethodExpression {
        public LessThanEquals(Class<?> targetClass, String propertyName) {
            super(targetClass, propertyName);
        }

        @Override
        public Query.Criterion createCriterion() {
            return Restrictions.lte(propertyName, arguments[0]);
        }
    }

    public static class Like extends MethodExpression {
        public Like(Class<?> targetClass, String propertyName) {
            super(targetClass, propertyName);
        }

        @Override
        public Query.Criterion createCriterion() {
            return Restrictions.like(propertyName, arguments[0].toString());
        }
    }

    public static class InList extends MethodExpression {
        public InList(Class<?> targetClass, String propertyName) {
            super(targetClass, propertyName);
        }

        @Override
        public Query.Criterion createCriterion() {
            return Restrictions.in(propertyName, (Collection) arguments[0]);
        }

        @Override
        public void setArguments(Object[] arguments) {
            Assert.isTrue(arguments.length > 0 && arguments[0] instanceof Collection,
                "Only a collection of elements is supported in an 'in' query");

            super.setArguments(arguments);
        }
    }

    public static class Between extends MethodExpression {
        public Between(Class<?> targetClass, String propertyName) {
            super(targetClass, propertyName);
        }

        @Override
        public Query.Criterion createCriterion() {
            return Restrictions.between(propertyName, arguments[0], arguments[1]);
        }

        @Override
        public int getArgumentsRequired() {
            return 2;
        }

        @Override
        public void setArguments(Object[] arguments) {
            Assert.isTrue(arguments.length > 1, "A 'between' query requires at least two arguments");
            Assert.isTrue(arguments[0] instanceof Number && arguments[1] instanceof Number,
                "A 'between' query requires that both arguments are numbers");

            super.setArguments(arguments);
        }
    }

    public static class IsNull extends MethodExpression {
        public IsNull(Class<?> targetClass, String propertyName) {
            super(targetClass, propertyName);
        }

        @Override
        public int getArgumentsRequired() {
            return 0;
        }

        @Override
        public Criterion createCriterion() {
            return Restrictions.isNull(propertyName);
        }
    }

    public static class IsNotNull extends MethodExpression {

        public IsNotNull(Class<?> targetClass, String propertyName) {
            super(targetClass, propertyName);
        }

        @Override
        public int getArgumentsRequired() {
            return 0;
        }

        @Override
        public Criterion createCriterion() {
            return Restrictions.isNotNull(propertyName);
        }
    }

    public static class IsEmpty extends MethodExpression {
        public IsEmpty(Class<?> targetClass, String propertyName) {
            super(targetClass, propertyName);
        }

        @Override
        public int getArgumentsRequired() {
            return 0;
        }

        @Override
        public Criterion createCriterion() {
            return Restrictions.isEmpty(propertyName);
        }
    }

    public static class IsNotEmpty extends MethodExpression {

        public IsNotEmpty(Class<?> targetClass, String propertyName) {
            super(targetClass, propertyName);
        }

        @Override
        public int getArgumentsRequired() {
            return 0;
        }

        @Override
        public Criterion createCriterion() {
            return Restrictions.isNotEmpty(propertyName);
        }
    }

    public static class Equal extends MethodExpression {
        public Equal(Class<?> targetClass, String propertyName) {
            super(targetClass, propertyName);
        }

        @Override
        public Query.Criterion createCriterion() {
            return Restrictions.eq(propertyName, arguments[0]);
        }
    }

    public static class NotEqual extends MethodExpression {
        public NotEqual(Class<?> targetClass, String propertyName) {
            super(targetClass, propertyName);
        }

        @Override
        public Query.Criterion createCriterion() {
            return Restrictions.ne(propertyName, arguments[0]);
        }
    }
}
