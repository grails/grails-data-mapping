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

package grails.gorm;

import groovy.lang.*;
import org.springframework.beans.TypeConverter;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.ClassMetadata;
import org.springframework.datastore.core.Datastore;
import org.springframework.datastore.keyvalue.convert.ByteArrayAwareTypeConverter;
import org.springframework.datastore.mapping.PersistentEntity;
import org.springframework.datastore.query.Projections;
import org.springframework.datastore.query.Query;
import org.springframework.datastore.query.Restrictions;
import static org.grails.datastore.gorm.finders.DynamicFinder.*;

import java.util.*;

/**
 *
 * Criteria builder implementation that operates against Spring datastore abstraction
 *
 * @author Graeme Rocher
 */
public class CriteriaBuilder extends GroovyObjectSupport {

    public static final String AND = "and"; // builder
    public static final String NOT = "not";// builder
    public static final String OR = "or"; // builder
    public static final String IS_NULL = "isNull"; // builder
    public static final String IS_NOT_NULL = "isNotNull"; // builder
    public static final String ID_EQUALS = "idEq"; // builder
    public static final String IS_EMPTY = "isEmpty"; //builder
    public static final String IS_NOT_EMPTY = "isNotEmpty"; //builder


    private static final String ROOT_DO_CALL = "doCall";
    private static final String ROOT_CALL = "call";
    private static final String LIST_CALL = "list";
    private static final String LIST_DISTINCT_CALL = "listDistinct";
    private static final String COUNT_CALL = "count";
    private static final String GET_CALL = "get";
    private static final String SCROLL_CALL = "scroll";
    private static final String PROJECTIONS = "projections";

    private Class targetClass;
    private Datastore datastore;
    private Query query;
    private boolean uniqueResult = false;
    private boolean count = false;
    private boolean paginationEnabledList;
    private List<Order> orderEntries = new ArrayList<Order>();
    private List<Query.Junction> logicalExpressionStack = new ArrayList<Query.Junction>();
    private TypeConverter typeConverter = new ByteArrayAwareTypeConverter();
    private MetaObjectProtocol queryMetaClass;
    private Query.ProjectionList projectionList;
    private PersistentEntity persistentEntity;

    public CriteriaBuilder(Class targetClass, Datastore datastore) {
        if(targetClass == null) throw new IllegalArgumentException("Argument [targetClass] cannot be null");
        if(datastore == null) throw new IllegalArgumentException("Argument [datastore] cannot be null");

        persistentEntity = datastore.getMappingContext().getPersistentEntity(targetClass.getName());
        if(persistentEntity == null) throw new IllegalArgumentException("Class ["+targetClass+"] is not a persistent entity");
        this.targetClass = targetClass;
        this.datastore = datastore;

    }

    private boolean isCriteriaConstructionMethod(String name, Object[] args) {
        return (name.equals(LIST_CALL) && args.length == 2 && args[0] instanceof Map && args[1] instanceof Closure) ||
                  (name.equals(ROOT_CALL) ||
                    name.equals(ROOT_DO_CALL) ||
                    name.equals(LIST_CALL) ||
                    name.equals(LIST_DISTINCT_CALL) ||
                    name.equals(GET_CALL) ||
                    name.equals(COUNT_CALL) ||
                    name.equals(SCROLL_CALL) && args.length == 1 && args[0] instanceof Closure);
    }

    private void invokeClosureNode(Object args) {
        Closure callable = (Closure)args;
        callable.setDelegate(this);
        callable.setResolveStrategy(Closure.DELEGATE_FIRST);
        callable.call();
    }

