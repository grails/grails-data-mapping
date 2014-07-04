package org.grails.datastore.gorm.cassandra.mapping;

import org.grails.datastore.mapping.model.types.BasicTypeConverterRegistrar;
import org.grails.datastore.mapping.model.types.conversion.StringToCurrencyConverter;
import org.grails.datastore.mapping.model.types.conversion.StringToLocaleConverter;
import org.grails.datastore.mapping.model.types.conversion.StringToTimeZoneConverter;
import org.grails.datastore.mapping.model.types.conversion.StringToURLConverter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.cassandra.convert.CassandraPersistentEntityParameterValueProvider;
import org.springframework.data.cassandra.convert.ColumnReader;
import org.springframework.data.cassandra.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.mapping.CassandraPersistentEntity;
import org.springframework.data.cassandra.mapping.CassandraPersistentProperty;
import org.springframework.data.convert.EntityInstantiator;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.mapping.model.DefaultSpELExpressionEvaluator;
import org.springframework.data.mapping.model.SpELExpressionEvaluator;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.Row;

/**
 * Overridden classes to: 
 * - fix BIGINT/varint bug in Spring Cassandra. TODO:
 * Remove readEntityFromRow/BasicCassandraRowValueProvider once fixed in
 * upstream project. 
 * - add extra converters for Common GORM properties
 */
public class MappingCassandraConverter extends org.springframework.data.cassandra.convert.MappingCassandraConverter {
    public MappingCassandraConverter(CassandraMappingContext cassandraMapping) {
        super(cassandraMapping);
        DefaultConversionService conversionService = (DefaultConversionService) getConversionService();
        conversionService.addConverter(new StringToCurrencyConverter());
        conversionService.addConverter(new StringToLocaleConverter());
        conversionService.addConverter(new StringToTimeZoneConverter());
        conversionService.addConverter(new StringToURLConverter());
        BasicTypeConverterRegistrar registrar = new BasicTypeConverterRegistrar();
        registrar.register(conversionService);
    }

    @Override
    protected <S> S readEntityFromRow(CassandraPersistentEntity<S> entity, Row row) {
        DefaultSpELExpressionEvaluator evaluator = new DefaultSpELExpressionEvaluator(row, spELContext);

        BasicCassandraRowValueProvider rowValueProvider = new BasicCassandraRowValueProvider(row, evaluator);

        CassandraPersistentEntityParameterValueProvider parameterProvider = new CassandraPersistentEntityParameterValueProvider(entity, rowValueProvider, null);

        EntityInstantiator instantiator = instantiators.getInstantiatorFor(entity);
        S instance = instantiator.createInstance(entity, parameterProvider);

        BeanWrapper<S> wrapper = BeanWrapper.create(instance, conversionService);

        readPropertiesFromRow(entity, rowValueProvider, wrapper);

        return wrapper.getBean();
    }

    private static class BasicCassandraRowValueProvider extends org.springframework.data.cassandra.convert.BasicCassandraRowValueProvider {

        private final ColumnReader reader;
        private final SpELExpressionEvaluator evaluator;

        /**
         * Creates a new {@link BasicCassandraRowValueProvider} with the given
         * {@link Row} and {@link DefaultSpELExpressionEvaluator}.
         * 
         * @param source
         *            must not be {@literal null}.
         * @param evaluator
         *            must not be {@literal null}.
         */
        public BasicCassandraRowValueProvider(Row source, DefaultSpELExpressionEvaluator evaluator) {

            super(source, evaluator);

            this.reader = new ColumnReader(source) {
                @Override
                public Object get(int i) {
                    DataType type = columns.getType(i);
                    if (type.equals(DataType.varint())) {
                        return row.getVarint(i);
                    }
                    return super.get(i);
                }
            };
            this.evaluator = evaluator;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object getPropertyValue(CassandraPersistentProperty property) {

            String expression = property.getSpelExpression();
            if (expression != null) {
                return evaluator.evaluate(expression);
            }

            return reader.get(property.getColumnName());
        }

        @Override
        public Row getRow() {
            return reader.getRow();
        }
    }
}
