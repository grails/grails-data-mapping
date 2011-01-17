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

import org.springframework.datastore.mapping.query.Query;
import org.springframework.datastore.mapping.query.Query.Criterion;
import org.springframework.datastore.mapping.query.Query.IsEmpty;
import org.springframework.datastore.mapping.query.Query.IsNull;
import org.springframework.datastore.mapping.query.Restrictions;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Locale;

/**
 *  Method expression used to evaluate a dynamic finder
 */
public abstract class MethodExpression {

   private static final String NOT = "Not";

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

    public static MethodExpression create(Class clazz, String expression) {
        if(expression.endsWith(Equal.class.getSimpleName())) {
            return new NotEqual(clazz, calcPropertyName(expression, NotEqual.class.getSimpleName()));
        }
        else if(expression.endsWith(Equal.class.getSimpleName())) {
            return new Equal(clazz, calcPropertyName(expression, Equal.class.getSimpleName()));
        }
        else if(expression.endsWith(InList.class.getSimpleName())) {
            return new InList(clazz, calcPropertyName(expression, InList.class.getSimpleName()));
        }
        else if(expression.endsWith(Between.class.getSimpleName())) {
            return new Between(clazz, calcPropertyName(expression, Between.class.getSimpleName()));
        }
        else if(expression.endsWith(Like.class.getSimpleName())) {
            return new Like(clazz, calcPropertyName(expression, Like.class.getSimpleName()));
        }
        else if(expression.endsWith(GreaterThan.class.getSimpleName())) {
            return new GreaterThan(clazz, calcPropertyName(expression, GreaterThan.class.getSimpleName()));
        }
        else if(expression.endsWith(LessThan.class.getSimpleName())) {
            return new LessThan(clazz, calcPropertyName(expression, LessThan.class.getSimpleName()));
        }
        else if(expression.endsWith(GreaterThanEquals.class.getSimpleName())) {
            return new GreaterThanEquals(clazz, calcPropertyName(expression, GreaterThanEquals.class.getSimpleName()));
        }
        else if(expression.endsWith(LessThanEquals.class.getSimpleName())) {
            return new LessThanEquals(clazz, calcPropertyName(expression, LessThanEquals.class.getSimpleName()));
        }
        else if(expression.endsWith(IsNull.class.getSimpleName())) {
            return new IsNull(clazz, calcPropertyName(expression, IsNull.class.getSimpleName()));
        }        
        else if(expression.endsWith(IsNotNull.class.getSimpleName())) {
            return new IsNotNull(clazz, calcPropertyName(expression, IsNotNull.class.getSimpleName()));
        } 
        else if(expression.endsWith(IsEmpty.class.getSimpleName())) {
            return new IsEmpty(clazz, calcPropertyName(expression, IsEmpty.class.getSimpleName()));
        }        
        else if(expression.endsWith(IsNotEmpty.class.getSimpleName())) {
            return new IsNotEmpty(clazz, calcPropertyName(expression, IsNotEmpty.class.getSimpleName()));
        }            


        return new Equal(clazz, calcPropertyName(expression, Equal.class.getSimpleName()));  
    }

    private static String calcPropertyName(String queryParameter, String clause) {
        String propName;
        if (clause != null && !clause.equals(Equal.class.getSimpleName())) {
            int i = queryParameter.indexOf(clause);
            propName = queryParameter.substring(0,i);
        }
        else {
            propName = queryParameter;
        }
        if (propName.endsWith(NOT)) {
            int i = propName.lastIndexOf(NOT);
            propName = propName.substring(0, i);
        }
        if(StringUtils.hasLength(propName))
            return propName.substring(0,1).toLowerCase(Locale.ENGLISH) + propName.substring(1);
        else
            throw new IllegalArgumentException("No property name specified in clause: " + clause);
    }

    public static class GreaterThan extends MethodExpression {
        protected GreaterThan(Class<?> targetClass, String propertyName) {
            super(targetClass, propertyName);
        }

        @Override
        public Query.Criterion createCriterion() {
            return Restrictions.gt(propertyName, arguments[0]);
        }
    }


    public static class GreaterThanEquals extends MethodExpression {
        protected GreaterThanEquals(Class<?> targetClass, String propertyName) {
            super(targetClass, propertyName);
        }

        @Override
        public Query.Criterion createCriterion() {
            return Restrictions.gte(propertyName, arguments[0]);
        }
    }

    public static class LessThan extends MethodExpression {
        protected LessThan(Class<?> targetClass, String propertyName) {
            super(targetClass, propertyName);
        }

        @Override
        public Query.Criterion createCriterion() {
            return Restrictions.lt(propertyName, arguments[0]);
        }
    }

    public static class LessThanEquals extends MethodExpression {
        protected LessThanEquals(Class<?> targetClass, String propertyName) {
            super(targetClass, propertyName);
        }

        @Override
        public Query.Criterion createCriterion() {
            return Restrictions.lte(propertyName, arguments[0]);
        }
    }


    public static class Like extends MethodExpression {
        protected Like(Class<?> targetClass, String propertyName) {
            super(targetClass, propertyName);
        }

        @Override
        public Query.Criterion createCriterion() {
            return Restrictions.like(propertyName, arguments[0].toString());
        }
    }

    public static class InList extends MethodExpression {
        protected InList(Class<?> targetClass, String propertyName) {
            super(targetClass, propertyName);
        }

        @Override
        public Query.Criterion createCriterion() {
            return Restrictions.in(propertyName, (Collection) arguments[0]);
        }

        @Override
        public void setArguments(Object[] arguments) {
            if(arguments.length == 0 || !(arguments[0] instanceof Collection))
                throw new IllegalArgumentException("Only a collection of elements is supported in an 'in' query");

            super.setArguments(arguments);
        }
    }

    public static class Between extends MethodExpression {
        protected Between(Class<?> targetClass, String propertyName) {
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
            if(arguments.length < 2 )
                throw new IllegalArgumentException("A 'between' query requires at least two arguments");
            if(!(arguments[0] instanceof Number) || !(arguments[0] instanceof Number))
                throw new IllegalArgumentException("A 'between' query requires that both arguments are numbers");

            super.setArguments(arguments);
        }
    }
    
    public static class IsNull extends MethodExpression {
		protected IsNull(Class<?> targetClass, String propertyName) {
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

		protected IsNotNull(Class<?> targetClass, String propertyName) {
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
		protected IsEmpty(Class<?> targetClass, String propertyName) {
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

		protected IsNotEmpty(Class<?> targetClass, String propertyName) {
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
        protected Equal(Class<?> targetClass, String propertyName) {
            super(targetClass, propertyName);
        }

        @Override
        public Query.Criterion createCriterion() {
            return Restrictions.eq(propertyName, arguments[0]);
        }
    }
    public static class NotEqual extends MethodExpression {
        protected NotEqual(Class<?> targetClass, String propertyName) {
            super(targetClass, propertyName);
        }

        @Override
        public Query.Criterion createCriterion() {
            return Restrictions.ne(propertyName, arguments[0]);
        }
    }
}
