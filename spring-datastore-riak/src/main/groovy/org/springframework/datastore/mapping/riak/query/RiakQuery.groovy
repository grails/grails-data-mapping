package org.springframework.datastore.mapping.riak.query

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.datastore.mapping.core.Session
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.query.Query
import org.springframework.datastore.mapping.riak.collection.RiakEntityIndex
import org.springframework.datastore.mapping.riak.util.RiakTemplate

/**
 * @author Jon Brisbin <jon.brisbin@npcinternational.com>
 */
class RiakQuery extends Query {

  static Logger log = LoggerFactory.getLogger(RiakQuery)
  static def queryHandlers
  static {
    queryHandlers = [
        (Query.Equals): { Query.Equals equals, PersistentEntity entity, buff ->
          def val = equals.value.asType(entity.getPropertyByName(equals.name).getType())
          if (val instanceof Boolean || val instanceof Integer || val instanceof Double) {
            buff << "(entry.${equals.name} == ${val})"
          } else {
            buff << "(entry.${equals.name} == \"${val}\")"
          }
        },
        (Query.IdEquals): { Query.IdEquals idEquals, PersistentEntity entity, buff ->
          buff << "v.key == ${idEquals.value}"
        },
        (Query.NotEquals): { Query.Equals notEquals, PersistentEntity entity, buff ->
          def val = notEquals.value.asType(entity.getPropertyByName(notEquals.name).getType())
          if (val instanceof Boolean || val instanceof Integer || val instanceof Double) {
            buff << "(entry.${notEquals.name} != ${val})"
          } else {
            buff << "(entry.${notEquals.name} != \"${val}\")"
          }
        },
        (Query.GreaterThan): { Query.GreaterThan gt, PersistentEntity entity, buff ->
          def val = gt.value.asType(entity.getPropertyByName(gt.name).getType())
          buff << "(entry.${gt.name} > ${val})"
        },
        (Query.GreaterThanEquals): { Query.GreaterThanEquals gte, PersistentEntity entity, buff ->
          def val = gte.value.asType(entity.getPropertyByName(gte.name).getType())
          buff << "(entry.${gte.name} >= ${val})"
        },
        (Query.LessThan): { Query.LessThan lt, PersistentEntity entity, buff ->
          def val = lt.value.asType(entity.getPropertyByName(lt.name).getType())
          buff << "(entry.${lt.name} < ${val})"
        },
        (Query.LessThanEquals): { Query.LessThanEquals lte, PersistentEntity entity, buff ->
          def val = lte.value.asType(entity.getPropertyByName(lte.name).getType())
          buff << "(entry.${lte.name} <= ${val})"
        },
        (Query.Between): { Query.Between between, PersistentEntity entity, buff ->
          def to = between.to.value.asType(entity.getPropertyByName(between.property).getType())
          def from = between.from.value.asType(entity.getPropertyByName(between.property).getType())
          buff << "(entry.${between.property} <= ${to}) && (entry.${between.property} >= ${from})"
        },
        (Query.Like): { Query.Like like, PersistentEntity entity, buff ->
          def regexp = like.value.toString().replaceAll("%", "(.+)")
          buff << "/^$regexp/.test(\"\"+entry.${like.name})"
        },
        (Query.Conjunction): { Query.Conjunction and, PersistentEntity entity, buff ->
          def conjunc = RiakQuery.handleJunction(and, entity).join("&&")
          buff << "($conjunc)"
        },
        (Query.Disjunction): { Query.Disjunction or, PersistentEntity entity, buff ->
          def disjunc = RiakQuery.handleJunction(or, entity).join("||")
          buff << "($disjunc)"
        },
        (Query.Negation): { Query.Negation not, PersistentEntity entity, buff ->
          def neg = RiakQuery.handleJunction(not, entity).join("&&")
          buff << "!($neg)"
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
    def mapJs = "function(v){var o=Riak.mapValuesJson(v);var r=[];for(i in o){var entry=o[i];if(${ifclause}){o[i].id=v.key;r.push(o[i]);}} return r;}"
    def reduceJs
    // Property projections
    List<Query.PropertyProjection> propProjs = new ArrayList<Query.PropertyProjection>()
    def projectionList = projections()
    if (projectionList.projections) {
      StringBuilder jsbuff = new StringBuilder("function(v){ var r=[];")
      projectionList.projectionList.each { proj ->
        if (proj instanceof Query.CountProjection) {
          jsbuff << "r.push(v.length);"
        } else if (proj instanceof Query.AvgProjection) {
          jsbuff << "if(v.length>0){ var total=0.0;for(i in v){ total += 1*v[i].${proj.propertyName}; } r.push(total/v.length); } else { r.push(0.0); }"
        } else if (proj instanceof Query.MaxProjection) {
          jsbuff << "if(v.length>0){ var max=1*v[0].${proj.propertyName};for(i in v){ if(i>0){ if(1*v[i].${proj.propertyName} > max){ max=1*v[i].${proj.propertyName}; }}} r.push(max);}"
        } else if (proj instanceof Query.MinProjection) {
          jsbuff << "if(v.length>0){ var min=1*v[0].${proj.propertyName};for(i in v){ if(i>0){ if(1*v[i].${proj.propertyName} < min){ min=1*v[i].${proj.propertyName}; }}} r.push(min);}"
        } else if (proj instanceof Query.IdProjection) {
          jsbuff << "if(v.length>0){ for(i in v){ r.push(1*v[i].id); }}"
        } else if (proj instanceof Query.PropertyProjection) {
          propProjs << proj
          jsbuff << "return v;"
        }
      }
      jsbuff << " return r;}"
      reduceJs = jsbuff.toString()
    }

    def results = riak.mapReduce(entity.name, mapJs, reduceJs)
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
          finalResult.collect { entry ->
            entry."${propProjs[0].propertyName}"
          }
        } else {
          finalResult
        }
      } else {
        getSession().retrieveAll(getEntity().getJavaClass(), finalResult.collect { it.id.toLong() })
      }
    } else {
      return Collections.emptyList()
    }
  }

  static def handleJunction(Query.Junction junc, PersistentEntity entity) {
    def conjunc = []
    junc.criteria.each { crit ->
      def handler = queryHandlers[crit.class]
      if (handler) {
        handler(crit, entity, conjunc)
      }
    }
    conjunc
  }

}
