package org.springframework.datastore.mapping.simpledb.model.types;

import com.amazonaws.services.simpledb.util.SimpleDBUtils;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.datastore.mapping.model.types.BasicTypeConverterRegistrar;
import org.springframework.datastore.mapping.simpledb.util.SimpleDBConst;

import java.util.Date;

/**
 * A registrar that registers type converters used for SimpleDB. For example, numeric types are padded with zeros
 * because AWS SimpleDB stores everything as a string and without padding ordering of numerics would not work. 
 *
 * @author Roman Stepanenko
 * @since 0.1
 */
public class SimpleDBTypeConverterRegistrar extends BasicTypeConverterRegistrar {
    @Override
    public void register(ConverterRegistry registry) {
        //we use most of the standard's converters
        super.register(registry);

        overwrite(registry, INTEGER_TO_STRING_CONVERTER);
        overwrite(registry, LONG_TO_STRING_CONVERTER);
    }

    protected void overwrite(ConverterRegistry registry, Converter converter){
        //get type info for the specified converter
        GenericConverter.ConvertiblePair typeInfo = getRequiredTypeInfo(converter, Converter.class);
        if (typeInfo == null) {
            throw new IllegalArgumentException("Unable to the determine sourceType <S> and targetType <T> which " +
                            "your Converter<S, T> converts between; declare these generic types. Converter class: "+converter.getClass().getName());
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

    public static final Converter<Integer, String> INTEGER_TO_STRING_CONVERTER = new Converter<Integer, String>() {
        public String convert(Integer source) {
            String result = SimpleDBUtils.encodeZeroPadding(source, SimpleDBConst.PADDING_INT_DEFAULT);
            return result;
        }
    };

    public static final Converter<Long, String> LONG_TO_STRING_CONVERTER = new Converter<Long, String>() {
        public String convert(Long source) {
            String result = SimpleDBUtils.encodeZeroPadding(source, SimpleDBConst.PADDING_LONG_DEFAULT);
            return result;
        }
    };
}
