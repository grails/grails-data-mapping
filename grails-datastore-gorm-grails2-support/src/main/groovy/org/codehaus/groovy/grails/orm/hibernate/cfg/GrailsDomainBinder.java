package org.codehaus.groovy.grails.orm.hibernate.cfg;

import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.grails.orm.hibernate.cfg.AbstractGrailsDomainBinder;
import org.grails.orm.hibernate.cfg.Mapping;

/**
 * Provides a compatibility bridge to the old scaffolding mechanism
 *
 * @author Graeme Rocher
 */
public class GrailsDomainBinder {

    Mapping getMapping(GrailsDomainClass domainClass) {
        return AbstractGrailsDomainBinder.getMapping(domainClass.getClazz());
    }
}
