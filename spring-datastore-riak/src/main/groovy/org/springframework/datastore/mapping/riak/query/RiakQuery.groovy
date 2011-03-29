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

package org.springframework.datastore.mapping.riak.query

import org.codehaus.groovy.runtime.typehandling.GroovyCastException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.keyvalue.riak.core.RiakTemplate
import org.springframework.data.keyvalue.riak.mapreduce.*
import org.springframework.datastore.mapping.core.Session
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.query.Query

/**
 * A {@link Query} implementation for the Riak Key/Value store.
 * <p/>
 * This query implementation relies heavily on Riak's native Map/Reduce functionality. It
 * expects data to be stored as JSON documents, which is how GORM stores objects into Riak.
 *
 * @author J. Brisbin <jon@jbrisbin.com>
 */
class RiakQuery extends Query {

    static Logger log = LoggerFactory.getLogger(RiakQuery)

    static queryHandlers
    static {
        queryHandlers = [
            (Query.Equals): { Query.Equals equals, PersistentEntity entity, buff ->
                def val = checkForDate(equals.value)
                if (val instanceof Boolean) {
                    // If we're comparing booleans, we need to see what format the entry property
                    // is in. If it's not already a boolean, make it one.
                    buff << "(typeof entry.${equals.name} === 'boolean' ? " + (val ? "entry.${equals.name}" : "!entry.${equals.name}") + " : " + (val ? "Boolean(entry.${equals.name})" : "!Boolean(entry.${equals.name})") + ")"
                } else if (val instanceof Integer || val instanceof Double || val instanceof Long) {
                    if ("id" == equals.name) {
                        // Handle object IDs by comparing to Riak's key field.
                        buff << "v.key === \"${val}\""
                    } else {
                        // All else must be a number, don't use quotes.
                        buff << "entry.${equals.name} == ${val}"
                    }
                } else {
                    // Compare with quotes.
                    buff << "entry.${equals.name} === \"${val}\""
                }
            },
            (Query.IdEquals): { Query.IdEquals idEquals, PersistentEntity entity, buff ->
                buff << "v.key === \"${idEquals.value}\""
            },
            (Query.NotEquals): { Query.NotEquals notEquals, PersistentEntity entity, buff ->
                def val = checkForDate(notEquals.value)
                if (val instanceof Boolean) {
                    buff << "(typeof entry.${notEquals.name} === 'boolean' ? " + (val ? "entry.${notEquals.name}" : "!entry.${notEquals.name}") + " : " + (val ? "Boolean(entry.${notEquals.name})" : "!Boolean(entry.${notEquals.name})") + ")"
                } else if (val instanceof Integer || val instanceof Double || val instanceof Long) {
                    if ("id" == equals.name) {
                        buff << "v.key !== \"${val}\""
                    } else {
                        buff << "entry.${notEquals.name} != ${val}"
                    }
                } else {
                    buff << "entry.${notEquals.name} !== \"${val}\""
                }
            },
            (Query.GreaterThan): { Query.GreaterThan gt, PersistentEntity entity, buff ->
                def val = checkForDate(gt.value)
                buff << "entry.${gt.name} > ${val}"
            },
            (Query.GreaterThanEquals): { Query.GreaterThanEquals gte, PersistentEntity entity, buff ->
                def val = checkForDate(gte.value)
                buff << "entry.${gte.name} >= ${val}"
            },
            (Query.LessThan): { Query.LessThan lt, PersistentEntity entity, buff ->
                def val = checkForDate(lt.value)
                buff << "entry.${lt.name} < ${val}"
            },
            (Query.LessThanEquals): { Query.LessThanEquals lte, PersistentEntity entity, buff ->
                def val = checkForDate(lte.value)
                buff << "entry.${lte.name} <= ${val}"
            },
            (Query.Between): { Query.Between between, PersistentEntity entity, buff ->
                def to = checkForDate(between.to)
                def from = checkForDate(between.from)
                buff << "(entry.${between.property} <= ${to}) && (entry.${between.property} >= ${from})"
            },
            (Query.Like): { Query.Like like, PersistentEntity entity, buff ->
                def regexp = like.value.toString().replaceAll("%", "(.+)")
                buff << "/^$regexp/.test(\"\"+entry.${like.name})"
            },
            (Query.Conjunction): { Query.Conjunction and, PersistentEntity entity, buff ->
                def conjunc = RiakQuery.handleJunction(and, entity).collect { "(" + it + ")"}.join("&&")
                buff << "($conjunc)"
            },
            (Query.Disjunction): { Query.Disjunction or, PersistentEntity entity, buff ->
                def disjunc = RiakQuery.handleJunction(or, entity).collect { "(" + it + ")"}.join("||")
                buff << "($disjunc)"
            },
            (Query.Negation): { Query.Negation not, PersistentEntity entity, buff ->
                def neg = RiakQuery.handleJunction(not, entity).collect { "!(" + it + ")"}.join("&&")
                buff << neg
            },
            (Query.In): { Query.In qin, PersistentEntity entity, buff ->
                def inClause = qin.values.collect {
                    "(entry.${qin.name} === " + (it.isInteger() ? it : "\"$it\"") + ")"
                }.join("||")
                buff << "($inClause)"
            }
        ]
    }

