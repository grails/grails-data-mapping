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
package org.springframework.datastore.mapping.model.types;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A registrar that registers basic type converters
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class BasicTypeConverterRegistrar {
    
    public void register(ConverterRegistry registry) {
       registry.addConverter(new Converter<Date, String>() {
            public String convert(Date date) {
                return String.valueOf(date.getTime());
            }
        });

        registry.addConverter(new Converter<Integer, Long>() {
            public Long convert(Integer integer) {
                return integer.longValue();
            }
        });

        registry.addConverter(new Converter<Integer, Double>() {
            public Double convert(Integer integer) {
                return integer.doubleValue();
            }
        });


        registry.addConverter(new Converter<String, Date>() {

            public Date convert(String s) {
                try {
                    final Long time = Long.valueOf(s);
                    return new Date(time);
                } catch (NumberFormatException e) {
                    // ignore
                }
                return null;
            }
        });

        registry.addConverter(new Converter<String, Double>() {

            public Double convert(String s) {
                try {
                    return Double.valueOf(s);
                } catch (NumberFormatException e) {
                    return (double) 0;
                }
            }
        });

        registry.addConverter(new Converter<String, Integer>() {

            public Integer convert(String s) {
                try {
                    return Integer.valueOf(s);
                } catch (NumberFormatException e) {
                    // ignore
                }
                return 0;
            }
        });
        registry.addConverter(new Converter<String, Long>() {

            public Long convert(String s) {
                try {
                    return Long.valueOf(s);
                } catch (NumberFormatException e) {
                    // ignore
                }
                return 0L;
            }
        });

        registry.addConverter(new Converter<Object, String>() {
            public String convert(Object o) {
                return o.toString();
            }
        });

        registry.addConverter(new Converter<Calendar, String>() {
            public String convert(Calendar calendar) {
                return String.valueOf(calendar.getTime().getTime());
            }
        });

        registry.addConverter(new Converter<String, Calendar>() {

            public Calendar convert(String s) {
                try {
                    Date date = new Date(Long.valueOf(s));
                    Calendar c = new GregorianCalendar();
                    c.setTime(date);
                    return c;
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        });        
    }
}
