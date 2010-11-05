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

import com.basho.riak.client.*;
import com.basho.riak.client.mapreduce.JavascriptFunction;
import com.basho.riak.client.request.MapReduceBuilder;
import com.basho.riak.client.request.RequestMeta;
import com.basho.riak.client.response.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
      return callback.doInRiak(riakClient);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw new DataAccessResourceFailureException(e.getMessage(), e);
    }
  }

  public boolean delete(final String bucket, final Long key) {
    return (Boolean) execute(new RiakCallback<RiakClient>() {
      public Object doInRiak(RiakClient riak) throws Exception {
        return riak.delete(bucket, String.format("%s", key)).isSuccess();
      }
    });
  }

  public void delete(final String bucket, final Long key, final RiakCallback<HttpResponse> callback) {
    execute(new RiakCallback<RiakClient>() {
      public Object doInRiak(RiakClient riak) throws Exception {
        HttpResponse resp = riak.delete(bucket, String.format("%s", key));
        return callback.doInRiak(resp);
      }
    });
  }

  @SuppressWarnings({"unchecked"})
  public Map<String, Object> fetch(final String bucket, final Long key) {
    return (Map<String, Object>) execute(new RiakCallback<RiakClient>() {
      public Object doInRiak(RiakClient riak) throws Exception {
        FetchResponse resp = riak.fetch(bucket, String.format("%s", key));
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

  public void fetch(final String bucket, final Long key, final RiakCallback<FetchResponse> callback) {
    execute(new RiakCallback<RiakClient>() {
      public Object doInRiak(RiakClient riak) throws Exception {
        FetchResponse resp = riak.fetch(bucket, String.format("%s", key));
        return callback.doInRiak(resp);
      }
    });
  }

  public Long fetchKeyAt(final String bucket, final int i) {
    return (Long) execute(new RiakCallback<RiakClient>() {
      public Object doInRiak(RiakClient riak) throws Exception {
        RequestMeta meta = new RequestMeta();
        meta.setQueryParam("keys", "true");
        BucketResponse bucketResp = riak.getBucketSchema(bucket, meta);
        if (bucketResp.isSuccess()) {
          RiakBucketInfo info = bucketResp.getBucketInfo();
          try {
            String key = (String) info.getKeys().toArray()[i];
            return Long.parseLong(key);
          } catch (IndexOutOfBoundsException ignored) {
          }
        }
        return null;
      }
    });
  }

  public Long store(final String bucket, final Map<String, Object> obj) {
    return (Long) execute(new RiakCallback<RiakClient>() {
      public Object doInRiak(RiakClient riak) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        mapper.writeValue(bout, obj);
        Long id = UUID.randomUUID().getLeastSignificantBits();
        RiakObject robj = new RiakObject(riak, bucket, String.format("%s", id), bout.toByteArray(), "application/json");
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

  public Long store(final String bucket, final Long key, final Map<String, Object> obj) {
    return (Long) execute(new RiakCallback<RiakClient>() {
      public Object doInRiak(RiakClient riak) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        mapper.writeValue(bout, obj);
        RiakObject robj = new RiakObject(riak,
            bucket,
            String.format("%s", key),
            bout.toByteArray(),
            "application/json");
        StoreResponse resp = riak.store(robj);
        if (resp.isSuccess()) {
          return Long.parseLong(robj.getKey());
        } else if (resp.isError() && resp.getStatusCode() >= 500) {
          throw new DataAccessResourceFailureException(String.format("Error encountered storing object: %s ",
              obj) + resp.getBodyAsString());
        }
        return null;
      }
    });
  }

  public Long store(final String bucket, final Long key, final Map<String, Object> obj, final RiakCallback<StoreResponse> callback) {
    return (Long) execute(new RiakCallback<RiakClient>() {
      public Object doInRiak(RiakClient riak) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        mapper.writeValue(bout, obj);
        RiakObject robj = new RiakObject(riak,
            bucket,
            String.format("%s", key),
            bout.toByteArray(),
            "application/json");
        StoreResponse resp = riak.store(robj);
        return callback.doInRiak(resp);
      }
    });
  }

  public void link(final String childBucket, final Long childKey, final String parentBucket, final Long parentKey, final String association) {
    execute(new RiakCallback<RiakClient>() {
      public Object doInRiak(RiakClient riak) throws Exception {
        FetchResponse resp = riak.fetch(childBucket, String.format("%s", childKey));
        if (resp.isSuccess()) {
          RiakLink link = new RiakLink(parentBucket, String.format("%s", parentKey), association);
          RiakObject child = resp.getObject();
          List<RiakLink> existingLinks = child.getLinks();
          if (null != existingLinks && !existingLinks.contains(link)) {
            child.addLink(link);
            child.store();
          }
          // Update special "index" entry
          List<RiakLink> links = new ArrayList<RiakLink>();
          links.add(new RiakLink(parentBucket, String.format("%s", parentKey), "parent"));
          links.add(new RiakLink(childBucket, String.format("%s", childKey), "entity"));
          String relBucket = formatRelBucketName(parentBucket, String.format("%s", parentKey), association);
          if (log.isDebugEnabled()) {
            log.debug("Associating " + relBucket + " with " + childBucket + "/" + childKey);
          }
          RiakObject relObj = new RiakObject(riak,
              relBucket,
              String.format("%s", childKey),
              new byte[0],
              "relationship/index",
              links);
          relObj.store();
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

  public List<Long> findChildKeysByOwner(final String ownerBucket, final Long ownerKey, final String association) {
    return (List<Long>) execute(new RiakCallback<RiakClient>() {
      public Object doInRiak(RiakClient riak) throws Exception {
        RequestMeta meta = new RequestMeta();
        meta.setQueryParam("keys", "true");
        BucketResponse resp = riak.listBucket(formatRelBucketName(ownerBucket,
            String.format("%s", ownerKey),
            association), meta);
        if (resp.isSuccess()) {
          List<Long> keys = new ArrayList<Long>();
          for (String s : resp.getBucketInfo().getKeys()) {
            keys.add(Long.parseLong(s));
          }
          return keys;
        }
        return new ArrayList<Long>();
      }
    });
  }

  public void walk(String bucket, Long key, String walkSpec) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public int count(final String bucket) {
    return (Integer) execute(new RiakCallback<RiakClient>() {
      public Object doInRiak(RiakClient riak) throws Exception {
        RequestMeta meta = new RequestMeta();
        meta.setQueryParam("keys", "true");
        BucketResponse resp = riak.getBucketSchema(bucket, meta);
        if (resp.isSuccess()) {
          return resp.getBucketInfo().getKeys().size();
        }
        return 0;
      }
    });
  }

  public List<Object> query(String bucket, String js) {
    MapReduceBuilder mapred = new MapReduceBuilder(riakClient);
    JavascriptFunction map = JavascriptFunction.anon(js);
    mapred.map(map, true);
    mapred.setBucket(bucket);
    MapReduceResponse resp = null;
    try {
      resp = mapred.submit();
    } catch (JSONException e) {
      log.error(e.getMessage(), e);
    }
    if (null != resp && resp.isSuccess()) {
      try {
        return mapper.readValue(resp.getBodyAsString(), List.class);
      } catch (IOException e) {
        log.error(e.getMessage(), e);
      }
    } else {
      throw new IllegalArgumentException("Error executing M/R script. Check server logs for details.");
    }
    return null;
  }

  public void clear(final String bucket) {
    execute(new RiakCallback<RiakClient>() {
      public Object doInRiak(RiakClient riak) throws Exception {
        RequestMeta meta = new RequestMeta();
        meta.setQueryParam("keys", "true");
        BucketResponse resp = riak.getBucketSchema(bucket, meta);
        if (resp.isSuccess()) {
          for (String key : resp.getBucketInfo().getKeys()) {
            riak.delete(bucket, key);
          }
        }
        return null;
      }
    });
  }

  protected String formatRelBucketName(String b, String k, String a) {
    return String.format("%s.%s.%s", b, a, k);
  }
}
