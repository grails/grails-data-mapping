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

package org.springframework.datastore.mapping.riak.util;

import com.basho.riak.client.RiakClient;
import com.basho.riak.client.RiakConfig;
import com.basho.riak.client.RiakLink;
import com.basho.riak.client.RiakObject;
import com.basho.riak.client.mapreduce.JavascriptFunction;
import com.basho.riak.client.request.MapReduceBuilder;
import com.basho.riak.client.response.FetchResponse;
import com.basho.riak.client.response.HttpResponse;
import com.basho.riak.client.response.MapReduceResponse;
import com.basho.riak.client.response.StoreResponse;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Jon Brisbin <jon.brisbin@npcinternational.com>
 */
public class RiakJavaClientTemplate implements RiakTemplate<RiakClient> {

  private Logger log = LoggerFactory.getLogger(getClass());
  private RiakClient riakClient;
  private ObjectMapper mapper = new ObjectMapper();

  public RiakJavaClientTemplate(RiakConfig riakConfig) {
    this.riakClient = new RiakClient(riakConfig);
  }

  public RiakJavaClientTemplate(RiakClient riakClient) {
    this.riakClient = riakClient;
  }

  public Object execute(RiakCallback<RiakClient> callback) {
    try {
      if (log.isDebugEnabled()) {
        log.debug("Executing Riak callback: " + callback);
      }
      return callback.doInRiak(riakClient);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new DataAccessResourceFailureException(e.getMessage(), e);
    }
  }

  public boolean delete(final String bucket, final String key) {
    return (Boolean) execute(new RiakCallback<RiakClient>() {
      public Object doInRiak(RiakClient riak) throws Exception {
        return riak.delete(bucket, key).isSuccess();
      }
    });
  }

  public void delete(final String bucket, final String key, final RiakCallback<HttpResponse> callback) {
    execute(new RiakCallback<RiakClient>() {
      public Object doInRiak(RiakClient riak) throws Exception {
        HttpResponse resp = riak.delete(bucket, key);
        return callback.doInRiak(resp);
      }
    });
  }

  @SuppressWarnings({"unchecked"})
  public Map<String, Object> fetch(final String bucket, final String key) {
    return (Map<String, Object>) execute(new RiakCallback<RiakClient>() {
      public Object doInRiak(RiakClient riak) throws Exception {
        FetchResponse resp = riak.fetch(bucket, key);
        if (resp.isSuccess()) {
          byte[] bytes = resp.getBody();
          return mapper.readValue(bytes, 0, bytes.length, Map.class);
        } else if (resp.getStatusCode() >= 500) {
          throw new DataAccessResourceFailureException(String.format("Error encountered retrieving object for key: %s ",
              key) + resp.getBodyAsString());
        }
        return null;
      }
    });
  }

  public void fetch(final String bucket, final String key, final RiakCallback<FetchResponse> callback) {
    execute(new RiakCallback<RiakClient>() {
      public Object doInRiak(RiakClient riak) throws Exception {
        FetchResponse resp = riak.fetch(bucket, key);
        return callback.doInRiak(resp);
      }
    });
  }

  public String store(final String bucket, final Map<String, Object> obj) {
    return (String) execute(new RiakCallback<RiakClient>() {
      public Object doInRiak(RiakClient riak) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        mapper.writeValue(bout, obj);
        String id = UUID.randomUUID().toString();
        RiakObject robj = new RiakObject(riak, bucket, id, bout.toByteArray(), "application/json");
        StoreResponse resp = riak.store(robj);
        if (resp.isSuccess()) {
          return id;
        } else if (resp.isError() && resp.getStatusCode() >= 500) {
          throw new DataAccessResourceFailureException(String.format("Error encountered storing object: %s ",
              obj) + resp.getBodyAsString());
        }
        return null;
      }
    });
  }

  public String store(final String bucket, final String key, final Map<String, Object> obj) {
    return (String) execute(new RiakCallback<RiakClient>() {
      public Object doInRiak(RiakClient riak) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        mapper.writeValue(bout, obj);
        RiakObject robj = new RiakObject(riak, bucket, key, bout.toByteArray(), "application/json");
        StoreResponse resp = riak.store(robj);
        if (resp.isSuccess()) {
          return robj.getKey();
        } else if (resp.isError() && resp.getStatusCode() >= 500) {
          throw new DataAccessResourceFailureException(String.format("Error encountered storing object: %s ",
              obj) + resp.getBodyAsString());
        }
        return null;
      }
    });
  }

  public String store(final String bucket, final String key, final Map<String, Object> obj, final RiakCallback<StoreResponse> callback) {
    return null;
  }

  public void link(final String childBucket, final String childKey, final String parentBucket, final String parentKey, final String association) {
    execute(new RiakCallback<RiakClient>() {
      public Object doInRiak(RiakClient riak) throws Exception {
        FetchResponse resp = riak.fetch(childBucket, childKey);
        if (resp.isSuccess()) {
          RiakLink link = new RiakLink(parentBucket, parentKey, association);
          RiakObject child = resp.getObject();
          child.addLink(link);
          child.store();
        } else if (resp.isError() && resp.getStatusCode() >= 500) {
          throw new DataAccessResourceFailureException(String.format("Error encountered linking %s/%s to %s/%s: %s ",
              childBucket,
              childKey,
              parentBucket,
              parentKey)
              + resp.getBodyAsString());
        }
        return null;
      }
    });
  }

  public List<String> findByOwner(final String childBucket, final String ownerBucket, final String ownerKey, final String association) {
    return (List<String>) execute(new RiakCallback<RiakClient>() {
      public Object doInRiak(RiakClient riak) throws Exception {
        MapReduceBuilder mapred = new MapReduceBuilder(riak);
        mapred.setBucket(childBucket);
        mapred.map(JavascriptFunction.anon(
            "function(data){ " +
                "  var r = [];" +
                "  var o = Riak.mapValuesJson(data);" +
                "  var links = data.values[0].metadata['Links'];" +
                "  if(links) {" +
                "    for(i in links) {" +
                "      var link = links[i];" +
                "      if(link[0] == '" + ownerBucket + "' && link[1] == '" + ownerKey + "' && link[2] == '" + association + "') {" +
                "        ejsLog('/tmp/riak_mapred.log', 'link: '+JSON.stringify(link));" +
                "        r.push(o);" +
                "      }" +
                "    }" +
                "  }" +
                "  ejsLog('/tmp/riak_mapred.log', JSON.stringify(data));" +
                "  return r;" +
                "}"),
            false);
        MapReduceResponse resp = mapred.submit();
        if (log.isDebugEnabled()) {
          log.debug("Map/Reduce response: " + resp.getBodyAsString());
        }
        return new ArrayList<String>();
      }
    });
  }

  public void walk(String bucket, String key, String walkSpec) {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}
