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

import com.basho.riak.client.response.FetchResponse;
import com.basho.riak.client.response.HttpResponse;
import com.basho.riak.client.response.StoreResponse;

import java.util.List;
import java.util.Map;

/**
 * @author Jon Brisbin <jon.brisbin@npcinternational.com>
 */
public interface RiakTemplate<T> {

  Object execute(RiakCallback<T> callback);

  boolean delete(String bucket, String key);

  void delete(String bucket, String key, RiakCallback<HttpResponse> callback);

  Map<String, Object> fetch(String bucket, String key);

  void fetch(String bucket, String key, RiakCallback<FetchResponse> callback);

  String store(String bucket, String key, Map<String, Object> obj);

  String store(String bucket, Map<String, Object> obj);

  String store(String bucket, String key, Map<String, Object> obj, RiakCallback<StoreResponse> callback);

  void link(String childBucket, String childKey, String parentBucket, String parentKey, String association);

  List<String> findChildKeysByOwner(String ownerBucket, String ownerKey, String association);

  void walk(String bucket, String key, String walkSpec);
}
