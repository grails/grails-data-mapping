package org.grails.orm.hibernate

import grails.persistence.Entity

@Entity
class NamedCriteriaPublicationSubclassWithNamedQueries extends NamedCriteriaPublication {
    static namedQueries = {
        oldPaperbacks {
            println "GOO!!"
            paperbacks()
            lt 'datePublished', new Date() - 365
        }
    }
}
