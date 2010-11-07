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
          buff << "(entry.${equals.name} == \"${equals.value}\")"
        },
        (Query.NotEquals): { Query.Equals notEquals, PersistentEntity entity, buff ->
          buff << "(entry.${notEquals.name} != \"${notEquals.value}\")"
        },
        (Query.GreaterThan): { Query.GreaterThan gt, PersistentEntity entity, buff ->
          buff << "(entry.${gt.name} > ${gt.value})"
        },
        (Query.GreaterThanEquals): { Query.GreaterThanEquals gte, PersistentEntity entity, buff ->
          buff << "(entry.${gte.name} >= ${gte.value})"
        },
        (Query.LessThan): { Query.LessThan lt, PersistentEntity entity, buff ->
          buff << "(entry.${lt.name} < ${lt.value})"
        },
        (Query.LessThanEquals): { Query.LessThanEquals lte, PersistentEntity entity, buff ->
          buff << "(entry.${lte.name} <= ${lte.value})"
        },
        (Query.Between): { Query.Between between, PersistentEntity entity, buff ->
          buff << "((entry.${between.property} < ${between.to})&&(entry.${between.property} > ${between.from}))"
        },
        (Query.Like): { Query.Like like, PersistentEntity entity, buff ->
          def regexp = like.value.toString().replaceAll("%", "(.*)")
          buff << "(\"\"+entry.${like.name}.match(/$regexp/))"
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
    if (criteria.empty) {
      return session.retrieveAll(entity.javaClass, new RiakEntityIndex(riak, entity.name))
    }

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
    def ifclause = buff.join("&&") ?: "true"
    def mapJs = "function(v){var o=Riak.mapValuesJson(v);var r=[];for(i in o){var entry=o[i];if(${ifclause}){r.push(v.key);}} return r;}"
    if (mapJs && log.debugEnabled) {
      log.debug "Map: ${mapJs}"
    }

    def reduceJs
    def projectionList = projections()
    if (projectionList) {
      StringBuilder jsbuff = new StringBuilder("function(v){var r=[];")
      projectionList.projectionList.each { proj ->
        if (proj instanceof Query.CountProjection) {
          jsbuff << "r.push(v.length);"
        }
      }
      jsbuff << " return r;}"
      reduceJs = jsbuff.toString()
    }

    def results = reduceJs ? riak.mapReduce(entity.name, mapJs, reduceJs) : session.retrieveAll(entity.javaClass, riak.query(entity.name, mapJs))
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
          def sorted = finalResult.sort { it."${order.property}"}
          final def os = order.direction.toString()
          finalResult = os == "DESC" ? sorted.reverse() : sorted
        }
      }
      return finalResult
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
