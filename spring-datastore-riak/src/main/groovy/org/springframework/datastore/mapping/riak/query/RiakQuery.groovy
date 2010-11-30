package org.springframework.datastore.mapping.riak.query

import org.codehaus.groovy.runtime.typehandling.GroovyCastException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.riak.core.RiakTemplate
import org.springframework.datastore.mapping.core.Session
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.query.Query
import org.springframework.data.riak.mapreduce.*

/**
 * @author Jon Brisbin <jon.brisbin@npcinternational.com>
 */
class RiakQuery extends Query {

  static Logger log = LoggerFactory.getLogger(RiakQuery)
  static def queryHandlers
  static {
    queryHandlers = [
        (Query.Equals): { Query.Equals equals, PersistentEntity entity, buff ->
          def val = checkForDate(equals.value)
          if (val instanceof Boolean) {
            buff << "Boolean(entry.${equals.name}) == ${val}"
          } else if (val instanceof Integer || val instanceof Double || val instanceof Long) {
            if ("id" == equals.name) {
              buff << "v.key === \"${val}\""
            } else {
              buff << "entry.${equals.name} == ${val}"
            }
          } else {
            buff << "entry.${equals.name} === \"${val}\""
          }
        },
        (Query.IdEquals): { Query.IdEquals idEquals, PersistentEntity entity, buff ->
          buff << "v.key === \"${idEquals.value}\""
        },
        (Query.NotEquals): { Query.NotEquals notEquals, PersistentEntity entity, buff ->
          def val = checkForDate(notEquals.value)
          if (val instanceof Boolean) {
            buff << "Boolean(entry.${notEquals.name}) != ${val}"
          } else if (val instanceof Integer || val instanceof Double || val instanceof Long) {
            buff << "entry.${notEquals.name} != ${val}"
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

  def RiakQuery(Session session, PersistentEntity ent, RiakTemplate riak) {
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
    def joinStr = (criteria && criteria instanceof Query.Disjunction ? "||" : "&&")
    def ifclause = buff.join(joinStr) ?: "true"
    StringBuilder mapJs = new StringBuilder("function(v){")
    if (log.debugEnabled) {
      mapJs << "ejsLog('/tmp/mapred.log', 'map input: '+JSON.stringify(v));"
    }
    if (ifclause != "true") {
      mapJs << "try{if(typeof v['values'] === \"undefined\" || v.values[0].data === \"\"){return [];}}catch(e){return [];};var o=Riak.mapValuesJson(v);var r=[];for(i in o){var entry=o[i];"
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
      mapJs << " var row = Riak.mapValuesJson(v); row[0].id = v.key; return row; }"
    }
    def reduceJs
    // Property projections
    List<Query.PropertyProjection> propProjs = new ArrayList<Query.PropertyProjection>()
    def projectionList = projections()
    if (projectionList.projections) {
      StringBuilder jsbuff = new StringBuilder("function(reduced){ var r=[];")
      if (log.debugEnabled) {
        jsbuff << "ejsLog('/tmp/mapred.log', 'reduce input: '+JSON.stringify(reduced));"
      }
      projectionList.projectionList.each { proj ->
        if (proj instanceof Query.CountProjection) {
          jsbuff << "var count=0; for(i in reduced){ if(typeof reduced[i] === 'object'){ count += 1; } else { count += reduced[i]; }} r.push(count);"
        } else if (proj instanceof Query.AvgProjection) {
          jsbuff << "var total=0.0; var count=0; for(i in reduced) { if(typeof reduced[i]['age'] !== 'undefined') { count += 1; total += parseFloat(reduced[i].age); } else { total += reduced[i].total; count += reduced[i].count; } } r.push({total: total, count: count});"
        } else if (proj instanceof Query.MaxProjection) {
          jsbuff << "var max = false; for(i in reduced){ var d = parseFloat(reduced[i].${proj.propertyName}); if(!max || d > max){ max = d; }} if(!max === false){ r.push(max); }"
        } else if (proj instanceof Query.MinProjection) {
          jsbuff << "var min = false; for(i in reduced){ var d = parseFloat(reduced[i].${proj.propertyName}); if(!min || d < min){ min = d; }} if(!min === false){ r.push(min); }"
        } else if (proj instanceof Query.IdProjection) {
          jsbuff << "for(i in reduced){ if(typeof reduced[i] === 'object'){ r.push(parseFloat(reduced[i].id)); }else{ r.push(reduced[i]); }}"
        } else if (proj instanceof Query.PropertyProjection) {
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

    MapReduceJob mr = riak.createMapReduceJob()
    MapReduceOperation mapOper = new JavascriptMapReduceOperation(mapJs.toString())
    MapReducePhase mapPhase = new RiakMapReducePhase(MapReducePhase.Phase.MAP, "javascript", mapOper)
    mr.addPhase(mapPhase)

    if (reduceJs) {
      MapReduceOperation reduceOper = new JavascriptMapReduceOperation(reduceJs)
      MapReducePhase reducePhase = new RiakMapReducePhase(MapReducePhase.Phase.REDUCE, "javascript", reduceOper)
      mr.addPhase(reducePhase)
    }

    def inputBuckets = [entity.name]
    def descendants = riak.getAsType(entity.name + ".metadata:descendants", List)
    if (descendants) {
      inputBuckets.addAll(descendants)
    }
    def results = []
    inputBuckets.each {
      mr.getInputs().clear()
      mr.getInputs().add(it)
      if (log.debugEnabled) {
        log.debug("Running M/R: \n${mr.toJson()}")
      }
      def l = riak.execute(mr)
      if (l && l.size() > 0) {
        results.addAll(l)
      }
    }
    // This portion shamelessly absconded from Graeme's RedisQuery.java
    if (results) {
      final def total = results.size()
      if (offset > total) {
        return Collections.emptyList()
      }
      def max = this.max // 20
      def from = offset // 10
      def to = max == -1 ? -1 : (offset + max) - 1      // 15
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
        if (propProjs) {
          if (propProjs.size() != 1) {
            log.warn "Only the first PropertyProjection is used: " + propProjs[0]
          }
          String propName = propProjs[0].propertyName
          finalResult.collect { entry ->
            try {
              entry."${propName}".asType(entity.getPropertyByName(propName).type)
            } catch (GroovyCastException e) {
              if (entry."${propName}".isLong()) {
                getSession().retrieve(entity.getPropertyByName(propName).type, entry."${propName}".toLong())
              } else {
                entry."${propName}"
              }
            }
          }
        } else {
          def projResult = projectionList.projectionList.collect { proj ->
            if (proj instanceof Query.CountProjection) {
              return finalResult.sum()
            } else if (proj instanceof Query.AvgProjection) {
              return finalResult.collect { it.total / it.count }
            } else if (proj instanceof Query.MaxProjection) {
              return finalResult.max()
            } else if (proj instanceof Query.MinProjection) {
              return finalResult.min()
            } else {
              return finalResult
            }
          }
          if (projResult && projResult.size() == 1 && projResult.get(0) instanceof List) {
            return projResult.get(0)
          } else {
            return projResult ?: finalResult
          }
        }
      } else {
        getSession().retrieveAll(getEntity().getJavaClass(), finalResult.collect { it.id?.toLong() })
      }
    } else {
      return Collections.emptyList()
    }
  }

  static def handleJunction(Query.Junction junc, PersistentEntity entity) {
    def buff = []
    junc.criteria.each { crit ->
      def handler = queryHandlers[crit.class]
      if (handler) {
        handler(crit, entity, buff)
      }
    }
    buff
  }

  static def checkForDate(val) {
    if (val instanceof Date) {
      ((Date) val).time
    } else if (val instanceof Calendar) {
      ((Calendar) val).time.time
    } else {
      val
    }
  }

}
