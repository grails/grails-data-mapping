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
package org.grails.datastore.mapping.query.api;


import java.util.Collection;
import java.util.List;

/**
 * Interface for the implementations that construct of criteria queries
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public interface Criteria  {
    /**
     * Creates a criterion that restricts the id to the given value
     * @param value The value
     * @return The criteria
     */
    Criteria idEquals(Object value);

    /**
     * Creates a criterion that asserts the given property is empty (such as a blank string)
     *
     * @param propertyName The property name
     * @return The criteria
     */
    Criteria isEmpty(String propertyName);

    /**
     * Creates a criterion that asserts the given property is not empty
     *
     * @param propertyName The property name
     * @return The criteria
     */
    Criteria isNotEmpty(String propertyName);
    /**
     * Creates a criterion that asserts the given property is null
     * 
     * @param propertyName The property name
     * @return The criteria
     */
    Criteria isNull(String propertyName);

    /**
     * Creates a criterion that asserts the given property is not null
     *
     * @param propertyName The property name
     * @return The criteria
     */
    Criteria isNotNull(String propertyName);

    /**
     * Creates an "equals" Criterion based on the specified property name and value.
     *
     * @param propertyName The property name
     * @param propertyValue The property value
     *
     * @return The criteria
     */
    Criteria eq(String propertyName, Object propertyValue);

    /**
     * Creates an "equals" Criterion based on the specified property name and value.
     *
     * @param propertyValue The property value
     *
     * @return The criteria
     */
    Criteria idEq(Object propertyValue);

    /**
     * Creates a "not equals" Criterion based on the specified property name and value.
     *
     * @param propertyName The property name
     * @param propertyValue The property value
     *
     * @return The criteria
     */
    Criteria ne(String propertyName, Object propertyValue);

    /**
     * Restricts the results by the given property value range (inclusive)
     *
     * @param propertyName The property name
     *
     * @param start The start of the range
     * @param finish The end of the range
     * @return The criteria
     */
    Criteria between(String propertyName, Object start, Object finish);

    /**
     * Used to restrict a value to be greater than or equal to the given value
     * @param property The property
     * @param value The value
     * @return The Criterion instance
     */
    Criteria gte(String property, Object value);

    /**
     * Used to restrict a value to be greater than or equal to the given value
     * @param property The property
     * @param value The value
     * @return The Criterion instance
     */
    Criteria ge(String property, Object value);

    /**
     * Used to restrict a value to be greater than or equal to the given value
     * @param property The property
     * @param value The value
     * @return The Criterion instance
     */
    Criteria gt(String property, Object value);

    /**
     * Used to restrict a value to be less than or equal to the given value
     * @param property The property
     * @param value The value
     * @return The Criterion instance
     */
    Criteria lte(String property, Object value);

    /**
     * Used to restrict a value to be less than or equal to the given value
     * @param property The property
     * @param value The value
     * @return The Criterion instance
     */
    Criteria le(String property, Object value);

    /**
     * Used to restrict a value to be less than or equal to the given value
     * @param property The property
     * @param value The value
     * @return The Criterion instance
     */
    Criteria lt(String property, Object value);

    /**
     * Creates an like Criterion based on the specified property name and value.
     *
     * @param propertyName The property name
     * @param propertyValue The property value
     *
     * @return The criteria
     */
    Criteria like(String propertyName, Object propertyValue);

    /**
     * Creates an ilike Criterion based on the specified property name and value. Unlike a like condition, ilike is case insensitive
     *
     * @param propertyName The property name
     * @param propertyValue The property value
     *
     * @return The criteria
     */
    Criteria ilike(String propertyName, Object propertyValue);

    /**
     * Creates an rlike Criterion based on the specified property name and value.
     *
     * @param propertyName The property name
     * @param propertyValue The property value
     *
     * @return The criteria
     */
    Criteria rlike(String propertyName, Object propertyValue);

    /**
     * Creates an "in" Criterion based on the specified property name and list of values.
     *
     * @param propertyName The property name
     * @param values The values
     *
     * @return The criteria
     */
    Criteria in(String propertyName, Collection values);

    /**
     * Creates an "in" Criterion based on the specified property name and list of values.
     *
     * @param propertyName The property name
     * @param values The values
     *
     * @return The criteria
     */
    Criteria inList(String propertyName, Collection values);

    /**
     * Creates an "in" Criterion based on the specified property name and list of values.
     *
     * @param propertyName The property name
     * @param values The values
     *
     * @return The criteria
     */
    Criteria inList(String propertyName, Object[] values);

    /**
      * Creates an "in" Criterion based on the specified property name and list of values.
      *
      * @param propertyName The property name
      * @param values The values
      *
      * @return The criteria
      */
    Criteria in(String propertyName, Object[] values);

    /**
     * Orders by the specified property name (defaults to ascending)
     *
     * @param propertyName The property name to order by
     * @return This criteria
     */
    Criteria order(String propertyName);

    /**
     * Orders by the specified property name and direction
     *
     * @param propertyName The property name to order by
     * @param direction Either "asc" for ascending or "desc" for descending
     *
     * @return This criteria
     */
    Criteria order(String propertyName, String direction);

    /**
     * Creates a Criterion that contrains a collection property by size
     *
     * @param propertyName The property name
     * @param size The size to constrain by
     *
     * @return This criteria
     */
    Criteria sizeEq(String propertyName, int size) ;

    /**
     * Creates a Criterion that contrains a collection property to be greater than the given size
     *
     * @param propertyName The property name
     * @param size The size to constrain by
     *
     * @return This criteria
     */
    Criteria sizeGt(String propertyName, int size);

    /**
     * Creates a Criterion that contrains a collection property to be greater than or equal to the given size
     *
     * @param propertyName The property name
     * @param size The size to constrain by
     *
     * @return This criteria
     */
    Criteria sizeGe(String propertyName, int size);

    /**
     * Creates a Criterion that contrains a collection property to be less than or equal to the given size
     *
     * @param propertyName The property name
     * @param size The size to constrain by
     *
     * @return This criteria
     */
    Criteria sizeLe(String propertyName, int size);

    /**
     * Creates a Criterion that contrains a collection property to be less than to the given size
     *
     * @param propertyName The property name
     * @param size The size to constrain by
     *
     * @return This criteria
     */
    Criteria sizeLt(String propertyName, int size);

    /**
     * Creates a Criterion that contrains a collection property to be not equal to the given size
     *
     * @param propertyName The property name
     * @param size The size to constrain by
     *
     * @return This criteria
     */
    Criteria sizeNe(String propertyName, int size);

    /**
     * Constraints a property to be equal to a specified other property
     *
     * @param propertyName The property
     * @param otherPropertyName The other property
     * @return This criteria
     */
    Criteria eqProperty(java.lang.String propertyName, java.lang.String otherPropertyName);

    /**
     * Constraints a property to be not equal to a specified other property
     *
     * @param propertyName The property
     * @param otherPropertyName The other property
     * @return This criteria
     */
    Criteria neProperty(java.lang.String propertyName, java.lang.String otherPropertyName);

    /**
     * Constraints a property to be greater than a specified other property
     *
     * @param propertyName The property
     * @param otherPropertyName The other property
     * @return This criteria
     */
    Criteria gtProperty(java.lang.String propertyName, java.lang.String otherPropertyName);

    /**
     * Constraints a property to be greater than or equal to a specified other property
     *
     * @param propertyName The property
     * @param otherPropertyName The other property
     * @return This criteria
     */
    Criteria geProperty(java.lang.String propertyName, java.lang.String otherPropertyName);

    /**
     * Constraints a property to be less than a specified other property
     *
     * @param propertyName The property
     * @param otherPropertyName The other property
     * @return This criteria
     */
    Criteria ltProperty(java.lang.String propertyName, java.lang.String otherPropertyName);

    /**
     * Constraints a property to be less than or equal to a specified other property
     *
     * @param propertyName The property
     * @param otherPropertyName The other property
     * @return This criteria
     */
    Criteria leProperty(java.lang.String propertyName, java.lang.String otherPropertyName);

}
