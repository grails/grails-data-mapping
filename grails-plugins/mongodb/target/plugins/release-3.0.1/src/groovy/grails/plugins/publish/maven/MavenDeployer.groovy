package grails.plugins.publish.maven

import grails.plugins.publish.PluginDeployer
import groovy.xml.NamespaceBuilder

/**
 * Implementation of {@link PluginDeployer} that deploys plugin packages
 * to a Maven-compatible repository using the Maven Ant tasks.
 */
class MavenDeployer implements PluginDeployer {
    def ant
    def mavenTasks
    def repoDefn
    def protocol

    MavenDeployer(ant, repoDefinition, protocol) {
        this.ant = ant
        this.mavenTasks = NamespaceBuilder.newInstance(ant, 'antlib:org.apache.maven.artifact.ant')
        this.repoDefn = repoDefinition
        this.protocol = protocol

        // The Ant Maven tasks don't recognise the 'type' argument in
        // the definition of remote repositories.
        repoDefn.args.remove("type")
    }

    /**
     * Checks whether the Subversion tag exists for the plugin and version
     * described by the given POM file.
     * @param pomFile The plugin's POM file.
     */
    boolean isVersionAlreadyPublished(File pomFile) {
        def url = buildPomUrl(pomFile)

        // Very quick, ugly hack :)
        try { new URL(url).text; return true }
        catch (e) { return false }
    }

    /**
     * Deploys the given plugin package to the Maven repository configured
     * at object instantiation.
     * @param pluginPackage The location of the plugin zip file.
     * @param pluginXmlFile The location of the XML plugin descriptor.
     * @param pomFile The location of the POM (pom.xml).
     * @param isRelease Ignored by the Maven deployer, since Maven determines
     * whether it's a release based on whether the artifact has a "-SNAPSHOT"
     * extension or not.
     */
    void deployPlugin(File pluginPackage, File pluginXmlFile, File pomFile, boolean isRelease) {
        mavenTasks.'install-provider'(artifactId: protocol, version: "1.0-beta-2")
        mavenTasks.deploy(file: pluginPackage) {
            attach file: pluginXmlFile, type:"xml", classifier: "plugin"
            pom(file: pomFile)

            if (repoDefn.configurer) {
                remoteRepository(repoDefn.args, repoDefn.configurer)
            }
            else {
                remoteRepository(repoDefn.args)
            }
        }
    }

    /**
     * Parses the given POM file (must have a 'text' property) and returns
     * a tuple of the plugin name and version (in that order).
     */
    protected final buildPomUrl(pomFile) {
        def pom = new XmlSlurper().parseText(pomFile.text)
        def url = new StringBuilder(repoDefn.args["url"].toString())
        url << '/' << pom.groupId.text().replace('.', '/')
        url << '/' << pom.artifactId.text()
        url << '/' << pom.version.text()
        url << '/' << "${pom.artifactId.text()}-${pom.version.text()}.pom"
        return url.toString()
    }
}
