package grails.plugins.publish

/**
 * Simple class representing a remote repository that plugins and applications
 * can be published to.
 */
class Repository {
    /** URL for 'grailsCentral' portal */
    static final String GRAILS_CENTRAL_PORTAL_URL = "http://grails.org/plugin/"

    /** The standard Grails Central repository. */
    static final Repository grailsCentral = new Repository(
            "grailsCentral",
            new URI("http://grails.org/plugins"),
            "grailsCentral")

    /** The name of this repository. */
    final String name

    /** The root URI to publish to. */
    final URI uri

    /** The ID of the default portal to ping when publishing to this repository. */
    final String defaultPortal

    Repository(String name, URI publishUri, String portalUri) {
        this.name = name
        this.uri = publishUri
        this.defaultPortal = portalUri
    }
}
