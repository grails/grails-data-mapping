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
package org.grails.datastore.mapping.dynamodb.model.types;

import org.grails.datastore.mapping.model.types.BasicTypeConverterRegistrar;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;

/**
 * A registrar that registers type converters used for DynamoDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public class DynamoDBTypeConverterRegistrar extends BasicTypeConverterRegistrar {

    public void register(ConverterRegistry registry) {
        //use most of the standard converters
        super.register(registry);

        registry.addConverter(new Converter<byte[],String>() {
            public String convert(byte[] bytes) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i< bytes.length;i++){
                    sb.append(bytes[i]);
                    if (i < bytes.length-1) {
                        sb.append(",");
                    }

                }
                return sb.toString();
            }
        });
        registry.addConverter(new Converter<String,byte[]>() {
            public byte[] convert(String s) {
                if (s == null || s.isEmpty()) {
                    return EMPTY_BYTES;
                }
                String[] tokens = s.split(",");
                byte[] bytes = new byte[tokens.length];
                for (int i = 0; i< tokens.length; i++){
                    bytes[i] = Byte.parseByte(tokens[i]);
                }
                return bytes;
            }
        });

    }
    private static final byte[] EMPTY_BYTES = new byte[0];
}
