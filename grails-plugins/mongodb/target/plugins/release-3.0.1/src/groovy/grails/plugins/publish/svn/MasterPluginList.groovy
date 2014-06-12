package grails.plugins.publish.svn

import org.codehaus.groovy.grails.plugins.publishing.DefaultPluginPublisher
import org.springframework.core.io.FileSystemResource

/**
 * This class allows you to interact with legacy master plugin lists that are
 * hosted on Subversion-based Grails plugin repositories.
 */
class MasterPluginList {
    static final String REMOTE_META_DIR = ".plugin-meta"
    static final String PLUGINS_LIST_NAME = "plugins-list.xml"

    final svnClient
    final metaDir
    final out
    final verbose

    private final checkoutDir

    /**
     * Creates a new instance for interacting with a single remote master plugin
     * list.
     * @param svnClient (SvnClient) An SvnClient instance for interacting with
     * the remote repository.
     * @param repoName (String) The logical name to use for the repository. The
     * actual name is unimportant as long as it's unique to the repository.
     * @param metaDir (File) Where to copy the remote repository information to -
     * must be a directory.
     * @param out Messages are printed to this stream via print() and println().
     * @param verbose More messages are printed if this is <code>true</code>.
     */
    MasterPluginList(
            svnClient,
            repoName = "grailsCentral",
            metaDir = new File(".plugin-meta"),
            out = System.out,
            boolean verbose = false) {
        this.svnClient = svnClient
        this.metaDir = metaDir
        this.out = out
        this.verbose = verbose

        checkoutDir = new File(metaDir, repoName)
    }

    /**
     * Updates the remote master plugin list with the latest information about a
     * given plugin.
     * @param pluginName (String) The name of the plugin.
     * @param pluginXml (File) Location of the plugin's XML descriptor.
     * @param skipLatest If <code>true</code> the current plugin version is
     * <em>not</em> marked as the latest.
     * @param msg The commit message to use when committing the master plugin list
     * changes to the remote repository.
     * @param maxAttempts The maximum number of times this method should attempt
     * to commit the changes. This is because someone else may have updated the
     * master plugin list inbetween fetching the latest version and then committing
     * the changes. Defaults to 3.
     */
    def update(pluginName, pluginXml, skipLatest, msg, maxAttempts = 3) {
        // Check whether the repository already has a master plugin list.
        def pluginsListFile
        def remotePath = "$REMOTE_META_DIR/$PLUGINS_LIST_NAME"
        if (!svnClient.pathExists(remotePath)) {
            // We need to add an initial plugin list to the repository.
            pluginsListFile = createPluginList(REMOTE_META_DIR)
        }
        else {
            // Grab the remote master plugin list and use that.
            pluginsListFile = fetchRemoteList(REMOTE_META_DIR)
        }

        // Generate a new plugin list by inserting the latest information for
        // the given plugin into the existing plugin list.
        def hasBaseDirArg = DefaultPluginPublisher.declaredConstructors.any {
            it.parameterTypes.size() == 3 && it.parameterTypes[0] == File
        }

        def publisher
        if (hasBaseDirArg) {
            publisher = new DefaultPluginPublisher(
                    pluginXml.parentFile,
                    svnClient.latestRevision.toString(),
                    svnClient.repoUrl.toString())
        }
        else {
            publisher = new DefaultPluginPublisher(
                    svnClient.latestRevision.toString(),
                    svnClient.repoUrl.toString())
        }

        def updatedList = publisher.publishRelease(pluginName, new FileSystemResource(pluginsListFile), !skipLatest)

        // Now commit the changes to disk by overwriting the existing plugin list.
        pluginsListFile.withWriter("UTF-8") { w ->
            publisher.writePluginList(updatedList, w)
        }

        // We're now in a position to commit the modified plugin list to the
        // remote repository!
        try {
            svnClient.commit(checkoutDir, msg)
        }
        catch (ex) {
            // Some SVN errors are recoverable. For example, the local copy of
            // the plugin list may be out of date, in which case we can update
            // it and try again. But don't keep trying for ever!
            if (maxAttempts < 2) {
                throw ex
            }
            else {
                // Try again.
                update(pluginName, skipLatest, msg, maxAttempts - 1)
            }
        }

        out.println "Committed the updated master plugin list"
    }

    /**
     * Creates a new master plugin list locally and performs an 'svn add' on
     * it so that when the changes are committed, the new list is pushed to
     * the remote repository. The assumption here is that the remote repository
     * does not have a master plugin list at this point.
     */
    private createPluginList(remotePath) {
        // First, make sure that the directory that will contain the master plugin
        // list exists in the repository, since we'll want to check it out.
        out.println "Creating new master plugin list for ${svnClient.repoUrl}/${remotePath}"

        if (!svnClient.pathExists(remotePath)) {
            svnClient.createPath(remotePath, "Adding $remotePath to the repository.")
        }

        // Perform the check out on the remote path.
        checkoutDir.mkdirs()
        svnClient.checkOut(checkoutDir, remotePath)

        // Create an initial master plugin list and add it via the working copy.
        // This file will later be modified and committed.
        def localListFile = new File(checkoutDir, PLUGINS_LIST_NAME)
        localListFile << """\
            <?xml version="1.0" encoding="UTF-8"?>
            <plugins revision="0">
            </plugins>
            """.stripIndent()
        svnClient.addFilesToSvn([ localListFile ])

        return localListFile
    }

    /**
     * Fetches the master plugin list from the remote repository. This is done
     * either through an svn update (if the file has already been checked out)
     * or an svn checkout.
     */
    private fetchRemoteList(remotePath) {
        out.println "Fetching remote master plugin list from ${svnClient.repoUrl}/${remotePath}"

        def doCheckout = true
        if (checkoutDir.exists()) {
            try {
                // Try to update the existing working copy.
                svnClient.update(checkoutDir)

                // Update successful, so we don't have to do a check out.
                doCheckout = false
            }
            catch (ex) {
                // Fall back to doing a check out.
                if (verbose) println "Failed to update master plugin list working copy: ${ex.message}"
            }
        }

        if (doCheckout) {
            svnClient.checkOut(checkoutDir, remotePath)
        }

        return new File(checkoutDir, PLUGINS_LIST_NAME)
    }
}
