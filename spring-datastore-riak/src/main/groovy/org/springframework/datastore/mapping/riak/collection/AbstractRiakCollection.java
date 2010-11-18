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

package org.springframework.datastore.mapping.riak.collection;


import org.springframework.datastore.riak.core.RiakTemplate;

import java.util.Collection;
import java.util.List;

/**
 * @author Jon Brisbin <jon.brisbin@npcinternational.com>
 */
public abstract class AbstractRiakCollection<T> implements Collection, RiakCollection {

  protected RiakTemplate riakTemplate;
  protected String bucket;

  public AbstractRiakCollection(RiakTemplate riakTemplate, String bucket) {
    this.riakTemplate = riakTemplate;
    this.bucket = bucket;
  }

  public String getBucket() {
    return this.bucket;
  }

  public int size() {
    List<?> keys = (List<?>) riakTemplate.getBucketSchema(bucket, true).get("keys");
    return keys.size();
  }

  public boolean isEmpty() {
    return (size() == 0);
  }


}