    RiakTemplate riak

    RiakQuery(Session session, PersistentEntity ent, RiakTemplate riak) {
        super(session, ent);
        this.riak = riak
    }

    protected List executeQuery(PersistentEntity entity, Query.Junction criteria) {
        def buff = []
        criteria.criteria.each { criterion ->
            if (log.debugEnabled) {
                log.debug "Found criteria: ${criterion}"
            }
            def handler = queryHandlers[criterion.class]
            if (handler) {
                handler(criterion, entity, buff)
            } else {
                return []
            }
        }
        // If any criteria exist, join them together into a meaningful if clause.
        def joinStr = (criteria && criteria instanceof Query.Disjunction ? "||" : "&&")
        def ifclause = buff.join(joinStr) ?: "true"
        StringBuilder mapJs = new StringBuilder("function(v){ var uuid='%UUID%'; var r=[];")
        if (log.debugEnabled) {
            mapJs << "ejsLog('/tmp/mapred.log', 'map input: '+JSON.stringify(v));"
        }
        if (ifclause != "true") {
            // All this mess is to catch as many weird cases as possible. If I check for it here,
            // I've seen it in testing and thought it prudent to catch as many errors as possible
            // until I don't need to check for them any more.
            mapJs << "try{if(v === [] || typeof v['values'] == \"undefined\" || v.values[0].data === \"\"){return [];}}catch(e){return [];};var o=Riak.mapValuesJson(v);for(i in o){var entry=o[i];"
            if (ifclause != "true") {
                mapJs << "if(${ifclause}){"
            }
            mapJs << "o[i].id=v.key;r.push(o[i]);}"
            if (log.debugEnabled) {
                mapJs << "} ejsLog('/tmp/mapred.log', 'map return: '+JSON.stringify(r)); return r;}"
            } else {
                mapJs << "} return r;}"
            }
        } else {
            // Just copy all values to output, adding the key as the 'id' property.
            mapJs << " var row = Riak.mapValuesJson(v); row[0].id = v.key; ejsLog('/tmp/mapred.log', 'map return: '+JSON.stringify(row)); return row; }"
        }
        def reduceJs
        // Property projections. Implemented as Riak reduce functions.
        List<Query.PropertyProjection> propProjs = new ArrayList<Query.PropertyProjection>()
        def projectionList = projections()
        if (projectionList.projections) {
            StringBuilder jsbuff = new StringBuilder("function(reduced){ var uuid='%UUID%'; var r=[];")
            if (log.debugEnabled) {
                jsbuff << "ejsLog('/tmp/mapred.log', 'reduce input: '+JSON.stringify(reduced));"
            }
            projectionList.projectionList.each { proj ->
                if (proj instanceof Query.CountProjection) {
                    // Since I'm not emitting a [1] from the map function, I have to interrogate
                    // every input element to see whether it's a count from a previous reduce call
                    // or whether it's unprocessed data coming from a map function. The input array
                    // contains both all mixed up. Arg! :/
                    jsbuff << "var count=0; if(typeof reduced['vclock'] != 'undefined'){return [1];} for(i in reduced){ if(typeof reduced[i] === 'object'){ count += 1; } else { count += reduced[i]; }} r.push(count);"
                } else if (proj instanceof Query.AvgProjection) {
                    // I don't actually average anything until I get ready to return the value below.
                    jsbuff << "var total=0.0; var count=0; for(i in reduced) { if(typeof reduced[i]['age'] !== 'undefined') { count += 1; total += parseFloat(reduced[i].age); } else { total += reduced[i].total; count += reduced[i].count; } } r.push({total: total, count: count});"
                } else if (proj instanceof Query.MaxProjection) {
                    // Find the max for this step, keeping in mind the real max won't be found until
                    // the end of the processing, right before the return below.
                    jsbuff << "var max = false; for(i in reduced){ var d; if(typeof reduced[i] === 'object'){ d = parseFloat(reduced[i].${proj.propertyName}); }else{ d = reduced[i]; } if(!max || d > max){ max = d; }} if(!max === false){ r.push(max); }"
                } else if (proj instanceof Query.MinProjection) {
                    // Find the min for this step, keeping in mind the real min won't be found until
                    // the end of the processing, right before the return below.
                    jsbuff << "var min = false; for(i in reduced){ var d; if(typeof reduced[i] === 'object'){ d = parseFloat(reduced[i].${proj.propertyName}); }else{ d = reduced[i]; } if(!min || d < min){ min = d; }} if(!min === false){ r.push(min); }"
                } else if (proj instanceof Query.IdProjection) {
                    // This just returns the object ID.
                    jsbuff << "for(i in reduced){ if(typeof reduced[i] === 'object'){ r.push(parseFloat(reduced[i].id)); }else{ r.push(reduced[i]); }}"
                } else if (proj instanceof Query.PropertyProjection) {
                    // Property projections are handled below.
                    propProjs << proj
                    jsbuff << "r = reduced;"
                }
            }
            if (log.debugEnabled) {
                jsbuff << "ejsLog('/tmp/mapred.log', 'reduce return: '+JSON.stringify(r));"
            }
            jsbuff << " return r;}"
            reduceJs = jsbuff.toString()
        }

        MapReduceJob mr = new RiakMapReduceJob(riak)
        JavascriptMapReduceOperation mapOper = new JavascriptMapReduceOperation(mapJs.toString())
        MapReducePhase mapPhase = new RiakMapReducePhase(MapReducePhase.Phase.MAP, "javascript", mapOper)
        mr.addPhase(mapPhase)

        JavascriptMapReduceOperation reduceOper
        if (reduceJs) {
            reduceOper = new JavascriptMapReduceOperation(reduceJs)
            MapReducePhase reducePhase = new RiakMapReducePhase(MapReducePhase.Phase.REDUCE, "javascript", reduceOper)
            mr.addPhase(reducePhase)
        }

        // For sure process the bucket of the entity I'm working with...
        def inputBuckets = [entity.name]
        // Check for any descendants I also need to be aware of...
        if (riak.containsKey(entity.name + ".metadata", "descendants")) {
            def descendants = riak.getAsType(entity.name + ".metadata", "descendants", Set)
            if (descendants) {
                // ...and run this M/R against the buckets of any descendants I might have.
                inputBuckets.addAll(descendants)
            }
        }

        def results = []
        inputBuckets.each {
            mr.getInputs().clear()
            mr.getInputs().add(it)
            // For busting internal caching
            def uuid = UUID.randomUUID().toString()
            if (mapOper) {
                def js = mapOper.source.replaceAll("%UUID%", uuid)
                mapOper.source = js
            }
            if (reduceOper) {
                def js = reduceOper.source.replaceAll("%UUID%", uuid)
                reduceOper.source = js
            }

            if (log.debugEnabled) {
                log.debug("Running M/R: \n${mr.toJson()}")
            }
            def l = riak.execute(mr)
            if (l) {
                results.addAll(l)
            }
        }
        // This portion shamelessly absconded from Graeme's RedisQuery.java
        if (results) {
            if (log.debugEnabled) {
                log.debug("Got results: \n" + results)
            }
            final total = results.size()
            if (offset > total) {
                return Collections.emptyList()
            }
            def max = this.max // 20
            def from = offset // 10
            def to = max == -1 ? -1 : (offset + max) - 1            // 15
            if (to >= total) {
                to = -1
            }
            def finalResult = results[from..to]
            if (orderBy) {
                orderBy.each { Query.Order order ->
                    def sorted = finalResult.sort {
                        it."${order.property}"
                    }
                    final def os = order.direction.toString()
                    finalResult = os == "DESC" ? sorted.reverse() : sorted
                }
            }

            if (projectionList.projections) {
                // I don't really like checking for projections again...
                if (propProjs) {
                    // Pull out the property needed here.
                    if (propProjs.size() != 1) {
                        // Is this necessary?
                        log.warn "Only the first PropertyProjection is used: " + propProjs[0]
                    }
                    String propName = propProjs[0].propertyName
                    finalResult.collect { entry ->
                        try {
                            // Try to return the object as the right data type.
                            entry."${propName}".asType(entity.getPropertyByName(propName).type)
                        } catch (GroovyCastException e) {
                            // If I can't do that...
                            if (entry."${propName}".isLong()) {
                                // Maybe I have an object ID? Try to look it up.
                                getSession().retrieve(entity.getPropertyByName(propName).type, entry."${propName}".toLong())
                            } else {
                                // Otherwise, just return it, as I'm guessing this will be fine.
                                entry."${propName}"
                            }
                        }
                    }
                } else {
                    // Use the built-in Groovy functions to operate on the List returned
                    // from the Map/Reduce step.
                    def projResult = projectionList.projectionList.collect { proj ->
                        if (proj instanceof Query.CountProjection) {
                            return finalResult.sum()
                        }
                        if (proj instanceof Query.AvgProjection) {
                            return finalResult.collect { it.total / it.count }
                        }
                        if (proj instanceof Query.MaxProjection) {
                            return finalResult.max()
                        }
                        if (proj instanceof Query.MinProjection) {
                            return finalResult.min()
                        }
                        return finalResult
                    }
                    // I might have a List of Lists because of the way I'm processing the
                    // projections and descendants. If so, I need a flat list.
                    if (projResult && projResult.size() == 1 && projResult.get(0) instanceof List) {
                        return projResult.get(0)
                    }
                    return projResult ?: finalResult
                }
            } else {
                // I've got a list of object IDs. Go get all of them...
                getSession().retrieveAll(getEntity().getJavaClass(), finalResult.collect { it.id?.toLong() })
            }
        } else {
            return Collections.emptyList()
        }
    }

    static handleJunction(Query.Junction junc, PersistentEntity entity) {
        def buff = []
        junc.criteria.each { crit ->
            def handler = queryHandlers[crit.class]
            if (handler) {
                handler(crit, entity, buff)
            }
        }
        buff
    }

    static checkForDate(val) {
        if (val instanceof Date) {
            ((Date) val).time
        } else if (val instanceof Calendar) {
            ((Calendar) val).time.time
        } else {
            val
        }
    }
}
