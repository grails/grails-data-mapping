/* Copyright (C) 2011 SpringSource
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
package org.grails.datastore.mapping.config.utils;

import java.util.Map;

import org.grails.datastore.mapping.model.types.conversion.DefaultConversionService;
import org.springframework.core.convert.ConversionService;

/**
 * Used to ease reading of configuration.
 */
public class ConfigUtils {

    private static ConversionService conversionService = new DefaultConversionService();

    public static <T> T read(Class<T> type, String key, Map<String, String> config, T defaultValue) {
        String value = config.get(key);
        return value == null ? defaultValue : conversionService.convert(value, type);
    }
}
