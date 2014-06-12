package grails.plugins.publish.portal

import grails.plugins.publish.PluginDeployer
import grails.plugins.rest.client.RestBuilder

/**
 * A deployer capable of deploying to the central repository at http://grails.org.
 * This implementation uses the grails.org REST API to implement the PluginDeployer interface.
 *
 * @author Graeme Rocher
 */
class GrailsCentralDeployer implements PluginDeployer {

    //private proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", 8888))
    RestBuilder rest = new RestBuilder(connectTimeout: 10000, readTimeout: 100000, proxy: null)
    String portalUrl = "http://grails.org/plugins"
    String username
    String password
    def askUser

    GrailsCentralDeployer(askUser = null) {
        this.askUser = askUser
    }

    boolean isVersionAlreadyPublished(File pomFile) {
        def (pluginName, pluginVersion) = parsePom(pomFile)
        def resp = rest.get("$baseUrl/api/v1.0/plugin/$pluginName/$pluginVersion")
        return resp?.status == 200
    }

    void deployPlugin(File pluginPackage, File pluginXmlFile, File pomFile, boolean isRelease) {
        def (pluginName, pluginVersion) = parsePom(pomFile)
        def url = "$baseUrl/api/v1.0/publish/$pluginName/$pluginVersion"
        println "Publishing to $url"

        def resp = handleAuthentication {
            return rest.post(url) {
                auth username, password
                contentType "multipart/form-data"
                accept "text/plain"
                zip = pluginPackage
                pom = pomFile
                xml = pluginXmlFile
            }
        }

        switch (resp.status) {
        case 200:
            break

        case 401:
            throw new RuntimeException("Repository authentication failed. Do you have an account " +
                    "and are your username and password correct?")
            break

        case 403:
            throw new RuntimeException(resp.text)
            break

        default:
            throw new RuntimeException("Server error deploying to Grails central repository " +
                    "(status ${resp.status}):\n${resp.text}")
        }

        waitForDeploymentStatus pluginName, pluginVersion
    }

    protected waitForDeploymentStatus(String name, String version) {
        for (int i in 0..<20) {
            def resp = rest.get("$baseUrl/api/v1.0/plugin/status/$name/$version").text
            if (resp == "COMPLETED") {
                println "Plugin successfully published."
                return
            }
            if (resp == "FAILED") {
                throw new RuntimeException("Server error deploying to Grails central repository. " +
                        "Please try publishing again. If that doesn't work, contact us on dev@grails.codehaus.org.")
            }

            Thread.sleep 1000
        }

        // Still don't know the status of the plugin...
        throw new RuntimeException("We cannot determine whether the plugin has been published or not. " +
                "Please wait and try publishing with the --ping-only option. If that doesn't work, " +
                "perform a full publish again. If you experience continued problems, please contact us " +
                "on dev@grails.codehaus.org.")
    }

    protected String getBaseUrl() {
        def base = new URI(portalUrl)
        return base.port > -1 ? "http://$base.host:$base.port" : "http://$base.host"
    }

    /**
     * Parses the given POM file (must have a 'text' property) and returns
     * a tuple of the plugin name and version (in that order).
     */
    protected parsePom(pomFile) {
        def pom = new XmlSlurper().parseText(pomFile.text)
        return [pom.artifactId.text(), pom.version.text()]
    }

    /**
     * @param c The closure to execute within the try/catch.
     */
    private handleAuthentication(c, int authCount = 0) {
        def resp = c()
        if (resp.status == 401 && authCount < 3 && askUser) {
            // Only allow three authentication attempts.
            if (username || authCount > 0) {
                System.out.println "Authentication failed - please try again."
            }

            username = askUser("Enter your username: ")
            password = askUser("Enter your password: ")
            resp = handleAuthentication(c, ++authCount)
        }

        return resp
    }
}
