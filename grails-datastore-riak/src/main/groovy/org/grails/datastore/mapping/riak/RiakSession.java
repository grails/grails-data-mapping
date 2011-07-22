/*
 * Copyright (c) 2010 by J. Brisbin <jon@jbrisbin.com>
 *     Portions (c) 2010 by NPC International, Inc. or the
 *     original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grails.datastore.mapping.riak;

import java.math.BigInteger;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.keyvalue.riak.core.QosParameters;
import org.springframework.data.keyvalue.riak.core.RiakTemplate;
import org.grails.datastore.mapping.core.AbstractSession;
import org.grails.datastore.mapping.core.Datastore;
import org.grails.datastore.mapping.engine.Persister;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.riak.engine.RiakEntityPersister;
import org.grails.datastore.mapping.transactions.Transaction;

/**
 * A {@link org.grails.datastore.mapping.core.Session} implementation for the Riak
 * Key/Value store.
 *
 * @author J. Brisbin <jon@jbrisbin.com>
 */
@SuppressWarnings({"unchecked"})
public class RiakSession extends AbstractSession {

    private RiakTemplate riakTemplate;
    private QosParameters qosParameters;

    public RiakSession(Datastore datastore, MappingContext mappingContext,
             RiakTemplate riakTemplate, ApplicationEventPublisher publisher) {
        super(datastore, mappingContext, publisher);
        this.riakTemplate = riakTemplate;
        mappingContext.addTypeConverter(new BigIntegerToLongConverter());
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
        if (null == entity) {
            return null;
        }
        return new RiakEntityPersister(mappingContext, entity, this, riakTemplate, publisher);
    }

    @Override
    protected Transaction beginTransactionInternal() {
        return new RiakTransaction(riakTemplate);
    }

    public Object getNativeInterface() {
        return riakTemplate;
    }

    public QosParameters getQosParameters() {
        return qosParameters;
    }

    /**
     * Set the Riak Quality Of Service parameters to use during this session.
     *
     * @param qosParameters
     */
    public void setQosParameters(QosParameters qosParameters) {
        this.qosParameters = qosParameters;
    }

    protected class BigIntegerToLongConverter implements Converter<BigInteger, Long> {
        public Long convert(BigInteger integer) {
            return Long.valueOf(integer.longValue());
        }
    }
}
