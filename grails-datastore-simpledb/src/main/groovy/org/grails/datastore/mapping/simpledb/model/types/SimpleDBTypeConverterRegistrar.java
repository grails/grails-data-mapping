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
package org.grails.datastore.mapping.simpledb.model.types;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.converter.GenericConverter;
import org.grails.datastore.mapping.model.types.BasicTypeConverterRegistrar;

import com.amazonaws.services.simpledb.util.SimpleDBUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;

/**
 * A registrar that registers type converters used for SimpleDB. For example,
 * numeric types are padded with zeros because AWS SimpleDB stores everything as
 * a string and without padding ordering of numerics would not work.
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public class SimpleDBTypeConverterRegistrar extends BasicTypeConverterRegistrar {
    public static final byte CONVERTER_NEGATIVE_BYTE_OFFSET = (byte)128; //abs value of Byte.MIN_VALUE
    public static final int PADDING_BYTE = 4; //how many digits there will be for byte string representation see the offset length +1

    public static final short CONVERTER_NEGATIVE_SHORT_OFFSET = (short)32768; //abs value of Short.MIN_VALUE
    public static final int PADDING_SHORT = 6; //how many digits there will be for byte string representation - see the offset length +1

    public static final int CONVERTER_NEGATIVE_INTEGER_OFFSET = new BigDecimal("2147483648").intValue(); //abs value of Integer.MIN_VALUE
    public static final int PADDING_INTEGER = 11; //how many digits there will be for byte string representation - see the offset length +1

    public static final long CONVERTER_NEGATIVE_LONG_OFFSET = new BigDecimal("9223372036854775808").longValue(); //abs value of Long.MIN_VALUE
    public static final int PADDING_LONG = 20; //how many digits there will be for byte string representation - see the offset length +1

    public static final Converter<Date, String> DATE_TO_STRING_CONVERTER = new Converter<Date, String>() {
        public String convert(Date source) {
            return SimpleDBUtils.encodeDate(source);
        }
    };

    public static final Converter<String, Date> STRING_TO_DATE_CONVERTER = new Converter<String, Date>() {
        public Date convert(String source) {
            try {
                return SimpleDBUtils.decodeDate(source);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    };

    public static final Converter<Byte, String> BYTE_TO_STRING_CONVERTER = new Converter<Byte, String>() {
        public String convert(Byte source) {
            // the goal is to move all negatives into the positives realms so that we can
            // compare strings lexicographically and it will be accurate for mix of positives and negatives
            // Byte is -128..127
            // Let's say we shift by adding 128 and pad with zeros into 4 digits.
            // This way we can cover all negatives and turn them into positives.
            // -128 --> 0000
            // -127 --> 0001
            // ...
            // -1   --> 0127 (this is the maximum a Byte can hold)
            //
            // so now we need to take care of how to convert remaining range of 0..127 values.
            // lets say that for those values which are initially positive we just do toString and prepend them with '1'
            // 0   --> 1000
            // 1   --> 1001
            // 126 --> 1126
            // 127 --> 1127
            //
            // with this logic initially positive values will be 'greater' than converted negatives and yet conversion back/forth is faster than dealing with BigDecimals
            // another benefit is that for 'initially' positive values would look pretty much the same in the converted format, which is handy when looking at raw DB values

            //decoding is simple - we look at first char to decide how to proceed because we know exactly how we got it
            if (source < 0) {
                byte shiftedValue = (byte)(source + CONVERTER_NEGATIVE_BYTE_OFFSET);
                return SimpleDBUtils.encodeZeroPadding(shiftedValue, PADDING_BYTE);
            }
            return "1" + SimpleDBUtils.encodeZeroPadding(source, PADDING_BYTE-1); //-1 because we explicitly put 1 in front
        }
    };

    public static final Converter<String, Byte> STRING_TO_BYTE_CONVERTER = new Converter<String, Byte>() {
        public Byte convert(String source) {
            //see conversion logic fully described in the BYTE_TO_STRING_CONVERTER
            if (source.length() < PADDING_BYTE) {
                //it might be just a short string like '10' - in this case just parse value
                return Byte.parseByte(source);
            }
            if (source.charAt(0) == '0') {
                //initial value was negative
                return (byte)(Byte.parseByte(source) - CONVERTER_NEGATIVE_BYTE_OFFSET);
            }
            if (source.charAt(0) == '1') {
                //initial value was positive, just ignore '1' in the front
                Integer intResult = SimpleDBUtils.decodeZeroPaddingInt(source.substring(1));
                return intResult == null ? null : intResult.byteValue();
            }
            if (source.charAt(0) == '-') {
                return Byte.parseByte(source);
            }
            throw new IllegalArgumentException("should not happen: "+source);
        }
    };

    public static final Converter<Short, String> SHORT_TO_STRING_CONVERTER = new Converter<Short, String>() {
        public String convert(Short source) {
            //see conversion logic fully described in the BYTE_TO_STRING_CONVERTER
            if (source < 0) {
                short shiftedValue = (short)(source + CONVERTER_NEGATIVE_SHORT_OFFSET);
                return SimpleDBUtils.encodeZeroPadding(shiftedValue, PADDING_SHORT);
            }
            return "1" + SimpleDBUtils.encodeZeroPadding(source, PADDING_SHORT-1); //-1 because we explicitly put 1 in front
        }
    };

    public static final Converter<String, Short> STRING_TO_SHORT_CONVERTER = new Converter<String, Short>() {
        public Short convert(String source) {
            //see conversion logic fully described in the BYTE_TO_STRING_CONVERTER
            if (source.length() < PADDING_SHORT) {
                //it might be just a short string like '10' - in this case just parse value
                return Short.parseShort(source);
            }
            if (source.charAt(0) == '0') {
                //initial value was negative
                return (short)(Short.parseShort(source) - CONVERTER_NEGATIVE_SHORT_OFFSET);
            }
            if (source.charAt(0) == '1') {
                //initial value was positive, just ignore '1' in the front
                Integer intResult = SimpleDBUtils.decodeZeroPaddingInt(source.substring(1));
                return intResult == null ? null : intResult.shortValue();
            }
            if (source.charAt(0) == '-') {
                return Short.parseShort(source);
            }
            throw new IllegalArgumentException("should not happen: "+source);
        }
    };

    public static final Converter<Integer, String> INTEGER_TO_STRING_CONVERTER = new Converter<Integer, String>() {
        public String convert(Integer source) {
            //see conversion logic fully described in the BYTE_TO_STRING_CONVERTER
            if (source < 0) {
                int shiftedValue = source + CONVERTER_NEGATIVE_INTEGER_OFFSET;
                return SimpleDBUtils.encodeZeroPadding(shiftedValue, PADDING_INTEGER);
            }
            return "1" + SimpleDBUtils.encodeZeroPadding(source, PADDING_INTEGER-1); //-1 because we explicitly put 1 in front
        }
    };

    public static final Converter<String, Integer> STRING_TO_INTEGER_CONVERTER = new Converter<String, Integer>() {
        public Integer convert(String source) {
            if (source.length() < PADDING_INTEGER) {
                //it might be just a short string like '10' - in this case just parse value
                return Integer.parseInt(source);
            }
            //see conversion logic fully described in the BYTE_TO_STRING_CONVERTER
            if (source.charAt(0) == '0') {
                //initial value was negative
                return Integer.parseInt(source) - CONVERTER_NEGATIVE_INTEGER_OFFSET;
            }
            if (source.charAt(0) == '1') {
                //initial value was positive, just ignore '1' in the front
                return SimpleDBUtils.decodeZeroPaddingInt(source.substring(1));
            }
            if (source.charAt(0) == '-') {
                return Integer.parseInt(source);
            }
            throw new IllegalArgumentException("should not happen: "+source);
        }
    };

    public static final Converter<Long, String> LONG_TO_STRING_CONVERTER = new Converter<Long, String>() {
        public String convert(Long source) {
            //see conversion logic fully described in the BYTE_TO_STRING_CONVERTER
            if (source < 0) {
                long shiftedValue = source + CONVERTER_NEGATIVE_LONG_OFFSET;
                return SimpleDBUtils.encodeZeroPadding(shiftedValue, PADDING_LONG);
            }
            return "1" + SimpleDBUtils.encodeZeroPadding(source, PADDING_LONG-1); //-1 because we explicitly put 1 in front
        }
    };

    public static final Converter<String, Long> STRING_TO_LONG_CONVERTER = new Converter<String, Long>() {
        public Long convert(String source) {
            //see conversion logic fully described in the BYTE_TO_STRING_CONVERTER
            if (source.length() < PADDING_LONG) {
                //it might be just a short string like '10' - in this case just parse value
                return Long.parseLong(source);
            }
            if (source.charAt(0) == '0') {
                //initial value was negative
                return Long.parseLong(source) - CONVERTER_NEGATIVE_LONG_OFFSET;
            }
            if (source.charAt(0) == '1') {
                //initial value was positive, just ignore '1' in the front
                return SimpleDBUtils.decodeZeroPaddingLong(source.substring(1));
            }
            if (source.charAt(0) == '-') {
                return Long.parseLong(source);
            }
            throw new IllegalArgumentException("should not happen: "+source);
        }
    };


    @Override
    public void register(ConverterRegistry registry) {
        //we use most of the standard's converters
        super.register(registry);

        overwrite(registry, BYTE_TO_STRING_CONVERTER);
        overwrite(registry, STRING_TO_BYTE_CONVERTER);

        overwrite(registry, SHORT_TO_STRING_CONVERTER);
        overwrite(registry, STRING_TO_SHORT_CONVERTER);

        overwrite(registry, INTEGER_TO_STRING_CONVERTER);
        overwrite(registry, STRING_TO_INTEGER_CONVERTER);

        overwrite(registry, LONG_TO_STRING_CONVERTER);
        overwrite(registry, STRING_TO_LONG_CONVERTER);

        overwrite(registry, DATE_TO_STRING_CONVERTER);
        overwrite(registry, STRING_TO_DATE_CONVERTER);
    }

    protected void overwrite(ConverterRegistry registry, @SuppressWarnings("rawtypes") Converter converter) {
        //get type info for the specified converter
        GenericConverter.ConvertiblePair typeInfo = getRequiredTypeInfo(converter, Converter.class);
        if (typeInfo == null) {
            throw new IllegalArgumentException(
                  "Unable to the determine sourceType <S> and targetType <T> which " +
                  "your Converter<S, T> converts between; declare these generic types. Converter class: " +
                  converter.getClass().getName());
        }

        //now remove converters that we will overwrite for SimpleDB
        registry.removeConvertible(typeInfo.getSourceType(), typeInfo.getTargetType());

        //now add
        registry.addConverter(converter);
    }

    private GenericConverter.ConvertiblePair getRequiredTypeInfo(Object converter, Class<?> genericIfc) {
        Class<?>[] args = GenericTypeResolver.resolveTypeArguments(converter.getClass(), genericIfc);
        return (args != null ? new GenericConverter.ConvertiblePair(args[0], args[1]) : null);
    }
}
