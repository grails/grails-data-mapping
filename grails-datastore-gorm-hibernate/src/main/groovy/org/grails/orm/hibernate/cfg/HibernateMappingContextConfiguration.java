package org.grails.orm.hibernate.cfg;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.orm.hibernate.proxy.GroovyAwarePojoEntityTuplizer;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.*;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;

import java.util.*;

/**
 * A Configuration that uses a MappingContext to configure Hibernate
 *
 * @since 5.0
 */
public class HibernateMappingContextConfiguration extends Configuration {
    private static final long serialVersionUID = -7115087342689305517L;

    protected static final Log LOG = LogFactory.getLog(GrailsAnnotationConfiguration.class);

    protected boolean configLocked;
    protected String sessionFactoryBeanName = "sessionFactory";
    protected String dataSourceName = Mapping.DEFAULT_DATA_SOURCE;
    protected GrailsDomainBinder binder = new GrailsDomainBinder();
    protected HibernateMappingContext hibernateMappingContext;
    private boolean subclassForeignKeysCreated = false;


    public void setHibernateMappingContext(HibernateMappingContext hibernateMappingContext) {
        this.hibernateMappingContext = hibernateMappingContext;
    }

    public void setSessionFactoryBeanName(String name) {
        sessionFactoryBeanName = name;
    }

    public void setDataSourceName(String name) {
        dataSourceName = name;
    }

    /* (non-Javadoc)
     * @see org.hibernate.cfg.Configuration#buildSessionFactory()
     */
    @Override
    public SessionFactory buildSessionFactory() throws HibernateException {

        // work around for HHH-2624
        addFilterDefinition(new FilterDefinition("dynamicFilterEnabler", "1=1", Collections.emptyMap()));

        SessionFactory sessionFactory = super.buildSessionFactory();

//        if (grailsApplication != null) {
//            GrailsHibernateUtil.configureHibernateDomainClasses(
//                    sessionFactory, sessionFactoryBeanName, grailsApplication);
//        }

        return sessionFactory;
    }

    @Override
    public Settings buildSettings() {
        configureNamingStrategy();
        Settings settings = super.buildSettings();
        settings.getEntityTuplizerFactory().registerDefaultTuplizerClass(EntityMode.POJO, GroovyAwarePojoEntityTuplizer.class);
        return settings;
    }

    @Override
    public Settings buildSettings(Properties props) throws HibernateException {
        configureNamingStrategy();
        Settings settings = super.buildSettings(props);
        settings.getEntityTuplizerFactory().registerDefaultTuplizerClass(EntityMode.POJO, GroovyAwarePojoEntityTuplizer.class);
        return settings;
    }


    /**
     * Overrides the default behaviour to including binding of Grails domain classes.
     */
    @Override
    protected void secondPassCompile() throws MappingException {
        if (!configLocked) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("[GrailsAnnotationConfiguration] [" + hibernateMappingContext.getPersistentEntities().size() +
                        "] Grails domain classes to bind to persistence runtime");
            }

            // do Grails class configuration
            // configure the static binder first

            for (PersistentEntity entity : hibernateMappingContext.getPersistentEntities()) {
                binder.evaluateMapping(entity);
            }

            for (PersistentEntity domainClass : hibernateMappingContext.getPersistentEntities()) {

                final String fullClassName = domainClass.getName();

                String hibernateConfig = fullClassName.replace('.', '/') + ".hbm.xml";

                final Mappings mappings = super.createMappings();
                if (!GrailsHibernateUtil.usesDatasource(domainClass, dataSourceName)) {
                    continue;
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("[GrailsAnnotationConfiguration] Binding persistent class [" + fullClassName + "]");
                }

                Mapping m = binder.getMapping(domainClass);
                mappings.setAutoImport(m == null || m.getAutoImport());
                binder.bindClass(domainClass, mappings, sessionFactoryBeanName);
            }
        }

        super.secondPassCompile();
        createSubclassForeignKeys();

        configLocked = true;
    }

    /**
     * Creates foreign keys for subclass tables that are mapped using table per subclass. Further information is
     * available in the <a href="http://jira.grails.org/browse/GRAILS-7729">JIRA ticket</a>
     */
    private void createSubclassForeignKeys() {
        if (subclassForeignKeysCreated) {
            return;
        }

        for (PersistentClass persistentClass : classes.values()) {
            if (persistentClass instanceof RootClass) {
                RootClass rootClass = (RootClass) persistentClass;

                if (rootClass.hasSubclasses()) {
                    Iterator subclasses = rootClass.getSubclassIterator();

                    while (subclasses.hasNext()) {

                        Object subclass = subclasses.next();

                        // This test ensures that foreign keys will only be created for subclasses that are
                        // mapped using "table per subclass"
                        if (subclass instanceof JoinedSubclass) {
                            JoinedSubclass joinedSubclass = (JoinedSubclass) subclass;
                            joinedSubclass.createForeignKey();
                        }
                    }
                }
            }
        }

        subclassForeignKeysCreated = true;
    }

    /**
     * Sets custom naming strategy specified in configuration or the default {@link ImprovedNamingStrategy}.
     */
    protected void configureNamingStrategy() {
        NamingStrategy strategy = getNamingStrategy();
        if (strategy == null) {
            strategy = ImprovedNamingStrategy.INSTANCE;
        }

        setNamingStrategy(strategy);
    }

    @Override
    protected void reset() {
        super.reset();
        try {
            GrailsIdentifierGeneratorFactory.applyNewInstance(this);
        }
        catch (Exception e) {
            // ignore exception
        }
    }
}
