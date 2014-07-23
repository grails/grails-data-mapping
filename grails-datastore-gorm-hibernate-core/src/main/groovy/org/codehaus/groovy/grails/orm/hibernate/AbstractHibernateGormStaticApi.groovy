package org.codehaus.groovy.grails.orm.hibernate

import groovy.transform.CompileStatic
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.orm.hibernate.exceptions.GrailsQueryException
import org.codehaus.groovy.grails.orm.hibernate.proxy.SimpleHibernateProxyHandler
import org.codehaus.groovy.grails.orm.hibernate.query.GrailsHibernateQueryUtils
import org.codehaus.groovy.grails.support.proxy.ProxyHandler
import org.grails.datastore.gorm.GormStaticApi
import org.grails.datastore.gorm.finders.DynamicFinder
import org.grails.datastore.gorm.finders.FinderMethod
import org.grails.datastore.mapping.core.Datastore
import org.hibernate.Criteria
import org.hibernate.Query
import org.hibernate.Session
import org.hibernate.criterion.Example
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

    protected ProxyHandler proxyHandler = new SimpleHibernateProxyHandler()
    protected Pattern queryPattern
    protected IHibernateTemplate hibernateTemplate
    protected ConversionService conversionService

    AbstractHibernateGormStaticApi(Class<D> persistentClass, Datastore datastore, List<FinderMethod> finders, IHibernateTemplate hibernateTemplate) {
        this(persistentClass, datastore, finders, null, hibernateTemplate)
    }

    AbstractHibernateGormStaticApi(Class<D> persistentClass, Datastore datastore, List<FinderMethod> finders, PlatformTransactionManager transactionManager, IHibernateTemplate hibernateTemplate) {
        super(persistentClass, datastore, finders, transactionManager)
        this.hibernateTemplate = hibernateTemplate
        this.queryPattern = ~/(?i)from(?-i)\s+[${persistentEntity.name}|${persistentEntity.javaClass.simpleName}].*/
        this.conversionService = datastore.mappingContext.conversionService
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
        if (!queryPattern.matcher(query).matches()) {
            throw new GrailsQueryException("Invalid query [$query] for domain class [$persistentEntity.name]");
        }

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
        if (!queryPattern.matcher(query).matches()) {
            throw new GrailsQueryException("Invalid query [$query] for domain class [$persistentEntity.name]");
        }

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
        if (!queryPattern.matcher(query).matches()) {
            throw new GrailsQueryException("Invalid query [$query] for domain class [$persistentEntity.name]");
        }

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
                return proxyHandler.unwrapIfProxy( results.get(0) )
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

    protected D executeHibernateQuery(Query q, Map args) {
        q.maxResults = 1
        List results = q.list()
        if (results) {
            return (D)proxyHandler.unwrapIfProxy(results.get(0))
        }
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

        if (max != null) {
            q.maxResults = max
        }
        if (offset != null) {
            q.firstResult = offset
        }

        if (args.containsKey(DynamicFinder.ARGUMENT_CACHE)) {
            q.cacheable = GrailsClassUtils.getBooleanFromMap(DynamicFinder.ARGUMENT_CACHE, args)
        }
        args.remove(DynamicFinder.ARGUMENT_CACHE)
    }
}
