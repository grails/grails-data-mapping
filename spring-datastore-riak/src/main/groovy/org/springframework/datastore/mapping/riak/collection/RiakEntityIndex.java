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

package org.springframework.datastore.mapping.riak.collection;

import org.springframework.data.keyvalue.riak.core.RiakTemplate;
import org.springframework.data.keyvalue.riak.mapreduce.*;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author J. Brisbin <jon@jbrisbin.com>
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
    MapReduceJob mapReduceJob = createFetchAtJob(i);
    return riakTemplate.execute(mapReduceJob, Map.class);
  }

  @Override
  public int size() {
    MapReduceJob mapReduceJob = createCountJob();
    return riakTemplate.execute(mapReduceJob, Integer.class);
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

  private MapReduceJob createFetchAtJob(int i) {
    MapReduceJob mapReduceJob = riakTemplate.createMapReduceJob();
    MapReduceOperation mapJs = new JavascriptMapReduceOperation(
        "function(values){ var row=Riak.mapValuesJson(values); return [row]; }");
    MapReducePhase mapPhase = new RiakMapReducePhase(MapReducePhase.Phase.MAP, "javascript", mapJs);
    mapReduceJob.addPhase(mapPhase);
    MapReduceOperation reduceJs = new JavascriptMapReduceOperation(String.format(
        "function(values){ return values[%s]; }",
        i));
    MapReducePhase reducePhase = new RiakMapReducePhase(MapReducePhase.Phase.REDUCE, "javascript", reduceJs);
    mapReduceJob.addPhase(reducePhase);

    return mapReduceJob;
  }

  private MapReduceJob createCountJob() {
    MapReduceJob mapReduceJob = riakTemplate.createMapReduceJob();
    MapReduceOperation mapJs = new JavascriptMapReduceOperation(
        "function(values){ var row=Riak.mapValuesJson(values); return [row]; }");
    MapReducePhase mapPhase = new RiakMapReducePhase(MapReducePhase.Phase.MAP, "javascript", mapJs);
    mapReduceJob.addPhase(mapPhase);
    MapReduceOperation reduceJs = new JavascriptMapReduceOperation("function(values){ return values.length; }");
    MapReducePhase reducePhase = new RiakMapReducePhase(MapReducePhase.Phase.REDUCE, "javascript", reduceJs);
    mapReduceJob.addPhase(reducePhase);

    return mapReduceJob;
  }
}
