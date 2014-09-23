package org.grails.datastore.mapping.cassandra.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.EnumUtils;
import org.grails.datastore.mapping.model.IllegalMappingException;
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
     * @throws IllegalMappingException if the value in the map cannot be converted to the enum 
     */
	public static <E extends Enum<E>> E findEnum(Class<E> enumClass, String key, Map<String, Object> map, E defaultValue) {
		if (map == null) {
			return defaultValue;
		}
		Object value = map.get(key);
		if (value == null || !(value instanceof String)) {
			return defaultValue;
		}
		E enumValue = findEnum(enumClass, (String) value);
		
		if (enumValue == null) {			
			throw new IllegalMappingException(String.format("Invalid option [%s] for the property [%s], allowable values are %s", value, key, getValidEnumList(enumClass)));
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

}
