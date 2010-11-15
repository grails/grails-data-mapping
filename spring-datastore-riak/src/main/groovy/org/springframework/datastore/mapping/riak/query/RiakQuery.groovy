package org.springframework.datastore.mapping.riak.query

import org.antlr.stringtemplate.StringTemplate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.datastore.mapping.core.Session
import org.springframework.datastore.mapping.model.PersistentEntity
import org.springframework.datastore.mapping.query.Query
import org.springframework.datastore.mapping.riak.util.RiakTemplate

/**
 * @author Jon Brisbin <jon.brisbin@npcinternational.com>
 */
class RiakQuery extends Query {

  static Logger log = LoggerFactory.getLogger(RiakQuery)
  static def queryHandlers
  static def jsTemplates
  static def ifClauseTemplates
  static def unquotedClasses = [Long, Integer, Double, Date, Calendar, BigDecimal, BigInteger, Boolean]
  static {
    jsTemplates = [
        eq: new StringTemplate()
    ]
    ifClauseTemplates = [
        eq: "(entry.%s == %s)",
        ne: "(entry.%s != %s)",
        gt: "(entry.%s > %s)",
        lt: "(entry.%s < %s)",
        gte: "(entry.%s >= %s)",
        lte: "(entry.%s <= %s)",
        between: "(entry.%s <= %s) && (entry.%s >= %s)",
        like: "/^%s/.test(\"\"+entry.%s)"
    ]
    queryHandlers = [
        (Query.Equals): { Query.Equals equals, PersistentEntity entity, buff ->
          buff << String.format(ifClauseTemplates.eq, equals.name, getQuotedValue(equals.value))
        },
        (Query.IdEquals): { Query.IdEquals idEquals, PersistentEntity entity, buff ->
          buff << "v.key == ${idEquals.value}"
        },
        (Query.NotEquals): { Query.Equals notEquals, PersistentEntity entity, buff ->
          buff << String.format(ifClauseTemplates.ne, notEquals.name, getQuotedValue(notEquals.value))
        },
        (Query.GreaterThan): { Query.GreaterThan gt, PersistentEntity entity, buff ->
          buff << String.format(ifClauseTemplates.gt, gt.name, getQuotedValue(gt.value))
        },
        (Query.GreaterThanEquals): { Query.GreaterThanEquals gte, PersistentEntity entity, buff ->
          buff << String.format(ifClauseTemplates.gte, gte.name, getQuotedValue(gte.value))
        },
        (Query.LessThan): { Query.LessThan lt, PersistentEntity entity, buff ->
          buff << String.format(ifClauseTemplates.lt, lt.name, getQuotedValue(lt.value))
        },
        (Query.LessThanEquals): { Query.LessThanEquals lte, PersistentEntity entity, buff ->
          buff << String.format(ifClauseTemplates.lte, lte.name, getQuotedValue(lte.value))
        },
        (Query.Between): { Query.Between between, PersistentEntity entity, buff ->
          buff << String.format(ifClauseTemplates.between, between.property, getQuotedValue(between.to), between.property, getQuotedValue(between.from))
        },
        (Query.Like): { Query.Like like, PersistentEntity entity, buff ->
          def regexp = like.value.toString().replaceAll("%", "(.+)")
          buff << String.format(ifClauseTemplates.like, regexp, like.name)
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
            String.format(ifClauseTemplates.eq, qin.name, getQuotedValue(it))
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
    def ifclause = buff.join(joinStr) ?: "1 == 1"
    def mapJs = "function(v){ejsLog('/tmp/mapred.log','v='+JSON.stringify(v));var o=Riak.mapValuesJson(v);var r=[];for(i in o){var entry=o[i];if(${ifclause}){if(typeof v['push'] == 'function'){o[i].id=entry.id;}else{o[i].id=v.key;};r.push(o[i]);}} return r;}"
    boolean hasReduce = false
    def reduceJs
    // Property projections
    List<Query.PropertyProjection> propProjs = new ArrayList<Query.PropertyProjection>()
    def projectionList = projections()
    if (projectionList.projections) {
      StringBuilder jsbuff = new StringBuilder("function(values){ var r=[];")
      projectionList.projectionList.each { proj ->
        if (proj instanceof Query.CountProjection) {
          jsbuff << "r.push(values.length);"
          hasReduce = true
        } else if (proj instanceof Query.AvgProjection) {
          jsbuff << "if(values.length>0){ var total=0.0;for(i in values){ total += 1*values[i].${proj.propertyName}; } r.push(total/values.length); } else { r.push(0.0); }"
          hasReduce = true
        } else if (proj instanceof Query.MaxProjection) {
          jsbuff << "if(values.length>0){ var max=1*values[0].${proj.propertyName};for(i in values){ if(i>0){ if(1*values[i].${proj.propertyName} > max){ max=1*values[i].${proj.propertyName}; }}} r.push(max);}"
          hasReduce = true
        } else if (proj instanceof Query.MinProjection) {
          jsbuff << "if(values.length>0){ var min=1*values[0].${proj.propertyName};for(i in values){ if(i>0){ if(1*values[i].${proj.propertyName} < min){ min=1*values[i].${proj.propertyName}; }}} r.push(min);}"
          hasReduce = true
        } else if (proj instanceof Query.IdProjection) {
          jsbuff << "if(values.length>0){ for(i in values){ r.push(1*values[i].id); }}"
          hasReduce = true
        } else if (proj instanceof Query.PropertyProjection) {
          propProjs << proj
        }
      }
      jsbuff << " return r;}"
      reduceJs = jsbuff.toString()
    }

    def results = riak.mapReduce(entity.name, "function(data, keyData, args){ log(arguments); return []; }", (hasReduce ? reduceJs : null))
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
            getSession().retrieve(getEntity().getJavaClass(), entry.id)?."${propProjs[0].propertyName}"
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

  static def getQuotedValue(obj) {
    if (unquotedClasses.contains(obj.class)) {
      if (obj instanceof Date) {
        obj.time
      } else if (obj instanceof Calendar) {
        obj.time.time
      } else {
        obj
      }
    } else {
      "\"${obj}\""
    }
  }
}
