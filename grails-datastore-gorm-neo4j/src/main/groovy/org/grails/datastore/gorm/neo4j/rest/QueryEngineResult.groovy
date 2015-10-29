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
package org.grails.datastore.gorm.neo4j.rest

import groovy.transform.CompileStatic
import org.neo4j.graphdb.ExecutionPlanDescription
import org.neo4j.graphdb.Notification
import org.neo4j.graphdb.QueryExecutionType
import org.neo4j.graphdb.QueryStatistics
import org.neo4j.graphdb.ResourceIterator
import org.neo4j.graphdb.Result
import org.neo4j.helpers.Listeners


/**
 * Adapts the QueryEngine result to a Neo4j Result
 *
 * @author Graeme Rocher
 * @since 5.0
 */
@CompileStatic
class QueryEngineResult implements Result {

    final org.springframework.data.neo4j.conversion.Result<Map<String,Object>> internalResult
    final Iterator<Map<String,Object>> iterator

    QueryEngineResult(org.springframework.data.neo4j.conversion.Result internalResult) {
        this.internalResult = internalResult
        this.iterator = internalResult.iterator()
    }

    @Override
    QueryExecutionType getQueryExecutionType() {
        QueryExecutionType.query(QueryExecutionType.QueryType.READ_WRITE)
    }

    @Override
    List<String> columns() {
        throw new UnsupportedOperationException("Method columns not supported")
    }

    @Override
    def <T> ResourceIterator<T> columnAs(String name) {
        throw new UnsupportedOperationException("Method columnAs not supported")
    }

    @Override
    boolean hasNext() {
        return iterator.hasNext()
    }

    @Override
    Map<String, Object> next() {
        return iterator.next()
    }

    @Override
    void close() {
       internalResult.finish()
    }

    @Override
    QueryStatistics getQueryStatistics() {
        throw new UnsupportedOperationException("Method getQueryStatistics() not supported")
    }

    @Override
    ExecutionPlanDescription getExecutionPlanDescription() {
        throw new UnsupportedOperationException("Method getExecutionPlanDescription()) not supported")
    }

    @Override
    String resultAsString() {
        return iterator.toString()
    }

    @Override
    void writeAsStringTo(PrintWriter writer) {
        writer.write(resultAsString())
    }

    @Override
    Iterable<Notification> getNotifications() {
        throw new UnsupportedOperationException("Method getNotifications() not supported")
    }

    public <VisitationException extends Exception> void accept( Result.ResultVisitor<VisitationException> visitor )
            throws VisitationException {
        throw new UnsupportedOperationException("Method accept() not supported")
    }
    @Override
    void remove() {
        // no-op
    }
}
