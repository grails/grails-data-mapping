/*
 * Copyright (c) 2010 by NPC International, Inc.
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

package org.springframework.datastore.mapping.riak;

import org.springframework.core.convert.converter.Converter;
import org.springframework.datastore.mapping.riak.engine.RiakEntityPersister;
import org.springframework.data.riak.core.RiakTemplate;
import org.springframework.datastore.mapping.core.AbstractSession;
import org.springframework.datastore.mapping.core.Datastore;
import org.springframework.datastore.mapping.engine.Persister;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.transactions.Transaction;

import java.math.BigInteger;

/**
 * @author Jon Brisbin <jon.brisbin@npcinternational.com>
 */
public class RiakSession extends AbstractSession {

  private RiakTemplate riakTemplate;

  public RiakSession(Datastore datastore, MappingContext mappingContext, RiakTemplate riakTemplate) {
    super(datastore, mappingContext);
    this.riakTemplate = riakTemplate;
    mappingContext.addTypeConverter(new BigIntegerToLongConverter());
  }

  @Override
  protected Persister createPersister(Class cls, MappingContext mappingContext) {
    PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
    if (null != entity) {
      return new RiakEntityPersister(mappingContext, entity, this, riakTemplate);
    }
    return null;
  }

  @Override
  protected Transaction beginTransactionInternal() {
    return new RiakTransaction(riakTemplate);
  }

  public boolean isConnected() {
    return true;
  }

  public Object getNativeInterface() {
    return riakTemplate;
  }

  protected class BigIntegerToLongConverter implements Converter<BigInteger, Long> {
    public Long convert(BigInteger integer) {
      return new Long(integer.longValue());
    }
  }

}
