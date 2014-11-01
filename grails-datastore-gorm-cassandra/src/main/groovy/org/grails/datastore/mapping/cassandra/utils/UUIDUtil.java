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
package org.grails.datastore.mapping.cassandra.utils;

import com.datastax.driver.core.utils.UUIDs;

/**
 * Utilities for creating regular and time based UUIDs for Cassandra
 *
 * Credit: http://blog.matygo.com/post/587641394/time-uuids-with-java-cassandra
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class UUIDUtil {

    public static java.util.UUID getRandomTimeUUID() {
        return UUIDs.timeBased();
    }

    public static java.util.UUID getRandomUUID() {
        return UUIDs.random();
    }

}