    @Override
    public Object invokeMethod(String name, Object obj) {
        Object[] args = obj.getClass().isArray() ? (Object[])obj : new Object[]{obj};

        if (isCriteriaConstructionMethod(name, args)) {
            if (query != null) {
                throw new IllegalArgumentException("call to [" + name + "] not supported here");
            }

            query = datastore.getCurrentSession().createQuery(targetClass);
            queryMetaClass = GroovySystem.getMetaClassRegistry().getMetaClass(query.getClass());            

            if (name.equals(GET_CALL)) {
                uniqueResult = true;
            }
            else if (name.equals(COUNT_CALL)) {
                count = true;
            }


            // Check for pagination params
            if (name.equals(LIST_CALL) && args.length == 2) {
                paginationEnabledList = true;
                orderEntries = new ArrayList<Order>();
                invokeClosureNode(args[1]);
            }
            else {
                invokeClosureNode(args[0]);
            }

            Object result;
            if (!uniqueResult) {
                if (count) {
                    query.projections().count();
                    result = query.singleResult();
                }
                else if (paginationEnabledList) {
                    populateArgumentsForCriteria(targetClass, query, (Map)args[0]);
                    result = query.list();
                }
                else {
                    result = query.list();
                }
            }
            else {
                result = query.singleResult();
            }
            query = null;
            return result;
        }


        MetaMethod metaMethod = getMetaClass().getMetaMethod(name, args);
        if (metaMethod != null) {
            return metaMethod.invoke(this, args);
        }

        metaMethod = queryMetaClass.getMetaMethod(name, args);
        if (metaMethod != null) {
            return metaMethod.invoke(query, args);
        }

        if (args.length == 1 && args[0] instanceof Closure) {
            if (name.equals(AND) || name.equals(OR) || name.equals(NOT)) {

                if(name.equals(AND)) {
                    logicalExpressionStack.add(new Query.Conjunction());
                }
                else {
                    final boolean isNot = name.equals(NOT);
                    logicalExpressionStack.add(new Query.Disjunction() {
                        @Override
                        public Query.Junction add(Query.Criterion c) {

                            // TODO: Add support for negation
//                            if(isNot)
//                                return super.add(Restrictions.not(c));
//                            else
                                return super.add(c);
                        }
                    });
                }
                invokeClosureNode(args[0]);

                Query.Junction logicalExpression = logicalExpressionStack.remove(logicalExpressionStack.size()-1);
                addToCriteria(logicalExpression);

                return name;
            }

            if (name.equals(PROJECTIONS) && args.length == 1 && (args[0] instanceof Closure)) {

                projectionList = query.projections();
                invokeClosureNode(args[0]);
                return name;
            }
        }
        else if (args.length == 1 && args[0] != null) {
            Object value = args[0];
            Query.Criterion c = null;
            if (name.equals(ID_EQUALS)) {
                return eq("id", value);
            }

            if (name.equals(IS_NULL) ||
                    name.equals(IS_NOT_NULL) ||
                    name.equals(IS_EMPTY) ||
                    name.equals(IS_NOT_EMPTY)) {
                if (!(value instanceof String)) {
                    new IllegalArgumentException("call to [" + name + "] with value [" +
                            value + "] requires a String value.");
                }
                String propertyName = value.toString();
                if (name.equals(IS_NULL)) {
                    // TODO Implement isNull restriction
//                    c = Restrictions.isNull(propertyName) ;
                }
                else if (name.equals(IS_NOT_NULL)) {
                    // TODO Implement isNotNull restriction
//                    c = Restrictions.isNotNull(propertyName);
                }
                else if (name.equals(IS_EMPTY)) {
                    // TODO Implement isEmpty restriction
//                    c = Restrictions.isEmpty(propertyName);
                }
                else if (name.equals(IS_NOT_EMPTY)) {
                    // TODO Implement isNotEmpty restriction
//                    c = Restrictions.isNotEmpty(propertyName);
                }
            }

            if (c != null) {
                return addToCriteria(c);
            }
        }

        throw new MissingMethodException(name, getClass(), args) ;

    }


