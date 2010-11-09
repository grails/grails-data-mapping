package org.springframework.datastore.mapping.config;

import javax.persistence.FetchType;

public class Property {

	private boolean index = false;
	private FetchType fetchStrategy = FetchType.LAZY;
	private String targetName;
	
	/**
	 * The target to map to, could be a database column, document attribute, or hash key
	 * 
	 * @return The target name
	 */
	public String getTargetName() {
		return targetName;
	}
	
	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}
	/**
	 * @return Whether this property is index
	 */
	public boolean isIndex() {
	    return index;
	}

	/**
	 * Whether this property is index
	 * @param index Sets whether to index the property or not
	 */
	public void setIndex(boolean index) {
	    this.index = index;
	}

	public FetchType getFetchStrategy() {
	    return this.fetchStrategy;
	}

	public void setFetchStrategy(FetchType fetchStrategy) {
	    this.fetchStrategy = fetchStrategy;
	}

}
