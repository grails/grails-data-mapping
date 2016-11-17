package grails.gorm.hibernate

import groovy.transform.CompileStatic
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.gorm.GormEntity
import org.grails.orm.hibernate.AbstractHibernateGormStaticApi

/**
 * Extends the {@link GormEntity} trait adding additional Hibernate specific methods
 *
 * @author Graeme Rocher
 * @since 6.1
 */
@CompileStatic
trait HibernateEntity<D> extends GormEntity<D> {

    /**
     * Finds all objects for the given string-based query
     *
     * @param sql The query
     *
     * @return The object
     */
    static List<D> findAllWithSql(CharSequence sql) {
        currentHibernateStaticApi().findAllWithSql sql
    }

    private static AbstractHibernateGormStaticApi currentHibernateStaticApi() {
        (AbstractHibernateGormStaticApi)GormEnhancer.findStaticApi(this)
    }
}