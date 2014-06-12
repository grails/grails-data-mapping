package org.grails.datastore.gorm.neo4j.pages

import geb.Page

/**
 * Created with IntelliJ IDEA.
 * User: stefan
 * Date: 01.05.13
 * Time: 19:11
 * To change this template use File | Settings | File Templates.
 */
class Neo4jPage extends Page {
    static content = {
        heading { $("h1") }
        message { $("div.message").text() }
    }
    static url = "neo4j"

    static at = {
        title ==~ /explore node space/

    }

}
