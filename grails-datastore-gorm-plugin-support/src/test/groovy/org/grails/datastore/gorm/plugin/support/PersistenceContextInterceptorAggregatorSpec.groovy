/* Copyright (C) 2014 Pivotal Software, Inc.
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

package org.grails.datastore.gorm.plugin.support

import grails.spring.BeanBuilder

import org.codehaus.groovy.grails.support.PersistenceContextInterceptor
import org.springframework.context.ApplicationContext

import spock.lang.Specification

class PersistenceContextInterceptorAggregatorSpec extends Specification {

    void "Test that the ApplicationContext constructed is valid when PersistenceContextInterceptorAggregator is used"() {
        when:"An application context is configured"
            ApplicationContext ctx = systemUnderTest()
        then:"The context contains the necessary beans"
            ctx.containsBean("persistenceInterceptor")
            !ctx.containsBean("firstPersistenceInterceptor")
            !ctx.containsBean("secondPersistenceInterceptor")
            !ctx.containsBean("thirdPersistenceInterceptor")
        then:"The persistenceInterceptor is type of AggregatePersistenceContextInterceptor and can be found by getBean(Class) method from context"
            def persistenceInterceptor = ctx.getBean(PersistenceContextInterceptor)
            persistenceInterceptor.class == AggregatePersistenceContextInterceptor
        then:"The aggregate interceptor has all 3 interceptors"
            def interceptors = persistenceInterceptor.interceptors
            interceptors.size() == 3
            interceptors*.name.containsAll(['first','second','third'])
        then:"getBeansOfType(PersistenceContextInterceptor) doesn't return the inner beans"
            ctx.getBeansOfType(PersistenceContextInterceptor).size() == 1
    }
    
    void "Test that the AggregatePersistenceContextInterceptor class calls all interceptors"() {
        when:"An application context is configured"
            ApplicationContext ctx = systemUnderTest()
            def persistenceInterceptor = ctx.getBean(PersistenceContextInterceptor)
            persistenceInterceptor.with {
                init()
                reconnect()
                flush()
                clear()
                setReadOnly()
                setReadWrite()
                isOpen()
                disconnect()
                // destroy gets only called for open interceptors
                interceptors.each { interceptor ->
                    interceptor.open = true
                }
                destroy()
            }
        then:"Call all methods on the interceptor and check calls"
            persistenceInterceptor.interceptors.size() == 3
            persistenceInterceptor.interceptors.each {
                assert it.methodsCalled.size()==9
            }
    }
    

    ApplicationContext systemUnderTest() {
        def bb = new BeanBuilder()
        bb.beans {
            persistenceContextInterceptorAggregator(PersistenceContextInterceptorAggregator)
            firstPersistenceInterceptor(DummyPersistenceContextInterceptor) {
                name = 'first'
            }
            secondPersistenceInterceptor(DummyPersistenceContextInterceptor) {
                name = 'second'
            }
            thirdPersistenceInterceptor(DummyPersistenceContextInterceptor) {
                name = 'third'
            }
            // each GORM provider adds it own PersistenceContextInterceptorAggregator BeanFactoryPostProcessor, so test it with 2 postprocessors
            secondPersistenceContextInterceptorAggregator(PersistenceContextInterceptorAggregator)
        }
        bb.createApplicationContext()
    }
    
    void "Test that PersistenceContextInterceptorAggregator doesn't do changes when there is only one interceptor"() {
        when:
            def ctx = new BeanBuilder().beans {
                persistenceContextInterceptorAggregator(PersistenceContextInterceptorAggregator)
                singlePersistenceInterceptor(DummyPersistenceContextInterceptor) {
                    name = 'single'
                }
            }.createApplicationContext()
        then:"The context contains the necessary beans"
            !ctx.containsBean("persistenceInterceptor")
            ctx.containsBean("singlePersistenceInterceptor")
        then:"The persistenceInterceptor is type of DummyPersistenceContextInterceptor and can be found by getBean(Class) method from context"
            def persistenceInterceptor = ctx.getBean(PersistenceContextInterceptor)
            persistenceInterceptor.class == DummyPersistenceContextInterceptor
        then:"getBeansOfType(PersistenceContextInterceptor) returns one interceptor"
            ctx.getBeansOfType(PersistenceContextInterceptor).size() == 1

    }
}

class DummyPersistenceContextInterceptor implements PersistenceContextInterceptor {
    def methodsCalled = [:]
    String name
    boolean open = false
    
    @Override
    public void init() {
        methodsCalled.init = true    
    }

    @Override
    public void destroy() {
        methodsCalled.destroy = true
    }

    @Override
    public void disconnect() {
        methodsCalled.disconnect = true
    }

    @Override
    public void reconnect() {
        methodsCalled.reconnect = true
    }

    @Override
    public void flush() {
        methodsCalled.flush = true
    }

    @Override
    public void clear() {
        methodsCalled.clear = true
    }

    @Override
    public void setReadOnly() {
        methodsCalled.setReadOnly = true
    }

    @Override
    public void setReadWrite() {
        methodsCalled.setReadWrite = true
    }

    @Override
    public boolean isOpen() {
        methodsCalled.isOpen = true
        open
    }
}
