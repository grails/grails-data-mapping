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

import org.springframework.datastore.mapping.riak.util.RiakTemplate;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Jon Brisbin <jon.brisbin@npcinternational.com>
 */
public class RiakEntityIndex<Long> extends AbstractList implements List, RiakCollection {

  private RiakTemplate riakTemplate;
  private String bucket;

  public RiakEntityIndex(RiakTemplate riakTemplate, String bucket) {
    this.riakTemplate = riakTemplate;
    this.bucket = bucket;
  }

  @Override
  public Object get(int i) {
    return riakTemplate.fetchKeyAt(bucket, i);
  }

  @Override
  public int size() {
    return riakTemplate.count(bucket);
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public Iterator iterator() {
    return super.iterator();    //To change body of overridden methods use File | Settings | File Templates.
  }

  public String getBucket() {
    return this.bucket;
  }
}
