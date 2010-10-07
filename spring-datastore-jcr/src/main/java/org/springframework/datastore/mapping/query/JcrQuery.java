package org.springframework.datastore.mapping.query;

import org.springframework.core.convert.ConversionService;
import org.springframework.datastore.mapping.core.Session;
import org.springframework.datastore.mapping.jcr.JcrSession;
import org.springframework.datastore.mapping.jcr.engine.JcrEntityPersister;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.extensions.jcr.JcrTemplate;

import java.util.List;

/**
 * @author Erawat Chamanont
 * @since 1.0
 */
public class JcrQuery extends Query {
    private JcrEntityPersister entityPersister;
    private JcrTemplate template;
    private ConversionService conversionService;

    public JcrQuery(JcrSession session, JcrTemplate jcrTemplate, PersistentEntity persistentEntity, JcrEntityPersister entityPersister) {
        super(session, persistentEntity);
        this.entityPersister = entityPersister;
        template = jcrTemplate;
        conversionService = getSession().getMappingContext().getConversionService();
    }
    protected JcrQuery(Session session, PersistentEntity entity) {
        super(session, entity);
    }

    @Override
    protected List executeQuery(PersistentEntity entity, Junction criteria) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
