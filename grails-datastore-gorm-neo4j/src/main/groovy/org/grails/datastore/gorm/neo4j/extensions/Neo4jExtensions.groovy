/*
 * Copyright 2015 original authors
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
package org.grails.datastore.gorm.neo4j.extensions

import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.MapEntryOrKeyValue
import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.Node
import org.neo4j.graphdb.Result


/**
 * Extension methods to improve the Neo4j experience in Groovy.
 *
 * @author Graeme Rocher
 * @since 5.0
 */
@CompileStatic
class Neo4jExtensions {

    /**
     * Allows the subscript operator on nodes
     */
    static Object getAt(Node node, String name) {
        node.getProperty(name)
    }

    /**
     * Allows the subscript operator on nodes
     */
    static void putAt(Node node, String name, value) {
        node.setProperty(name, value)
    }

    /**
     * Allows a Node to be iterated through using a closure. If the
     * closure takes one parameter then it will be passed the Map.Entry
     * otherwise if the closure takes two parameters then it will be
     * passed the key and the value.
     * <pre class="groovyTestCase">def result = ""
     * [a:1, b:3].each { key, value -> result += "$key$value" }
     * assert result == "a1b3"</pre>
     * <pre class="groovyTestCase">def result = ""
     * [a:1, b:3].each { entry -> result += entry }
     * assert result == "a=1b=3"</pre>
     *
     *
     * @param self    the map over which we iterate
     * @param closure the 1 or 2 arg closure applied on each entry of the map
     * @return returns the self parameter
     */
    public static Node each(Node self, @ClosureParams(MapEntryOrKeyValue.class) Closure closure) {
        for (final String prop : self.propertyKeys) {
            final value = self.getProperty(prop)
            callClosureForMapEntry(closure, new Map.Entry<String, Object>() {
                @Override
                String getKey() {
                    prop
                }

                @Override
                Object getValue() {
                    value
                }

                @Override
                Object setValue(Object v) {
                    self.setProperty(prop, v)
                    return value
                }
            })
        }
        return self;
    }

    /**
     * Executes a cypher query with positional parameters
     *
     * @param databaseService The GraphDatabaseService
     * @param cypher The cypher query
     * @param positionalParameters The position parameters
     * @return The query result
     */
    static Result execute(GraphDatabaseService databaseService, String cypher, List<Object> positionalParameters) {
        Map<String,Object> params = new LinkedHashMap<>()
        int i = 0;
        for(p in positionalParameters) {
            params.put(String.valueOf(++i), p)
        }
        databaseService.execute(cypher, params)
    }


    // internal helper method
    protected static <T> T callClosureForMapEntry(Closure<T> closure, Map.Entry entry) {
        if (closure.getMaximumNumberOfParameters() == 2) {
            return closure.call(entry.key, entry.value);
        }
        return closure.call(entry)
    }
}
