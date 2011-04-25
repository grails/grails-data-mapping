package org.grails.datastore.gorm.neo4j

import org.grails.datastore.gorm.GormEnhancer
import org.springframework.datastore.mapping.core.Datastore
import org.springframework.transaction.PlatformTransactionManager

/**
 * Created by IntelliJ IDEA.
 * User: stefan
 * Date: 25.04.11
 * Time: 13:10
 * To change this template use File | Settings | File Templates.
 */
class Neo4jGormEnhancer extends GormEnhancer {

    Neo4jGormEnhancer(Datastore datastore) {
        super(datastore, null)
    }

    Neo4jGormEnhancer(Datastore datastore, PlatformTransactionManager transactionManager) {
        super(datastore, transactionManager)
    }
}
