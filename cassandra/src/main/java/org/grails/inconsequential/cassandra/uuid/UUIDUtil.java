/* Copyright (C) 2010 SpringSource
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
package org.grails.inconsequential.cassandra.uuid;

import java.nio.ByteBuffer;

/**
 * Utilities for creating Time based UUIDs for Cassandra
 *
 * Credit: http://blog.matygo.com/post/587641394/time-uuids-with-java-cassandra
 * 
 * @author Graeme Rocher
 * @since 1.0
 */
public class UUIDUtil {
  public static java.util.UUID getTimeUUID() {
      return java.util.UUID.fromString(new com.eaio.uuid.UUID().toString());
  }

  public static java.util.UUID getRandomUUID(){
      return java.util.UUID.randomUUID();
  }

  public static java.util.UUID toUUID( byte[] uuid ) {

      ByteBuffer buffer = ByteBuffer.allocate(16);

      buffer.put(uuid);

      buffer.rewind();

      com.eaio.uuid.UUID u = new com.eaio.uuid.UUID(buffer.getLong(),buffer.getLong());

      return java.util.UUID.fromString(u.toString());

  }

  public static byte[] asByteArray(java.util.UUID uuid) {

      ByteBuffer buffer = ByteBuffer.allocate(16);

      buffer.putLong(uuid.getMostSignificantBits());

      buffer.putLong(uuid.getLeastSignificantBits());

      return buffer.array();

  }
}
