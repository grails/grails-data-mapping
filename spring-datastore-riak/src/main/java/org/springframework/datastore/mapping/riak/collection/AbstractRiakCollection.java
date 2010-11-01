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

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Jon Brisbin <jon.brisbin@npcinternational.com>
 */
public class AbstractRiakCollection implements RiakCollection {

  protected RiakTemplate riakTemplate;
  protected String bucket;
  protected String ownerKey;
  protected String ownerAssociation;

  public AbstractRiakCollection(RiakTemplate riakTemplate, String bucket, String ownerKey, String ownerAssociation) {
    this.riakTemplate = riakTemplate;
    this.bucket = bucket;
    this.ownerKey = ownerKey;
    this.ownerAssociation = ownerAssociation;
  }

  public String getBucket() {
    return this.bucket;
  }

  public String getOwnerKey() {
    return this.ownerKey;
  }

  public String getOwnerAssociation() {
    return this.ownerAssociation;
  }

  public int size() {
    return 0;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public boolean isEmpty() {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public boolean contains(Object o) {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public Iterator iterator() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public Object[] toArray() {
    return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  public Object[] toArray(Object[] objects) {
    return new Object[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  public boolean add(Object o) {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public boolean remove(Object o) {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public boolean containsAll(Collection objects) {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public boolean addAll(Collection collection) {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public boolean removeAll(Collection objects) {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public boolean retainAll(Collection objects) {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void clear() {
    //To change body of implemented methods use File | Settings | File Templates.
  }

}
