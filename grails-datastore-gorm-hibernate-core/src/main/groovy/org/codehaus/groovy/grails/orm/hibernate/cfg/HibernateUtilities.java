package org.codehaus.groovy.grails.orm.hibernate.cfg;

import java.util.List;

import grails.core.GrailsDomainClass;
import grails.core.GrailsDomainClassProperty;
import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateDomainClass;

public class HibernateUtilities {

    public static boolean usesDatasource(GrailsDomainClass domainClass, String dataSourceName, AbstractGrailsDomainBinder binder) {
        if (domainClass instanceof AbstractGrailsHibernateDomainClass) {
            AbstractGrailsHibernateDomainClass hibernateDomainClass = (AbstractGrailsHibernateDomainClass)domainClass;
            String sessionFactoryName = hibernateDomainClass.getSessionFactoryName();
            if (dataSourceName.equals(GrailsDomainClassProperty.DEFAULT_DATA_SOURCE)) {
                return "sessionFactory".equals(sessionFactoryName);
            }
            return sessionFactoryName.endsWith("_" + dataSourceName);
        }

        List<String> names = getDatasourceNames(domainClass, binder);
        return names.contains(dataSourceName) ||
               names.contains(GrailsDomainClassProperty.ALL_DATA_SOURCES);
    }

    public static List<String> getDatasourceNames(GrailsDomainClass domainClass, AbstractGrailsDomainBinder binder) {
        // Mappings won't have been built yet when this is called from
        // HibernatePluginSupport.doWithSpring  so do a temporary evaluation but don't cache it
        Mapping mapping = isMappedWithHibernate(domainClass) ? binder.evaluateMapping(domainClass, null, false) : null;
        if (mapping == null) {
            mapping = new Mapping();
        }
        return mapping.getDatasources();
    }

    public static boolean isMappedWithHibernate(GrailsDomainClass domainClass) {
        return domainClass instanceof AbstractGrailsHibernateDomainClass || domainClass.getMappingStrategy().equals(GrailsDomainClass.GORM);
    }
}
