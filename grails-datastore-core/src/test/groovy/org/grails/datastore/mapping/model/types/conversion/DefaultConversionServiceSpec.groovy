package org.grails.datastore.mapping.model.types.conversion

import spock.lang.Specification

class DefaultConversionServiceSpec extends Specification {

    DefaultConversionService conversionService = new DefaultConversionService()

    def "CharSequence conversions should be supported"() {
        expect:
            conversionService.convert("${'123'}", Integer) == 123
    }

    def "enum conversions should be supported"() {
        expect:
            conversionService.convert("ONE", MyEnum) == MyEnum.ONE
            conversionService.convert(MyEnum.THREE, String) == "THREE"
    }

    enum MyEnum {
        ONE,
        TWO,
        THREE
    }
}
