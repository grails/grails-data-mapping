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

package org.springframework.datastore.mapping.jpa.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.datastore.mapping.jpa.JpaSession;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.PersistentProperty;
import org.springframework.datastore.mapping.query.Query;
import org.springframework.orm.jpa.JpaCallback;
import org.springframework.orm.jpa.JpaTemplate;

/**
 * Query implementation for JPA
 * 
 * @author Graeme Rocher
 * @since 1.0
 *
 */
public class JpaQuery extends Query{
	
	public static final String NOT_CLAUSE = " NOT";
	public static final String LOGICAL_AND = " AND ";
	public static final String LOGICAL_OR = " OR ";
	
	private static final Log LOG = LogFactory.getLog(org.springframework.datastore.mapping.jpa.query.JpaQuery.class); 
	private static final Map<Class, QueryHandler> queryHandlers = new HashMap<Class, QueryHandler>();
	
	static public int appendCriteriaForOperator(StringBuilder q,
			String logicalName, final String name, int position, String operator) {
		q
        	.append(logicalName)
        	.append('.')
        	.append(name)
        	.append(operator)
        	.append('?')
        	.append(++position);
		return position;
	}
    static {
    	queryHandlers.put(Negation.class, new QueryHandler() {			
			@Override
			public int handle(PersistentEntity entity, Criterion criterion,
					StringBuilder q, String logicalName, int position, List parameters,
					ConversionService conversionService) {
				q.append(NOT_CLAUSE)
				 .append('(');
				
				final Negation negation = (Negation)criterion;
				position = buildWhereClauseForCriterion(entity, negation, q, logicalName, negation.getCriteria(), position, parameters, conversionService);
				q.append(')');
				
				return position;
			}
		});
    	queryHandlers.put(Conjunction.class, new QueryHandler() {			
			@Override
			public int handle(PersistentEntity entity, Criterion criterion,
					StringBuilder q, String logicalName, int position, List parameters,
					ConversionService conversionService) {
				q.append('(');
				
				final Conjunction conjunction = (Conjunction)criterion;
				position = buildWhereClauseForCriterion(entity, conjunction, q, logicalName, conjunction.getCriteria(), position, parameters, conversionService);
				q.append(')');
				
				return position;
			}
		});
    	queryHandlers.put(Disjunction.class, new QueryHandler() {			
			@Override
			public int handle(PersistentEntity entity, Criterion criterion,
					StringBuilder q, String logicalName, int position, List parameters,
					ConversionService conversionService) {
				q.append('(');
				
				final Disjunction disjunction = (Disjunction)criterion;
				position = buildWhereClauseForCriterion(entity, disjunction, q, logicalName, disjunction.getCriteria(), position, parameters, conversionService);
				q.append(')');
				
				return position;
			}
		});
        queryHandlers.put(Equals.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, String logicalName, int position, List parameters, ConversionService conversionService) {
                Equals eq = (Equals) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, Equals.class);
                Class propType = prop.getType();
                position = appendCriteriaForOperator(q, logicalName, name, position, "=");                
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });
        queryHandlers.put(IdEquals.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, String logicalName, int position, List parameters, ConversionService conversionService) {
            	IdEquals eq = (IdEquals) criterion;
            	PersistentProperty prop = entity.getIdentity();
                Class propType = prop.getType();
                position = appendCriteriaForOperator(q, logicalName, prop.getName(), position, "=");                
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });        
        queryHandlers.put(NotEquals.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, String logicalName, int position, List parameters, ConversionService conversionService) {
            	NotEquals eq = (NotEquals) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, Equals.class);
                Class propType = prop.getType();
                position = appendCriteriaForOperator(q, logicalName, name, position, " != ");                
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });        
        queryHandlers.put(GreaterThan.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, String logicalName, int position, List parameters, ConversionService conversionService) {
            	GreaterThan eq = (GreaterThan) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, GreaterThan.class);
                Class propType = prop.getType();
                position = appendCriteriaForOperator(q, logicalName, name, position, " > ");                
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });
        queryHandlers.put(LessThanEquals.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, String logicalName, int position, List parameters, ConversionService conversionService) {
            	LessThanEquals eq = (LessThanEquals) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, LessThanEquals.class);
                Class propType = prop.getType();
                position = appendCriteriaForOperator(q, logicalName, name, position, " <= ");                
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });  
        queryHandlers.put(GreaterThanEquals.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, String logicalName, int position, List parameters, ConversionService conversionService) {
            	GreaterThanEquals eq = (GreaterThanEquals) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, GreaterThanEquals.class);
                Class propType = prop.getType();
                position = appendCriteriaForOperator(q, logicalName, name, position, " >= ");                
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });  
        queryHandlers.put(Between.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, String logicalName, int position, List parameters, ConversionService conversionService) {
            	Between between = (Between) criterion;
            	final Object from = between.getFrom();
            	final Object to = between.getTo();
            	
                final String name = between.getProperty();
                PersistentProperty prop = validateProperty(entity, name, Between.class);
                Class propType = prop.getType();
        		final String qualifiedName = logicalName + '.' + name;
				q
        		.append('(')
            	.append(qualifiedName)
            	.append(" >= ")
            	.append('?')
            	.append(++position)
            	.append(" AND ")
            	.append(qualifiedName)
            	.append(" <= ")
            	.append('?')
            	.append(++position)
            	.append(')');
            	
        		
        		
                parameters.add(conversionService.convert( from, propType ));
                parameters.add(conversionService.convert( to, propType ));
                return position;
            }
        });         
        queryHandlers.put(LessThan.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, String logicalName, int position, List parameters, ConversionService conversionService) {
            	LessThan eq = (LessThan) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, LessThan.class);
                Class propType = prop.getType();
                position = appendCriteriaForOperator(q, logicalName, name, position, " < ");                
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
            }
        });  
                
        queryHandlers.put(Like.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, String logicalName, int position, List parameters, ConversionService conversionService) {
            	Like eq = (Like) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, Like.class);
                Class propType = prop.getType();
                position = appendCriteriaForOperator(q, logicalName, name, position, " like ");                
                parameters.add(conversionService.convert( eq.getValue(), propType ));
                return position;
                	
            }
        });        
        queryHandlers.put(In.class, new QueryHandler() {
            public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, String logicalName, int position, List parameters, ConversionService conversionService) {
            	In eq = (In) criterion;
                final String name = eq.getProperty();
                PersistentProperty prop = validateProperty(entity, name, In.class);
                Class propType = prop.getType();
                q
                	.append(logicalName)
                	.append('.')
                	.append(name)
                	.append(" IN (");
                	for (Iterator i = eq.getValues().iterator(); i
							.hasNext();) {
						Object val = i.next();
                		q.append("?");
                		q.append(++position);
                		if(i.hasNext())
                			q.append(",");
                		parameters.add(conversionService.convert(val, propType));
						
					}
                	q.append(")");
                
                return position;
            }
        });        
    }	
    
    private static PersistentProperty validateProperty(PersistentEntity entity, String name, Class criterionType) {
        if(entity.getIdentity().getName().equals(name)) return entity.getIdentity();
        PersistentProperty prop = entity.getPropertyByName(name);
        if(prop == null) {
            throw new InvalidDataAccessResourceUsageException("Cannot use ["+ criterionType.getSimpleName()+"] criterion on non-existent property: " + name);
        }
        return prop;
    }
    
	public JpaQuery(JpaSession session, PersistentEntity entity) {
		super(session, entity);
		
		if(session == null) {
			throw new InvalidDataAccessApiUsageException("Argument session cannot be null");
		}
		if(entity == null) {
			throw new InvalidDataAccessApiUsageException("No persistent entity specified");
		}
		
	}
	
	@Override
	public JpaSession getSession() {
		return (JpaSession) super.getSession();
	}

	@Override
	protected List executeQuery(final PersistentEntity entity, final Junction criteria) {
		final JpaTemplate jpaTemplate = getSession().getJpaTemplate();
		
		
		return jpaTemplate.execute(new JpaCallback<List>() {

			@Override
			public List doInJpa(EntityManager em) throws PersistenceException {
				
				final String logicalName = entity.getDecapitalizedName();
				StringBuilder queryString = new StringBuilder("SELECT ");
				
				if(projections.isEmpty()) {
					queryString
					.append(logicalName);
				}
				else {
					for (Iterator i = projections.getProjectionList().iterator(); i
							.hasNext();) {
						Projection projection = (Projection) i.next();
						if(projection instanceof CountProjection) {
							queryString.append("COUNT(")
									   .append(logicalName)
									   .append(')');
						}
						else if(projection instanceof IdProjection) {
							queryString.append(logicalName)
									   .append('.')
									   .append(entity.getIdentity().getName());
						}
						else if(projection instanceof PropertyProjection) {
							PropertyProjection pp = (PropertyProjection) projection;
							if(projection instanceof AvgProjection) {
								queryString
								   .append("AVG(")
								   .append(logicalName)
								   .append('.')
								   .append(pp.getPropertyName())
								   .append(')');
							}
							else if(projection instanceof SumProjection) {
								queryString
								   .append("SUM(")
								   .append(logicalName)
								   .append('.')
								   .append(pp.getPropertyName())
								   .append(')');
							}	
							else if(projection instanceof MinProjection) {
								queryString
								   .append("MIN(")
								   .append(logicalName)
								   .append('.')
								   .append(pp.getPropertyName())
								   .append(')');
							}	
							else if(projection instanceof MaxProjection) {
								queryString
								   .append("MAX(")
								   .append(logicalName)
								   .append('.')
								   .append(pp.getPropertyName())
								   .append(')');
							}								
							else {
								queryString.append(logicalName)
								   .append('.')
								   .append(pp.getPropertyName());								
							}
						}
						
						if(i.hasNext()) {
							queryString.append(',');
						}
					}
						
				}
				queryString
						.append(" FROM " )
						.append(entity.getName())
						.append(" AS " )
						.append(logicalName);
				
				List parameters = null;
				if(!criteria.isEmpty()) {
					parameters = buildWhereClause(entity, criteria, queryString, logicalName);
				}
				
				appendOrder(queryString, logicalName);						
				final String queryToString = queryString.toString();
				
				if(LOG.isDebugEnabled()) {
					LOG.debug("Built JPQL to execute: " + queryToString);
				}
				final javax.persistence.Query q = em.createQuery(queryToString);
				
				if(parameters != null) {
					for (int i = 0; i < parameters.size(); i++) {
						final Object value = parameters.get(i);
						q.setParameter(i+1, value);						
					}
				}
				q.setFirstResult(offset);
				if(max > -1)
					q.setMaxResults(max);
									
				return q.getResultList();				
			}
		});
	}

	protected void appendOrder(StringBuilder queryString, String logicalName) {
		if(!orderBy.isEmpty()) {
			queryString.append( " ORDER BY ");
			for (Order order : orderBy) {
				queryString.append(logicalName)
						   .append('.')
						   .append(order.getProperty())
						   .append(' ')
						   .append(order.getDirection() == Order.Direction.ASC ? "ASC" : "DESC")
						   .append(' ');
			}
		}
	}

    private static interface QueryHandler {
        public int handle(PersistentEntity entity, Criterion criterion, StringBuilder q, String logicalName, int position, List parameters, ConversionService conversionService);
    }
    private List buildWhereClause(PersistentEntity entity, Junction criteria, StringBuilder q, String logicalName) {
        final List<Criterion> criterionList = criteria.getCriteria();
        q.append(" WHERE ");
        if(criteria instanceof Negation) {
            q.append(NOT_CLAUSE);
        }
        q.append('(');
        int position = 0;
        List parameters = new ArrayList();
        position = buildWhereClauseForCriterion(entity, criteria, q,
				logicalName, criterionList, position, parameters, getSession().getMappingContext().getConversionService());
        q.append(')');
        return parameters;
    }

	static int buildWhereClauseForCriterion(PersistentEntity entity,
			Junction criteria, StringBuilder q, String logicalName,
			final List<Criterion> criterionList, int position, List parameters, ConversionService conversionService) {
		for (Iterator<Criterion> iterator = criterionList.iterator(); iterator.hasNext();) {
            Criterion criterion = iterator.next();

            final String operator = criteria instanceof Conjunction ? LOGICAL_AND : LOGICAL_OR;
            QueryHandler qh = queryHandlers.get(criterion.getClass());
            if(qh != null) {
            	position = qh.handle(entity,criterion, q, logicalName,position,parameters, conversionService);
            }

            if(iterator.hasNext())
                q.append(operator);

        }
		return position;
	}	
}
