package org.grails.datastore.mapping.cassandra.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.EnumUtils;
import org.springframework.cassandra.core.keyspace.Option;

public class EnumUtil {

	
	/**
     * <p>Gets the enum for the specified class, returning {@code null} if not found.</p>
     *
     * <p>This method differs from {@link Enum#valueOf} in that it finds the
     * enum based on its toString method or its getName method if its an instance of @link {@link Option}.
     *</p>
     * @param <E> the type of the enumeration
     * @param enumClass  the class of the enum to query, not null
     * @param value the enum as a string as returned by its toString method, null returns null
     * @return the enum, null if not found
     */
	public static <E extends Enum<E>> E findEnum(Class<E> enumClass, String value) {
		if (value == null) {
			return null;
		}
		List<E> enumTypes = EnumUtils.getEnumList(enumClass);
		E enumValue = null;
		for (E e : enumTypes) {
			if (Option.class.isInstance(e)) {
				if (((Option) e).getName().equals(value)) {
					enumValue = e;
					break;
				}
			} else if (e.toString().equals(value)) {
				enumValue = e;
				break;
			}
		}
		return enumValue;
	}	
	
	/**
     * <p>Returns the value to which the specified key is mapped as an Enum of the specified enumClass,
     * returning the defaultValue if there is no value in the map or it's not a String.
     * </p>
     * <p>
     * This method finds the enum based on its toString method or its getName method if its an instance of @link {@link Option}.
     * </p>
     * @param <E> the type of the enumeration
     * @param enumClass  the class of the enum to query, not null
     * @param key the enum as a string as returned by its toString method 
     * @param map  the map to search in for the key
     * @return the enum, defaultValue if key has no value in the map
     * @throws IllegalArgumentException if the value in the map cannot be converted to the enum 
     */
	public static <E extends Enum<E>> E findEnum(Class<E> enumClass, String key, Map<String, Object> map, E defaultValue) {
		if (map == null) {
			return defaultValue;
		}
		Object value = map.get(key);
		if (value == null) {
			return defaultValue;
		}
		
		if (!(value instanceof String)) {
			throw new IllegalArgumentException(String.format("Invalid type for property [%s], expected java.lang.String", key));
		}
		
		E enumValue = getRequiredEnum(enumClass, key, (String) value);
		return enumValue;
	}

	/**
     * <p>Gets the enum for the specified class, throwing IllegalArgumentException if not found.</p>
     *
     * This method finds the enum based on its toString method or its getName method if its an instance of @link {@link Option}.
     * 
     * @param <E> the type of the enumeration
     * @param enumClass  the class of the enum to query, not null
     * @param property used in the exception message to inform caller which property is invalid
     * @param value the enum as a string as returned by its toString method
     * @return the enum
     * @throws IllegalArgumentException if the enum is not found from the specified value, message indicates invalid property and the allowable values for it
     */
	public static <E extends Enum<E>> E getRequiredEnum(Class<E> enumClass, String property, String value) {
		E enumValue = findEnum(enumClass, (String) value);		
		if (enumValue == null) {			
			throw new IllegalArgumentException(String.format("Invalid option [%s] for property [%s], allowable values are %s", value, property, getValidEnumList(enumClass)));
		}
		return enumValue;
	}
	
	/**
     * <p>Gets the {@code List} of enum's string value based on its toString method or its getName method if its an instance of @link {@link Option}.</p>
     *
     * @param <E> the type of the enumeration
     * @param enumClass  the class of the enum to query, not null
     * @return the modifiable list of the enums' string values, never null
     */
	public static <E extends Enum<E>> List<String> getValidEnumList(Class<E> enumClass) {
		List<String> allowable = new ArrayList<String>();
		List<E> enumList = EnumUtils.getEnumList(enumClass);
		for (E e: enumList) {
			if (Option.class.isInstance(e)) {
				allowable.add(((Option)e).getName());
			} else {
				allowable.add(e.toString());
			}
		}
		return allowable;
	}

	/**
     * <p>Returns the enum mapped by the specified key in the specified map,
     * returning the defaultValue if the key or map is null.
     * </p>     
     * @param <E> the type of the enumeration
     * @param property  used in the exception message to inform caller which property is invalid
     * @param key the key to look for in the specified map 
     * @param map  the map to search in for the key
     * @return the enum, defaultValue if the key or map is null
     * @throws IllegalArgumentException if the key has no corresponding enum in the map
     */
	public static <E extends Enum<E>> E findMatchingEnum(String property, Object key, Map<String, E> map, E defaultValue) {
		if (key == null || map == null) {
			return defaultValue;
		}
		if (!(key instanceof String)) {
			throw new IllegalArgumentException(String.format("Invalid type for property [%s], expected java.lang.String", key));
		}
		E enumValue = map.get(key);		
		if (enumValue == null) {			
			throw new IllegalArgumentException(String.format("Invalid option [%s] for property [%s], allowable values are %s", key, property, map.keySet()));
		}
		return enumValue;
	}
}
