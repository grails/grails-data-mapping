package grails.plugins.publish;

import java.io.File;

public interface PluginDeployer {
    boolean isVersionAlreadyPublished(File pomFile);
    void deployPlugin(File pluginPackage, File pluginXmlFile, File pomFile, boolean isRelease);
}
