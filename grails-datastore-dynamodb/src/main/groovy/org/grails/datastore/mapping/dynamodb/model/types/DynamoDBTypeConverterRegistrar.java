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
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.converter.GenericConverter;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;

/**
 * A registrar that registers type converters used for DynamoDB.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public class DynamoDBTypeConverterRegistrar extends BasicTypeConverterRegistrar {
}
