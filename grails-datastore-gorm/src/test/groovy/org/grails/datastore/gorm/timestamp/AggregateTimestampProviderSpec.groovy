package org.grails.datastore.gorm.timestamp

import grails.spring.BeanBuilder
import org.grails.datastore.gorm.bootstrap.support.InstanceFactoryBean

import spock.lang.Specification

public class AggregateTimestampProviderSpec extends Specification {

    def "should support autowiring beans"() {
        given:
        def mockTimestampProvider = Mock(TimestampProvider)
        def someDate = new Date()
        def ctx = new BeanBuilder().beans {
            xmlns context:"http://www.springframework.org/schema/context"
            context.'annotation-config'()
            
            timestampProvider(AggregateTimestampProvider)
            mockTimestampProviderBean(InstanceFactoryBean, mockTimestampProvider, TimestampProvider)
        }.createApplicationContext()
        when:
        def timestampProvider = ctx.timestampProvider
        then:
        timestampProvider != null
        when:
        timestampProvider.supportsCreating(Date) == true
        then:
        mockTimestampProvider.supportsCreating(Date) >> true
        when:
        timestampProvider.createTimestamp(Date) == someDate
        then:
        mockTimestampProvider.createTimestamp(Date) >> someDate 
    }
    
    def "should support autowiring multiple timestamp provider beans"() {
        given:
        def mockTimestampProvider1 = Mock(TimestampProvider)
        def mockTimestampProvider2 = Mock(TimestampProvider)
        def someDate = new Date()
        def ctx = new BeanBuilder().beans {
            xmlns context:"http://www.springframework.org/schema/context"
            context.'annotation-config'()
            
            timestampProvider(AggregateTimestampProvider)
            mockTimestampProviderBean1(InstanceFactoryBean, mockTimestampProvider1, TimestampProvider)
            mockTimestampProviderBean2(InstanceFactoryBean, mockTimestampProvider2, TimestampProvider)
        }.createApplicationContext()
        when:
        def timestampProvider = ctx.timestampProvider
        then:
        timestampProvider != null
        when:
        timestampProvider.supportsCreating(Date) == true
        then:
        mockTimestampProvider1.supportsCreating(Date) >> true
        when:
        timestampProvider.createTimestamp(Date) == someDate
        then:
        mockTimestampProvider1.supportsCreating(Date) >> true
        mockTimestampProvider1.createTimestamp(Date) >> someDate
        when:
        timestampProvider.createTimestamp(String) == "Hello"
        then:
        mockTimestampProvider1.supportsCreating(String) >> false
        mockTimestampProvider2.supportsCreating(String) >> true
        mockTimestampProvider2.createTimestamp(String) >> "Hello"
    }

}
