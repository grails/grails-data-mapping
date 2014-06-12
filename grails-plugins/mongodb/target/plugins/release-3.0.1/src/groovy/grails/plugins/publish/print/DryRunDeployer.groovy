package grails.plugins.publish.print

import grails.plugins.publish.PluginDeployer

/**
 * Dummy implementation of {@link PluginDeployer} that simply echoes
 * to a target information about what files are being deployed. By
 * default, the target is stdout.
 */
class DryRunDeployer implements PluginDeployer {
    def output = System.out

    boolean isVersionAlreadyPublished(File pomFile) {
        return false
    }

    void deployPlugin(File pluginPackage, File pluginXmlFile, File pomFile, boolean isRelease) {
        def out = (output instanceof PrintStream) ? output : new PrintWriter(new OutputStreamWriter(output))
        out.println """\
Deploying the plugin package ${pluginPackage.canonicalPath}
with plugin descriptor ${pluginXmlFile.canonicalPath}
and POM file ${pomFile.canonicalPath}

This is${ !isRelease ? ' not' : ''} a release version"""
    }
}
