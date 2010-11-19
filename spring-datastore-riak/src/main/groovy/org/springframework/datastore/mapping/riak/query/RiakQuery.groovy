package org.springframework.datastore.mapping.riak.query

import org.codehaus.groovy.runtime.typehandling.GroovyCastException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.datastore.mapping.core.Session
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.query.Query
import org.springframework.datastore.riak.core.RiakTemplate
import org.springframework.datastore.riak.mapreduce.*

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
              buff << "v.key == \"${val}\""
            } else {
              buff << "entry.${equals.name} == ${val}"
            }
          } else {
            buff << "entry.${equals.name} == \"${val}\""
          }
        },
        (Query.IdEquals): { Query.IdEquals idEquals, PersistentEntity entity, buff ->
          buff << "v.key == \"${idEquals.value}\""
        },
        (Query.NotEquals): { Query.NotEquals notEquals, PersistentEntity entity, buff ->
          def val = checkForDate(notEquals.value)
          if (val instanceof Boolean) {
            buff << "Boolean(entry.${notEquals.name}) != ${val}"
          } else if (val instanceof Integer || val instanceof Double || val instanceof Long) {
            buff << "entry.${notEquals.name} != ${val}"
          } else {
            buff << "entry.${notEquals.name} != \"${val}\""
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
            "(entry.${qin.name} == " + (it.isInteger() ? it : "\"$it\"") + ")"
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
    StringBuilder mapJs = new StringBuilder("function(v){try{if(typeof v['values'] == \"undefined\" || v.values[0].data == \"\"){return [];}}catch(e){return [];};var o=Riak.mapValuesJson(v);var r=[];for(i in o){var entry=o[i];")
    if (ifclause != "true") {
      mapJs << "if(${ifclause}){"
    }
    mapJs << "o[i].id=v.key;r.push(o[i]);"
    if (ifclause != "true") {
      mapJs << "}"
    }
    mapJs << "} return r;}"
    def reduceJs
    // Property projections
    List<Query.PropertyProjection> propProjs = new ArrayList<Query.PropertyProjection>()
    def projectionList = projections()
    if (projectionList.projections) {
      StringBuilder jsbuff = new StringBuilder("function(reduced){ var r=[];")
      projectionList.projectionList.each { proj ->
        if (proj instanceof Query.CountProjection) {
          jsbuff << "r.push(reduced.length);"
        } else if (proj instanceof Query.AvgProjection) {
          jsbuff << "if(reduced.length>0){ var total=0.0;for(i in reduced){ total += 1*(reduced[i].${proj.propertyName}); } r.push(total/reduced.length); } else { r.push(0.0); }"
        } else if (proj instanceof Query.MaxProjection) {
          jsbuff << "if(reduced.length>0){ var max=1*reduced[0].${proj.propertyName};for(i in reduced){ if(i>0){ if(1*(reduced[i].${proj.propertyName}) > max){ max=1*(reduced[i].${proj.propertyName}); }}} r.push(max);}"
        } else if (proj instanceof Query.MinProjection) {
          jsbuff << "if(reduced.length>0){ var min=1*reduced[0].${proj.propertyName};for(i in reduced){ if(i>0){ if(1*(reduced[i].${proj.propertyName}) < min){ min=1*(reduced[i].${proj.propertyName}); }}} r.push(min);}"
        } else if (proj instanceof Query.IdProjection) {
          jsbuff << "if(reduced.length>0){ for(i in reduced){ r.push(1*(reduced[i].id)); }}"
        } else if (proj instanceof Query.PropertyProjection) {
          propProjs << proj
          jsbuff << "return reduced;"
        }
      }
      jsbuff << " return r;}"
      reduceJs = jsbuff.toString()
    }

    MapReduceJob mr = riak.createMapReduceJob()
    mr.addInputs([entity.name])
    MapReduceOperation mapOper = new JavascriptMapReduceOperation(mapJs.toString())
    MapReducePhase mapPhase = new RiakMapReducePhase(MapReducePhase.Phase.MAP, "javascript", mapOper)
    mr.addPhase(mapPhase)

    if (reduceJs) {
      MapReduceOperation reduceOper = new JavascriptMapReduceOperation(reduceJs)
      MapReducePhase reducePhase = new RiakMapReducePhase(MapReducePhase.Phase.REDUCE, "javascript", reduceOper)
      mr.addPhase(reducePhase)
    }

    log.debug("Running M/R: \n${mr.toJson()}")
    def results = riak.execute(mr)
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
          finalResult
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