    protected void populateArgumentsForCriteria(Class<?> targetClass, Query q, Map argMap) {
        Integer maxParam = null;
        Integer offsetParam = null;
        if (argMap.containsKey(ARGUMENT_MAX)) {
            maxParam = typeConverter.convertIfNecessary(argMap.get(ARGUMENT_MAX),Integer.class);
        }
        if (argMap.containsKey(ARGUMENT_OFFSET)) {
            offsetParam = typeConverter.convertIfNecessary(argMap.get(ARGUMENT_OFFSET),Integer.class);
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
                q.order(Query.Order.DESC);
            }
            else {
                q.order(Query.Order.DESC);
            }
        }
    }

    /**
     * Creates an "equals" Criterion based on the specified property name and value.
     * 
     * @param propertyName The property name
     * @param propertyValue The property value
     *
     * @return A Criterion instance
     */
    @SuppressWarnings("rawtypes")
    public Query.Criterion eq(String propertyName, Object propertyValue) {
        validatePropertyName(propertyName, "eq");
        return addToCriteria(Restrictions.eq(propertyName, propertyValue));
    }


    /**
     * Creates an "equals" Criterion based on the specified property name and value.
     *
     * @param propertyName The property name
     * @param propertyValue The property value
     *
     * @return A Criterion instance
     */
    @SuppressWarnings("rawtypes")
    public Query.Criterion like(String propertyName, Object propertyValue) {
        validatePropertyName(propertyName, "like");
        if(propertyValue == null) throw new IllegalArgumentException("Cannot use like expression with null value");
        return addToCriteria(Restrictions.like(propertyName, propertyValue.toString()));
    }

    /**
     * Creates an "in" Criterion based on the specified property name and list of values.
     *
     * @param propertyName The property name
     * @param values The values
     *
     * @return A Criterion instance
     */
    @SuppressWarnings("rawtypes")
    public Query.Criterion in(String propertyName, Collection values) {
        validatePropertyName(propertyName, "in");
        if(values == null) throw new IllegalArgumentException("Cannot use in expression with null values");
        return addToCriteria(Restrictions.in(propertyName, values));
    }

    /**
     * Creates an "in" Criterion based on the specified property name and list of values.
     *
     * @param propertyName The property name
     * @param values The values
     *
     * @return A Criterion instance
     */
    @SuppressWarnings("rawtypes")
    public Query.Criterion inList(String propertyName, Collection values) {
        return in(propertyName, values);
    }

    /**
     * Creates an "in" Criterion based on the specified property name and list of values.
     *
     * @param propertyName The property name
     * @param values The values
     *
     * @return A Criterion instance
     */
    @SuppressWarnings("rawtypes")
    public Query.Criterion inList(String propertyName, Object[] values) {
        return in(propertyName, Arrays.asList(values));
    }

   /**
     * Creates an "in" Criterion based on the specified property name and list of values.
     *
     * @param propertyName The property name
     * @param values The values
     *
     * @return A Criterion instance
     */
    @SuppressWarnings("rawtypes")
    public Query.Criterion in(String propertyName, Object[] values) {
        return in(propertyName, Arrays.asList(values));
    }




    private void validatePropertyName(String propertyName, String methodName) {
        if(propertyName == null) {
            throw new IllegalArgumentException("Cannot use ["+methodName+"] restriction with null property name");
        }

        if(persistentEntity.getPropertyByName(propertyName) == null) {
            throw new IllegalArgumentException("Property ["+propertyName+"] is not a valid property of class ["+persistentEntity+"]");
        }
    }


    /*
    * adds and returns the given criterion to the currently active criteria set.
    * this might be either the root criteria or a currently open
    * LogicalExpression.
    */
    private Query.Criterion addToCriteria(Query.Criterion c) {
        if (!logicalExpressionStack.isEmpty()) {
            logicalExpressionStack.get(logicalExpressionStack.size() - 1).add(c);
        }
        else {
            query.add(c);
        }
        return c;
    }


}
