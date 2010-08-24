/* Copyright (C) 2010 SpringSource
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
package org.springframework.datastore.mock.query

import org.springframework.datastore.query.Query
import org.springframework.datastore.mapping.PersistentEntity
import org.springframework.datastore.mock.SimpleMapSession
import org.springframework.datastore.mock.engine.SimpleMapEntityPersister
import org.springframework.dao.InvalidDataAccessResourceUsageException
import org.springframework.datastore.keyvalue.mapping.KeyValue
import org.springframework.datastore.mapping.PersistentProperty

import org.springframework.datastore.mapping.types.ToOne
import org.springframework.datastore.query.Restrictions
import org.springframework.datastore.engine.EntityPersister
import org.springframework.datastore.engine.EntityAccess

/**
 * Simple query implementation that queries a map of objects
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class SimpleMapQuery extends Query{

  Map<String, Map> datastore
  private String family
  private SimpleMapEntityPersister entityPersister

  SimpleMapQuery(SimpleMapSession session, PersistentEntity entity, SimpleMapEntityPersister entityPersister) {
    super(session, entity);
    this.datastore = session.getBackingMap();
    family = getFamily(entity)
    this.entityPersister = entityPersister
  }

  protected List executeQuery(PersistentEntity entity, Query.Junction criteria) {
    def results = []
    def entityMap = [:]
    if(criteria.isEmpty()) {
      entityMap = datastore[family]
    }
    else {
      def criteriaList = criteria.getCriteria()
      entityMap = executeSubQuery(criteria, criteriaList)
    }

    if(projections.isEmpty()) {
      results = entityMap.values() as List
    }
    else {
      def projectionList = projections.projectionList
      def projectionCount = projectionList.size()
      def entityList = entityMap.values()
      
      projectionList.each { Query.Projection p ->

        if(p instanceof Query.IdProjection) {
           if(projectionCount == 1) {
             results = entityMap.keySet()
           }
          else {
             results.add( entityMap.keySet() )
           }
        }
        else if(p instanceof Query.CountProjection) {
            results.add(entityList.size())
        }
        else if(p instanceof Query.PropertyProjection) {
          def propertyValues = entityList.collect { it."$p.propertyName"}
          if(p instanceof Query.MaxProjection) {
            results.add(  propertyValues.max() )
          }
          else if(p instanceof Query.MinProjection) {
            results.add(  propertyValues.min() )
          }
          else if(p instanceof Query.SumProjection) {
            results.add(  propertyValues.sum() )
          }
          else if(p instanceof Query.AvgProjection) {
            def average = propertyValues.sum() / propertyValues.size()
            results.add( average )
          }
          else {

            PersistentProperty prop = entity.getPropertyByName(p.propertyName)
            if(prop) {
              if(prop instanceof ToOne) {
                propertyValues = propertyValues.collect { session.retrieve(prop.type, it)}
              }
              if(projectionCount == 1)
                results.addAll(propertyValues)
              else {
                results.add(propertyValues)
              }

            }
          }
        }
      }
    }
    if(results) {
      final def total = results.size()
      if(offset > total) return Collections.emptyList()

      def overflow = offset + max
      if(overflow >= total) {
          max == total - offset
      }
      def finalResult = results[offset..(max > -1 ? max-1 : max)]
      if(orderBy) {
        orderBy.each { Query.Order order ->
          def sorted = finalResult.sort { it."${order.property}"}
          final def os = order.direction.toString()
          finalResult = os == "DESC" ? sorted.reverse() : sorted  
        }
      }
      return finalResult
    }
    else {
      return Collections.emptyList()
    }

  }

  def handlers = [
        (Query.Equals): { Query.Equals equals, PersistentProperty property ->
          def indexer = entityPersister.getPropertyIndexer(property)
          return indexer.query(equals.value)
        },
        (Query.Like): { Query.Like like, PersistentProperty property ->
          def indexer = entityPersister.getPropertyIndexer(property)

          def root = indexer.indexRoot
          def regexFormat = like.pattern.replaceAll('%', '.+?')
          def pattern = "${root}:${regexFormat}"
          def matchingIndices = entityPersister.indices.findAll { key, value ->
            key ==~ pattern
          }

          def result = [] as Set

          for( indexed in matchingIndices ) {
            result.addAll(indexed.value)
          }

          return result

        },
        (Query.In): { Query.In inList, PersistentProperty property ->
            def disjunction = new Query.Disjunction()
            for(value in inList.values) {
              disjunction.add( Restrictions.eq(inList.name, value) )
            }

            executeSubQueryInternal(disjunction, disjunction.criteria)
        },
        (Query.Between): { Query.Between between, PersistentProperty property ->
          def from = between.from
          def to = between.to
          def name = between.property
          def allEntities = datastore[family]

          allEntities.findAll { it.value[name] >= from && it.value[name] <= to }.collect { it.key }
        },
        (Query.GreaterThan): { Query.GreaterThan gt, PersistentProperty property ->
          def name = gt.property
          def value = gt.value
          def allEntities = datastore[family]

          allEntities.findAll { it.value[name] > value }.collect { it.key }
        },
        (Query.GreaterThanEquals): { Query.GreaterThanEquals gt, PersistentProperty property ->
          def name = gt.property
          def value = gt.value
          def allEntities = datastore[family]

          allEntities.findAll { it.value[name] >= value }.collect { it.key }
        },
        (Query.LessThan): { Query.LessThan lt, PersistentProperty property ->
          def name = lt.property
          def value = lt.value
          def allEntities = datastore[family]

          allEntities.findAll { it.value[name] < value }.collect { it.key }
        },
        (Query.LessThanEquals): { Query.LessThanEquals lte, PersistentProperty property ->
          def name = lte.property
          def value = lte.value
          def allEntities = datastore[family]

          allEntities.findAll { it.value[name] <= value }.collect { it.key }
        }


  ]
  protected Map executeSubQuery(criteria, criteriaList) {

    def finalIdentifiers = executeSubQueryInternal(criteria, criteriaList)

    Map queryResult = [:]
    populateQueryResult(finalIdentifiers, queryResult)
    return queryResult
  }

  protected Collection executeSubQueryInternal(criteria, criteriaList) {
    def resultList = []
    for (Query.Criterion criterion in criteriaList) {
      if(criterion instanceof Query.Junction) {
        resultList << executeSubQueryInternal(criterion, criterion.criteria)
      }
      else {
        PersistentProperty property = getValidProperty(criterion)

        def handler = handlers[criterion.getClass()]
        def results = handler?.call(criterion, property)
        if(results)
          resultList << results
      }

    }
    def finalIdentifiers = []
    if (!resultList.isEmpty()) {
      if (resultList.size() > 1) {
        if (criteria instanceof Query.Conjunction) {
          def total = resultList.size()
          finalIdentifiers = resultList[0]
          for (num in 1..<total) {
            def secondList = resultList[num]
            if (secondList)
              finalIdentifiers = finalIdentifiers.intersect(secondList)
          }
        }
        else {
          finalIdentifiers = resultList.flatten()
        }
      }
      else {
        finalIdentifiers = resultList[0]

      }
    }
    return finalIdentifiers
  }

  protected PersistentProperty getValidProperty(criterion) {
    def property = entity.getPropertyByName(criterion.property)
    if (property == null) {
      throw new InvalidDataAccessResourceUsageException("Cannot query [" + entity + "] on non-existent property: " + property);
    }
    else if (!isIndexed(property)) {
      throw new InvalidDataAccessResourceUsageException("Cannot query [" + entity + "] on non-indexed property: " + property);
    }
    return property
  }

  private boolean isIndexed(PersistentProperty property) {
      KeyValue kv = (KeyValue) property.getMapping().getMappedForm();
      return kv.isIndex();
  }


  protected populateQueryResult(identifiers, Map queryResult) {
    for (id in identifiers) {
      def map = datastore[family].get(id)
      if (map) {
        for( PersistentProperty p in entity.persistentProperties) {
          if(p instanceof ToOne) {
            def associatedId = map[p.name]
            if(!p.type.isInstance(associatedId)) {
              map[p.name] = session.retrieve(p.type, associatedId)
            }
          }
        }
        def o = entity.javaClass.newInstance(map)
        new EntityAccess(entity, o).setIdentifier(id)
        queryResult.put(id, o)
      }
    }
  }

  protected String getFamily(PersistentEntity entity) {
      def cm = entity.getMapping()
      String table = null;
      if(cm.getMappedForm() != null) {
          table = cm.getMappedForm().getFamily();
      }
      if(table == null) table = entity.getJavaClass().getName();
      return table;
  }


  
}
