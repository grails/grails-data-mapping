package org.grails.orm.hibernate

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.proxy.ProxyHandler
import org.grails.datastore.mapping.reflect.ClassUtils
import org.grails.orm.hibernate.cfg.AbstractGrailsDomainBinder
import org.grails.orm.hibernate.cfg.CompositeIdentity
import org.grails.orm.hibernate.exceptions.GrailsQueryException
import org.grails.orm.hibernate.query.GrailsHibernateQueryUtils
import org.grails.orm.hibernate.support.HibernateRuntimeUtils
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.finders.DynamicFinder
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.core.Datastore
import org.hibernate.Criteria
import org.hibernate.FlushMode
import org.hibernate.Query
import org.hibernate.Session
import org.hibernate.criterion.Example
import org.hibernate.criterion.Projections
import org.hibernate.criterion.Restrictions
import org.hibernate.transform.DistinctRootEntityResultTransformer
import org.springframework.core.convert.ConversionService
import org.springframework.transaction.PlatformTransactionManager

import java.util.regex.Pattern

/**
 * Abstract implementation of the Hibernate static API for GORM, providing String-based method implementations
 *
 * @author Graeme Rocher
 * @since 4.0
 */
@CompileStatic
abstract class AbstractHibernateGormStaticApi<D> extends GormStaticApi<D> {

    protected ProxyHandler proxyHandler
    protected Pattern queryPattern
    protected IHibernateTemplate hibernateTemplate
    protected ConversionService conversionService

    AbstractHibernateGormStaticApi(Class<D> persistentClass, Datastore datastore, List<FinderMethod> finders, IHibernateTemplate hibernateTemplate) {
        this(persistentClass, datastore, finders, null, hibernateTemplate)
    }

    AbstractHibernateGormStaticApi(Class<D> persistentClass, Datastore datastore, List<FinderMethod> finders, PlatformTransactionManager transactionManager, IHibernateTemplate hibernateTemplate) {
        super(persistentClass, datastore, finders, transactionManager)
        this.hibernateTemplate = hibernateTemplate
        this.queryPattern = ~/(?i)\s*from(?-i)\s+[${persistentEntity.name}|${persistentEntity.javaClass.simpleName}].*/
        this.conversionService = datastore.mappingContext.conversionService
        this.proxyHandler = datastore.mappingContext.proxyHandler
    }

    @Override
    D get(Serializable id) {
        if (id || (id instanceof Number)) {
            id = convertIdentifier(id)
            if(id != null) {
                D result = (D)hibernateTemplate.get((Class)persistentClass, id)
                return (D)proxyHandler.unwrap(result)
            }
            else {
                return null
            }
        }
    }

    @Override
    D read(Serializable id) {
        if (id == null) {
            return null
        }

        (D)hibernateTemplate.execute(  { Session session ->

            def criteria = session.createCriteria(persistentEntity.javaClass)
            criteria.add Restrictions.idEq(convertIdentifier(id))
            criteria.readOnly = true
            def result = (D) criteria.uniqueResult()
            if(result)
                session.setReadOnly(result, true)
            return proxyHandler.unwrap( result )
        } )
    }

    @Override
    D load(Serializable id) {
        id = convertIdentifier(id)
        if (id != null) {
            return (D) hibernateTemplate.load((Class)persistentClass, id)
        }
    }

    @Override
    List<D> getAll() {
        (List<D>)hibernateTemplate.execute({ Session session ->
            Criteria criteria = session.createCriteria(persistentClass)
            hibernateTemplate.applySettings(criteria)
            criteria.list()
        })
    }

    @Override
    Integer count() {
        (Integer)hibernateTemplate.execute({ Session session ->
            def criteria = session.createCriteria(persistentClass)
            hibernateTemplate.applySettings(criteria)
            criteria.setProjection(Projections.rowCount())
            def num = criteria.uniqueResult()
            num == null ? 0 : num
        })
    }

    @Override
    boolean exists(Serializable id) {
        id = convertIdentifier(id)
        hibernateTemplate.execute  { Session session ->
            Criteria criteria = session.createCriteria(persistentEntity.javaClass)
            hibernateTemplate.applySettings(criteria)
            criteria.add(Restrictions.idEq(id))
                    .setProjection(Projections.rowCount())
                    .uniqueResult()
        }
    }

