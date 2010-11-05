package org.springframework.datastore.mapping.riak.query

import org.springframework.datastore.mapping.core.Session
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.query.Query
import org.springframework.datastore.mapping.riak.collection.RiakEntityIndex
import org.springframework.datastore.mapping.riak.util.RiakTemplate

/**
 * @author Jon Brisbin <jon.brisbin@npcinternational.com>
 */
class RiakQuery extends Query {

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
          def conjunc = RiakQuery.handleJunction(or, entity).join("||")
          buff << "($conjunc)"
        },
        (Query.Negation): { Query.Negation not, PersistentEntity entity, buff ->
          buff << "&& !"
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
      println "trying to handle: ${criterion}"
      def handler = queryHandlers[criterion.class]
      if (handler) {
        handler(criterion, entity, buff)
      } else {
        return []
      }
    }
    def ifclause = buff.join("&&") ?: "true"
    def js = "function(v){var o=Riak.mapValuesJson(v);var r=[];for(i in o){var entry=o[i];if(${ifclause}){ejsLog('/tmp/mapred.log','pushing entry: '+JSON.stringify(entry));r.push(v.key);}} ejsLog('/tmp/mapred.log','returning: '+JSON.stringify(r)); return r;}"
    println "js: ${js}"

    riak.query(entity.name, js)
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

class Condition {

  String leftSide
  String rightSide
  String operator
  String var = "entry"

  String toString() {
    "($var.$leftSide $operator $rightSide)"
  }

}
