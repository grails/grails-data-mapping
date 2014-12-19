package org.grails.orm.hibernate

import grails.persistence.Entity
import org.junit.Test

/**
 * @author graemerocher
 */
class ManyToOneWithUUIDTests extends AbstractGrailsHibernateTests{


    @Test
    void testManyToOneWithUUIDAssociation() {
        def page = ManyToOneWithUUIDPage.newInstance()

        assert page.save(flush:true) != null

        def pe = ManyToOneWithUUIDPageElement.newInstance(page:page)
        assert pe.save()

        session.clear()

        pe = ManyToOneWithUUIDPageElement.get(pe.id)

        assert pe
        assert pe.page
    }

    @Override
    protected getDomainClasses() {
        [ManyToOneWithUUIDPage, ManyToOneWithUUIDPageElement]
    }
}

abstract class AbstractPage {}

@Entity
class ManyToOneWithUUIDPage extends org.grails.orm.hibernate.AbstractPage {
    String id
    Long version

    static mapping = {
        id generator:'uuid'
    }
}

@Entity
class ManyToOneWithUUIDPageElement {
    Long id
    Long version
    ManyToOneWithUUIDPage page
    static belongsTo = [page: ManyToOneWithUUIDPage]
}

