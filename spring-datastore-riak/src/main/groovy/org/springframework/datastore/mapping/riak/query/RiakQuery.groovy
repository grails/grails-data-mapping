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
        (Query.Like): { Query.Like like, PersistentEntity entity, buff ->
          def regexp = like.value.toString().replaceAll("%", "(.*)")
          buff << "(entry.${like.name}.match(/$regexp/))"
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
      return new RiakEntityIndex(riak, entity.name)
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
    def js = "function(v){var o=Riak.mapValuesJson(v);var r=[];for(i in o){var entry=o[i];if(${ifclause}){r.push(v.key);}} return r;}"
    if (log.debugEnabled) {
      log.debug "Map/Reduce: ${js}"
    }

    def keys = riak.query(entity.name, js)
    session.retrieveAll(entity.javaClass, keys)
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
