package org.springframework.datastore.mapping.query;

import org.springframework.core.convert.ConversionService;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.jcr.JcrSession;
import org.springframework.datastore.mapping.jcr.engine.JcrEntityPersister;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.datastore.mapping.model.PersistentProperty;
import org.springframework.extensions.jcr.JcrTemplate;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Erawat Chamanont
 * @since 1.0
 */
public class JcrQuery extends Query {
    private JcrEntityPersister entityPersister;
    private JcrTemplate jcrTemplate;
    private ConversionService conversionService;

    public JcrQuery(JcrSession session, JcrTemplate jcrTemplate, PersistentEntity persistentEntity, JcrEntityPersister entityPersister) {
        super(session, persistentEntity);
        this.entityPersister = entityPersister;
        this.jcrTemplate = jcrTemplate;
        this.conversionService = getSession().getMappingContext().getConversionService();
    }

    protected JcrQuery(Session session, PersistentEntity entity) {
        super(session, entity);
    }

    @Override
    protected List executeQuery(PersistentEntity entity, Junction criteria) {
        List<String> results = null;                 
        String jcrNode = null;
        if (criteria.isEmpty()) {
             System.out.println("criteria.isEmpty()");
             jcrNode = entity.getJavaClass().getSimpleName();
             System.out.println(jcrNode);
        } else {
            List criteriaList = criteria.getCriteria();
            System.out.println("criteriaList and size: " + criteriaList.size());

        }
        final ProjectionList projectionList = projections();
        IdProjection idProjection = null;
        if (projectionList.isEmpty()) {
            System.out.println("projectionList.isEmpty()");
            results = paginateResults(jcrNode);

        } else {
            List projectionResults = new ArrayList();
            System.out.println("projectionResults size: "+projectionResults.size());             

            //String postSortAndPaginationKey = null;
            for (Projection projection : projectionList.getProjectionList()) {
                if (projection instanceof CountProjection) {
                    System.out.println("CountProjection");

                } else if (projection instanceof MaxProjection) {
                    MaxProjection max = (MaxProjection) projection;
                    System.out.println("MaxProjection");

                } else if (projection instanceof MinProjection) {
                    MinProjection min = (MinProjection) projection;
                    System.out.println("MinProjection");

                } else {
                    final String projectionType = projection.getClass().getSimpleName();
                    System.out.println("projectionType: "+projectionType);
                    if (projection instanceof SumProjection) {
                        System.out.println("SumProjection");

                    } else if (projection instanceof AvgProjection) {
                        System.out.println("AvgProjection");

                    } else if (projection instanceof PropertyProjection) {
                        System.out.println("PropertyProjection");

                        PropertyProjection propertyProjection = (PropertyProjection) projection;
                        System.out.println("propertyProjection.getPropertyName()" + propertyProjection.getPropertyName());
                        //final PersistentProperty validProperty = getValidProperty(propertyProjection);
                        //if (postSortAndPaginationKey == null) postSortAndPaginationKey = storeSortedKey(finalKey);

                        //String entityKey = entityPersister.getEntityBaseKey();
                        //final List<String> values = template.sort(postSortAndPaginationKey, template.sortParams().get(entityKey + ":*->" + validProperty.getName()));
                        List resultList = new ArrayList();
                        //Class type = validProperty.getType();
                        //final PersistentEntity associatedEntity = getSession().getMappingContext().getPersistentEntity(type.getName());
                        //final boolean isEntityType = associatedEntity != null;
                        // if (isEntityType) {
                        //return getSession().retrieveAll(type, values);
                        //} else {
                        //for (String value : values) {
                        //    resultList.add(conversionService.convert(value, type));
                        // }

                        return resultList;
                        // }

                    } else if (projection instanceof IdProjection) {
                        System.out.println("IdProjection");
                        idProjection = (IdProjection) projection;

                    }
                }
            }

            if (!projectionResults.isEmpty()){
                System.out.println("!projectionResults.isEmpty");
                return projectionResults;
            }else {
                System.out.println("projectionResults.isEmpty");
                // results = paginateResults(finalKey);
            }
        }


        if (results != null) {
            System.out.println("results != null");
            if (idProjection != null) {
                System.out.println("idProjection != null");
                //return RedisQueryUtils.transformRedisResults(conversionService, results);
                return results;
            } else {
                for(String str : results)
                    System.out.println("result: " + str);
                System.out.println("idProjection == null");   
                return getSession().retrieveAll(getEntity().getJavaClass(), results);
            }
        } else {
            System.out.println("results == null");            
            return Collections.emptyList();
        }
    }

    private List<String> paginateResults(String jcrNode) {
        StringBuffer query = new StringBuffer();
        query.append("//");
        query.append(jcrNode);
        System.out.println(query.toString());
        QueryResult qr = jcrTemplate.query(query.toString(), javax.jcr.query.Query.XPATH);
        List<String> uuids = new ArrayList<String>();
        try {
            NodeIterator itr = qr.getNodes();
            while(itr.hasNext()){
                Node node = itr.nextNode();
                uuids.add(node.getUUID());
            }
        } catch (RepositoryException e) {

        }
        return uuids; 
    }

}

