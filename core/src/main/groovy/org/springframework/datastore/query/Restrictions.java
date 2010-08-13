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
package org.springframework.datastore.query;

/**
 * Factory for creating {@link org.springframework.datastore.query.Query.Criterion} instances
 */
public class Restrictions {

    public static Query.Equals eq(String property, Object value) {
        return new Query.Equals(property, value);
    }

    public static Query.Criterion and(Query.Criterion a, Query.Criterion b) {
        return new Query.Conjunction().add(a).add(b);
    }

    public static Query.Criterion or(Query.Criterion a, Query.Criterion b) {
        return new Query.Disjunction().add(a).add(b);
    }
}