    D first(Map m) {
        def entityMapping = AbstractGrailsDomainBinder.getMapping(persistentEntity.javaClass)
        if (entityMapping?.identity instanceof CompositeIdentity) {
            throw new UnsupportedOperationException('The first() method is not supported for domain classes that have composite keys.')
        }
        super.first(m)
    }

    D last(Map m) {
        def entityMapping = AbstractGrailsDomainBinder.getMapping(persistentEntity.javaClass)
        if (entityMapping?.identity instanceof CompositeIdentity) {
            throw new UnsupportedOperationException('The last() method is not supported for domain classes that have composite keys.')
        }
        super.last(m)
    }

    /**
     * Implements the 'find(String' method to use HQL queries with named arguments
     *
     * @param query The query
     * @param queryNamedArgs The named arguments
     * @param args Any additional query arguments
     * @return A result or null if no result found
     */
    @Override
    D find(String query, Map queryNamedArgs, Map args) {
        query = normalizeMultiLineQueryString(query)
        if (!queryPattern.matcher(query).matches()) {
            throw new GrailsQueryException("Invalid query [$query] for domain class [$persistentEntity.name]");
        }

        def template = hibernateTemplate
        queryNamedArgs = new HashMap(queryNamedArgs)
        args = new HashMap(args)
        return (D) template.execute { Session session ->
            def q = session.createQuery(query)
            template.applySettings(q)

            populateQueryArguments(q, queryNamedArgs)
            populateQueryArguments(q, args)
            populateQueryWithNamedArguments(q, queryNamedArgs)
            return executeHibernateQuery(q, args)
        }
    }

    @Override
    D find(String query, Collection params, Map args) {
        query = normalizeMultiLineQueryString(query)
        if (!queryPattern.matcher(query).matches()) {
            throw new GrailsQueryException("Invalid query [$query] for domain class [$persistentEntity.name]");
        }

        args = new HashMap(args)
        def template = hibernateTemplate
        return (D) template.execute { Session session ->
            def q = session.createQuery(query)
            template.applySettings(q)

            params.eachWithIndex { val, int i ->
                if (val instanceof CharSequence) {
                    q.setParameter i, val.toString()
                }
                else {
                    q.setParameter i, val
                }
            }
            populateQueryArguments(q, args)
            return executeHibernateQuery(q, args)
        }
    }

    @Override
    List<D> findAll(String query, Map params, Map args) {
        query = normalizeMultiLineQueryString(query)
        if (!queryPattern.matcher(query).matches()) {
            throw new GrailsQueryException("Invalid query [$query] for domain class [$persistentEntity.name]");
        }

        args = new HashMap(args)
        params = new HashMap(params)
        def template = hibernateTemplate
        return (List<D>) template.execute { Session session ->
            def q = session.createQuery(query)
            template.applySettings(q)

            populateQueryArguments(q, params)
            populateQueryArguments(q, args)
            populateQueryWithNamedArguments(q, params)

            return q.list()
        }
    }

    @Override
    List<D> findAll(String query, Collection params, Map args) {
        query = normalizeMultiLineQueryString(query)
        if (!queryPattern.matcher(query).matches()) {
            throw new GrailsQueryException("Invalid query [$query] for domain class [$persistentEntity.name]");
        }

        args = new HashMap(args)

        def template = hibernateTemplate
        return (List<D>) template.execute { Session session ->
            def q = session.createQuery(query)
            template.applySettings(q)

            params.eachWithIndex { val, int i ->
                if (val instanceof CharSequence) {
                    q.setParameter i, val.toString()
                }
                else {
                    q.setParameter i, val
                }
            }
            populateQueryArguments(q, args)
            return q.list()
        }
    }

    @Override
    D find(D exampleObject, Map args) {
        def template = hibernateTemplate
        return (D) template.execute { Session session ->
            Example example = Example.create(exampleObject).ignoreCase()

            Criteria crit = session.createCriteria(persistentEntity.javaClass);
            hibernateTemplate.applySettings(crit)
            crit.add example
            GrailsHibernateQueryUtils.populateArgumentsForCriteria(persistentEntity, crit, args, datastore.mappingContext.conversionService, true)
            crit.maxResults = 1
            List results = crit.list()
            if (results) {
                return proxyHandler.unwrap( results.get(0) )
            }
        }
    }

