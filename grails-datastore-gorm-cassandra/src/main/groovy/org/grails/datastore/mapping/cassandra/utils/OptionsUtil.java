package org.grails.datastore.mapping.cassandra.utils;

import java.util.Map;

import org.springframework.cassandra.core.ConsistencyLevel;
import org.springframework.cassandra.core.RetryPolicy;
import org.springframework.cassandra.core.WriteOptions;

public class OptionsUtil {

	public static WriteOptions convertToWriteOptions(Map<String, Object> params) {
		if (params == null) {
			return null;
		}
		Map<String, Object> writeOptionsMap = (Map<String, Object>) params.get("writeOptions");
		if (writeOptionsMap == null) {
			return null;
		}
		WriteOptions writeOptions = new WriteOptions();		
		writeOptions.setConsistencyLevel(EnumUtil.findEnum(ConsistencyLevel.class, "consistencyLevel", writeOptionsMap, null));
		writeOptions.setRetryPolicy(EnumUtil.findEnum(RetryPolicy.class, "retryPolicy", writeOptionsMap, null));
		Object integer = writeOptionsMap.get("ttl");
		if (Integer.class.isInstance(integer)) {
			writeOptions.setTtl((Integer) integer);
		}
		return writeOptions;
	}
}
