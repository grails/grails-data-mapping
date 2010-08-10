package org.springframework.datastore.keyvalue.convert;

import org.springframework.beans.SimpleTypeConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;

import java.io.UnsupportedEncodingException;

/**
 * Type converter that can convert byte[] values
 */
public class ByteArrayAwareTypeConverter extends SimpleTypeConverter{

    public ByteArrayAwareTypeConverter() {
        super();
        GenericConversionService conversionService = new GenericConversionService();
        conversionService.addConverter(new Converter<byte[], Long>() {
            public Long convert(byte[] source) {
                try {
                    String value = new String(source, "UTF-8");
                    return Long.valueOf(value);
                } catch (UnsupportedEncodingException e) {
                    return 0L;
                }
                catch(NumberFormatException e) {
                    return 0L;
                }
            }
        });
        conversionService.addConverter(new Converter<byte[], String>() {
            public String convert(byte[] source) {
                try {
                    return new String(source, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    return null;
                }
            }
        });
        setConversionService(conversionService);
    }
}