    @Override
    List<D> findAll(D exampleObject, Map args) {
        def template = hibernateTemplate
        return (List<D>) template.execute { Session session ->
            Example example = Example.create(exampleObject).ignoreCase()

            Criteria crit = session.createCriteria(persistentEntity.javaClass);
            hibernateTemplate.applySettings(crit)
            crit.add example
            GrailsHibernateQueryUtils.populateArgumentsForCriteria(persistentEntity, crit, args, datastore.mappingContext.conversionService, true)
            crit.list()
        }
    }

    @Override
    List<D> findAllWhere(Map queryMap, Map args) {
        if (!queryMap) return null
        (List<D>)hibernateTemplate.execute { Session session ->
            Map<String, Object> processedQueryMap = [:]
            queryMap.each{ key, value -> processedQueryMap[key.toString()] = value }
            Map queryArgs = filterQueryArgumentMap(processedQueryMap)
            List<String> nullNames = removeNullNames(queryArgs)
            Criteria criteria = session.createCriteria(persistentClass)
            hibernateTemplate.applySettings(criteria)
            criteria.add(Restrictions.allEq(queryArgs))
            for (name in nullNames) {
                criteria.add Restrictions.isNull(name)
            }
            criteria.setResultTransformer(DistinctRootEntityResultTransformer.INSTANCE)
            criteria.list()
        }
    }

    @Override
    List<D> executeQuery(String query, Map params, Map args) {
        def template = hibernateTemplate
        args = new HashMap(args)
        params = new HashMap(params)

        return (List<D>) template.execute { Session session ->
            def q = session.createQuery(query)
            template.applySettings(q)

            populateQueryArguments(q, params)
            populateQueryArguments(q, args)
            populateQueryWithNamedArguments(q, params)

            return q.list()
        }
    }

    @Override
    List<D> executeQuery(String query, Collection params, Map args) {
        def template = hibernateTemplate
        args = new HashMap(args)

        return (List<D>) template.execute { Session session ->
            def q = session.createQuery(query)
            template.applySettings(q)

            params.eachWithIndex { val, int i ->
                if (val instanceof CharSequence) {
                    q.setParameter i, val.toString()
                }
                else {
                    q.setParameter i, val
                }
            }
            populateQueryArguments(q, args)
            return q.list()
        }
    }

    @Override
    D findWhere(Map queryMap, Map args) {
        if (!queryMap) return null
        (D)hibernateTemplate.execute { Session session ->
            Map<String, Object> processedQueryMap = [:]
            queryMap.each{ key, value -> processedQueryMap[key.toString()] = value }
            Map queryArgs = filterQueryArgumentMap(processedQueryMap)
            List<String> nullNames = removeNullNames(queryArgs)
            Criteria criteria = session.createCriteria(persistentClass)
            hibernateTemplate.applySettings(criteria)
            criteria.add(Restrictions.allEq(queryArgs))
            for (name in nullNames) {
                criteria.add Restrictions.isNull(name)
            }
            criteria.setMaxResults(1)
            proxyHandler.unwrap(criteria.uniqueResult())
        }
    }



    List<D> getAll(List ids) {
        getAllInternal(ids)
    }

    List<D> getAll(Long... ids) {
        getAllInternal(ids as List)
    }

    @Override
    List<D> getAll(Serializable... ids) {
        getAllInternal(ids as List)
    }

    private List getAllInternal(List ids) {
        if (!ids) return []

        (List)hibernateTemplate.execute { Session session ->
            def identityType = persistentEntity.identity.type
            ids = ids.collect { HibernateRuntimeUtils.convertValueToType((Serializable)it, identityType, conversionService) }
            def criteria = session.createCriteria(persistentClass)
            hibernateTemplate.applySettings(criteria)
            def identityName = persistentEntity.identity.name
            criteria.add(Restrictions.'in'(identityName, ids))
            def results = criteria.list()
            def idsMap = [:]
            for (object in results) {
                idsMap[object[identityName]] = object
            }
            results.clear()
            for (id in ids) {
                results << idsMap[id]
            }
            results
        }
    }

    protected Map filterQueryArgumentMap(Map query) {
        def queryArgs = [:]
        for (entry in query.entrySet()) {
            if (entry.value instanceof CharSequence) {
                queryArgs[entry.key] = entry.value.toString()
            }
            else {
                queryArgs[entry.key] = entry.value
            }
        }
        return queryArgs
    }


