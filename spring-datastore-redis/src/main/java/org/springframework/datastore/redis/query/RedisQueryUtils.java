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
package org.springframework.datastore.redis.query;

import org.springframework.beans.SimpleTypeConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Internal utility methods for dealing with queries
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class RedisQueryUtils {


    public static List<Long> transformRedisResults(SimpleTypeConverter typeConverter, String[] results) {
        List<Long> returnResults;
        if(results.length != 0) {
            List<Long> foreignKeys = new ArrayList<Long>(results.length);
            for (String result : results) {
                foreignKeys.add(getLong(typeConverter, result));
            }
            returnResults=  foreignKeys;
        }
        else {
            returnResults= Collections.emptyList();
        }
        return returnResults;
    }

    public static Long getLong(SimpleTypeConverter typeConverter, Object key) {
        return typeConverter.convertIfNecessary(key, Long.class);
    }
}
