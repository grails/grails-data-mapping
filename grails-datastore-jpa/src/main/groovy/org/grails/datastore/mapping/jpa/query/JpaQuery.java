/* Copyright (C) 2011 SpringSource
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

package org.grails.datastore.mapping.jpa.query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.grails.datastore.mapping.jpa.JpaSession;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.query.Query;
import org.grails.datastore.mapping.query.Restrictions;
import org.grails.datastore.mapping.query.jpa.JpaQueryBuilder;
import org.grails.datastore.mapping.query.jpa.JpaQueryInfo;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.orm.jpa.JpaCallback;
import org.springframework.orm.jpa.JpaTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import java.util.List;

/**
 * Query implementation for JPA.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
@SuppressWarnings({"hiding", "rawtypes", "unchecked"})
public class JpaQuery extends Query {
    private static final Log LOG = LogFactory.getLog(JpaQuery.class);

    public JpaQuery(JpaSession session, PersistentEntity entity) {
        super(session, entity);

        if (session == null) {
            throw new InvalidDataAccessApiUsageException("Argument session cannot be null");
        }
        if (entity == null) {
            throw new InvalidDataAccessApiUsageException("No persistent entity specified");
        }
    }

    @Override
    public JpaSession getSession() {
        return (JpaSession) super.getSession();
    }

    @Override
    public void add(Criterion criterion) {
        if (criterion instanceof Equals) {
            final Equals eq = (Equals) criterion;
            Object resolved = resolveIdIfEntity(eq.getValue());
            if (resolved != eq.getValue()) {
                criterion = Restrictions.idEq(resolved);
            }
        }

        criteria.add(criterion);
    }

    @Override
    protected List executeQuery(final PersistentEntity entity, final Junction criteria) {
        final JpaTemplate jpaTemplate = getSession().getJpaTemplate();

        return (List)jpaTemplate.execute(new JpaCallback<Object>() {
            public Object doInJpa(EntityManager em) throws PersistenceException {
                return executeQuery(entity, criteria, em, false);
            }
        });
    }

    @Override
    public Object singleResult() {
        final JpaTemplate jpaTemplate = getSession().getJpaTemplate();
        try {
            return jpaTemplate.execute(new JpaCallback<Object>() {
                public Object doInJpa(EntityManager em) throws PersistenceException {
                    return executeQuery(entity, criteria, em, true);
                }
            });
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }




    Object executeQuery(final PersistentEntity entity, final Junction criteria,
            EntityManager em, boolean singleResult) {


        JpaQueryBuilder queryBuilder = new JpaQueryBuilder(entity, criteria, projections, orderBy);
        queryBuilder.setConversionService(session.getDatastore().getMappingContext().getConversionService());
        JpaQueryInfo jpaQueryInfo = queryBuilder.buildSelect();
        List parameters = jpaQueryInfo.getParameters();
        final String queryToString = jpaQueryInfo.getQuery();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Built JPQL to execute: " + queryToString);
        }
        final javax.persistence.Query q = em.createQuery(queryToString);

        if (parameters != null) {
            for (int i = 0, count = parameters.size(); i < count; i++) {
                q.setParameter(i + 1, parameters.get(i));
            }
        }
        q.setFirstResult(offset);
        if (max > -1) {
            q.setMaxResults(max);
        }

        if (!singleResult) {
            return q.getResultList();
        }
        return q.getSingleResult();
    }


}