    protected List<String> removeNullNames(Map query) {
        List<String> nullNames = []
        Set<String> allNames = new HashSet(query.keySet())
        for (String name in allNames) {
            if (query[name] == null) {
                query.remove name
                nullNames << name
            }
        }
        nullNames
    }

    protected D executeHibernateQuery(Query q, Map args) {
        q.maxResults = 1
        List results = q.list()
        if (results) {
            return (D)proxyHandler.unwrap(results.get(0))
        }
    }

    protected Serializable convertIdentifier(Serializable id) {
        def identity = persistentEntity.identity
        if(identity != null) {
            ConversionService conversionService = persistentEntity.mappingContext.conversionService
            if(id != null) {
                Class identityType = identity.type
                Class idInstanceType = id.getClass()
                if(identityType.isAssignableFrom(idInstanceType)) {
                    return id
                }
                else if(conversionService.canConvert(idInstanceType, identityType)) {
                    try {
                        return (Serializable)conversionService.convert(id, identityType)
                    } catch (Throwable e) {
                        // unconvertable id, return null
                        return null
                    }
                }
                else {
                    // unconvertable id, return null
                    return null
                }
            }
        }
        return id
    }

    protected void populateQueryWithNamedArguments(Query q, Map queryNamedArgs) {

        if (queryNamedArgs) {
            for (Map.Entry entry in queryNamedArgs.entrySet()) {
                def key = entry.key
                if (!(key instanceof CharSequence)) {
                    throw new GrailsQueryException("Named parameter's name must be String: $queryNamedArgs")
                }
                String stringKey = key.toString()
                def value = entry.value

                if(value == null) {
                    q.setParameter stringKey, null
                } else if (value instanceof CharSequence) {
                    q.setParameter stringKey, value.toString()
                } else if (List.class.isAssignableFrom(value.getClass())) {
                    q.setParameterList stringKey, (List) value
                } else if (value.getClass().isArray()) {
                    q.setParameterList stringKey, (Object[]) value
                } else {
                    q.setParameter stringKey, value
                }
            }
        }
    }

    protected Integer intValue(Map args, String key) {
        def value = args.get(key)
        if(value) {
            return conversionService.convert(value, Integer.class)
        }
        return null
    }

    protected void populateQueryArguments(Query q, Map args) {
        Integer max = intValue(args, DynamicFinder.ARGUMENT_MAX)
        args.remove(DynamicFinder.ARGUMENT_MAX)
        Integer offset = intValue(args, DynamicFinder.ARGUMENT_OFFSET)
        args.remove(DynamicFinder.ARGUMENT_OFFSET)

        //
        if (max != null) {
            q.maxResults = max
        }
        if (offset != null) {
            q.firstResult = offset
        }

        if (args.containsKey(DynamicFinder.ARGUMENT_CACHE)) {
            q.cacheable = ClassUtils.getBooleanFromMap(DynamicFinder.ARGUMENT_CACHE, args)
        }
        if (args.containsKey(DynamicFinder.ARGUMENT_FETCH_SIZE)) {
            Integer fetchSizeParam = conversionService.convert(args.remove(DynamicFinder.ARGUMENT_FETCH_SIZE), Integer.class);
            q.setFetchSize(fetchSizeParam.intValue());
        }
        if (args.containsKey(DynamicFinder.ARGUMENT_TIMEOUT)) {
            Integer timeoutParam = conversionService.convert(args.remove(DynamicFinder.ARGUMENT_TIMEOUT), Integer.class);
            q.setTimeout(timeoutParam.intValue());
        }
        if (args.containsKey(DynamicFinder.ARGUMENT_READ_ONLY)) {
            q.setReadOnly((Boolean)args.remove(DynamicFinder.ARGUMENT_READ_ONLY));
        }
        if (args.containsKey(DynamicFinder.ARGUMENT_FLUSH_MODE)) {
            q.setFlushMode((FlushMode)args.remove(DynamicFinder.ARGUMENT_FLUSH_MODE));
        }

        args.remove(DynamicFinder.ARGUMENT_CACHE)
    }

    private String normalizeMultiLineQueryString(String query) {
        if (query.indexOf('\n') != -1)
           return query.trim().replace('\n', ' ')
        return query
    }
}
