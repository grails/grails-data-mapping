import org.codehaus.groovy.grails.cli.CommandLineHelper

includeTargets << grailsScript("_GrailsPluginDev")
includeTargets << new File(releasePluginDir, "scripts/_GrailsMaven.groovy")

USAGE = """
    publish-plugin [--repository=REPO] [--protocol=PROTOCOL] [--portal=PORTAL] [--dry-run] [--snapshot] [--scm] [--no-scm] [--message=MESSAGE] [--no-message] [--ping-only]

where
    REPO     = The name of a configured repository to deploy the plugin to. Can be
               a Subversion repository or a Maven-compatible one.
               (default: Grails Central Plugin Repository).

    PROTOCOL = The protocol to use when deploying to a Maven-compatible
               repository. Can be one of 'http', 'scp', 'scpexe', 'ftp', or
               'webdav'. (default: 'http').

    PORTAL   = The portal to inform of the plugin's release.
               (default: Grails Plugin Portal).

    MESSAGE  = Commit message to use when committing source changes using your
               SCM provider.

    --dry-run      = Shows you what will happen when you publish the plugin,
                     but doesn't actually publish it.

    --snapshot     = Force this release to be a snapshot version, i.e. it isn't
                     automatically made the latest available release.

    --scm          = Enables source control management for this release.

    --no-scm       = Disables source control management for this release.


    --no-message   = Commit using just the default message.

    --no-overwrite = Don't fail if this plugin has already been published.
                     This is useful if this plugin is being published from a
                     continuous integration server and you don't want the
                     command to exit with failure.

    --allow-overwrite = Allow any existing plugin to be overwritten.

    --ping-only    = Don't publish/deploy the plugin, only send a notification
                     to the plugin portal. This is useful if portal
                     notification failed during a previous attempt to publish
                     the plugin. Mutually exclusive with the --dry-run option.

    --binary       = Release as a binary plugin.
"""

scmProvider = null
scmHost = null

target(default: "Publishes a plugin to either a Subversion or Maven repository.") {
    depends(parseArguments, checkGrailsVersion, packagePlugin, processDefinitions, generatePom)

    // Handle old names for options. Trying to be consistent with Grails 2.0 conventions.
    if (argsMap["dryRun"]) { argsMap["dry-run"] = true }
    if (argsMap["noScm"]) { argsMap["no-scm"] = true }
    if (argsMap["noMessage"]) { argsMap["no-message"] = true }
    if (argsMap["pingOnly"]) { argsMap["ping-only"] = true }
    if (argsMap["noOverwrite"]) { argsMap["no-overwrite"] = true }
    if (argsMap["allowOverwrite"]) { argsMap["allow-overwrite"] = true }
    if (argsMap["promptAuth"]) { argsMap["prompt-auth"] = true }

    // Read the plugin information from the POM.
    pluginInfo = new XmlSlurper().parse(new File(pomFileLocation))
    isRelease = !pluginInfo.version.text().endsWith("-SNAPSHOT")
    if (argsMap["snapshot"]) isRelease = false

    pluginInfo = [
            name : pluginInfo.artifactId.text(),
            group : pluginInfo.groupId.text(),
            version : pluginInfo.version.text(),
            isSnapshot : !isRelease ]
    event "PublishPluginStart", [ pluginInfo ]

    // Is source control management enabled for this run?
    boolean scmEnabled = Boolean.valueOf(getPropertyValue("grails.release.scm.enabled", true))
    if (argsMap["scm"]) scmEnabled = true
    if (argsMap["no-scm"]) scmEnabled = false

    if (scmEnabled) {
        final inputHelper = new CommandLineHelper()
        final interactive = new Expando(
                out: System.out,
                askUser: { msg ->
                    // This closure is executed whenever the SCM needs to
                    // ask for user input.
                    return userInput(
                            inputHelper,
                            msg,
                            "SCM requires an answer to \"${msg}\", but you are running in non-interactive mode.")
                })

        // Load any SCM provider that may be installed.
        event "InitScm", [grailsSettings.baseDir, interactive]

        if (!scmProvider) {
            println "WARN: No SCM provider installed."
        }
    }

    // If SCM is enabled and a provider available, make sure the plugin
    // source is under source control and that the latest code is committed
    // and tagged.
    if (scmProvider) {
        processScm scmProvider
    }

    // Use the Grails Central Plugin repository as the default.
    def repoClass = classLoader.loadClass("grails.plugins.publish.Repository")
    def repo = repoClass.grailsCentral

    // Add the Grails Central portal to the distribution info under the
    // ID 'grailsCentral'. 'distributionInfo' is created by the processDefinitions
    // target and contains the configuration for all the declared repositories
    // and portals.
    def grailsCentralPortal = distributionInfo.portals["grailsCentral"]
    if (!grailsCentralPortal) {
        grailsCentralPortal = [ url: repoClass.GRAILS_CENTRAL_PORTAL_URL ]
        distributionInfo.portals["grailsCentral"] = grailsCentralPortal
    }
    else {
        if (grailsCentralPortal["url"]) {
            println "WARN: You cannot change the URL for the 'grailsCentral' portal - ignoring the user-specified value"
        }
        grailsCentralPortal["url"] = repoClass.GRAILS_CENTRAL_PORTAL_URL
    }

    // Is a default repository configured for this project? If yes, use that
    // unless a '--repository' command line option is specified.
    def repoName = argsMap["repository"] ?: grailsSettings.config.grails.project.repos.default
    def type = "grailsCentral"

    if (repoName && repoName != "grailsCentral") {
        // First look for the repository definition for this name. This
        // could either be from the newer Maven-based definitions or the
        // legacy Subversion-based ones.
        def repoDefn = distributionInfo.remoteRepos[repoName]
        def defaultPortal = null
        def url
        if (repoDefn) {
            type = repoDefn.args["type"] ?: "maven"
            url = repoDefn.args["url"]

            // If the repository defines a portal, then we should use that
            // ahead of the public Grails plugin portal.
            defaultPortal = repoDefn.args["portal"]
        }
        else {
            // Handle legacy Subversion repository definitions.
            type = "svn"
            url = grailsSettings.config.grails.plugin.repos.distribution."$repoName"
        }

        // Check that the repository is defined.
        if (url) {
            repo = repoClass.newInstance(
                    repoName,
                    new URI(url),
                    defaultPortal ?: null)
            def repoNames = [svn: "Subversion", maven: "Maven", grailsCentral: "Grails"]
            println "Publishing to ${repoNames[type]} repository '$repoName'"
        }
        else {
            println "No configuration found for repository '$repoName'"
            exit 1
        }
    }
    else {
        println "Publishing to Grails Central"
    }

    def deployer
    if (argsMap["dry-run"]) {
        def retval = processAuthConfig.call(repo.name) { username, password ->
            if (username) {
                println "Using configured username and password from grails.project.repos.${repo.name}"
            }
        }

        if (retval) return retval

        deployer = classLoader.loadClass("grails.plugins.publish.print.DryRunDeployer").newInstance()
    }
    else if (type == "svn") {
        // Helper class for getting user input from the command line.
        def inputHelper = new CommandLineHelper()

        // If the username and password are declared in the standard configuration,
        // grails.project.repos.<repo>.username/password, then pick them out now
        // and set them on the SvnClient instance.
        def uri = repo.uri
        def svnClient = classLoader.loadClass("grails.plugin.svn.SvnClient").newInstance(uri.toString())
        def retval = processAuthConfig.call(repo.name) { username, password ->
            if (username) {
                if (uri.userInfo) {
                    println "WARN: username and password defined in config and in repository URI - using the credentials from the URI."
                }
                else {
                    svnClient.setCredentials(username, password)
                }
            }
        }

        if (retval) return retval

        // Create a deployer for Subversion
        def masterPluginList = classLoader.loadClass("grails.plugins.publish.svn.MasterPluginList").newInstance(
                svnClient,
                repo.name,
                new File(grailsSettings.projectWorkDir, ".plugin-meta"),
                System.out,
                false)

        deployer = classLoader.loadClass("grails.plugins.publish.svn.SvnDeployer").newInstance(
                svnClient,
                grailsSettings.projectWorkDir,
                repo.name,
                masterPluginList,
                System.out) { msg ->
            // This closure is executed whenever the deployer needs to
            // ask for user input.
            return userInput(
                    inputHelper,
                    msg,
                    "SvnDeployer requires an answer to \"${msg}\", but you are running in non-interactive mode.")
        }
    }
    else if (type == "grailsCentral") {
        deployer = classLoader.loadClass("grails.plugins.publish.portal.GrailsCentralDeployer").newInstance() { msg ->
            // This closure is executed whenever the deployer needs to
            // ask for user input.
            return userInput(
                    new CommandLineHelper(),
                    msg,
                    "GrailsCentralDeployer requires an answer to \"${msg}\", but you are running in non-interactive mode.")
        }

        def retval = processAuthConfig.call(repo.name) { username, password ->
            if (username) {
                deployer.username = username
                deployer.password = password

                def gcp = distributionInfo.portals["grailsCentral"]
                if(gcp && !gcp?.username) {
                    gcp.username = username
                    gcp.password = password

                }
            }
        }

        if (retval) return retval
        def uri = repo?.uri?.toString()
        if (uri) {
            deployer.portalUrl = uri
        }
    }
    else if (type == "maven") {
        // Work out the protocol to use. This may be provided as a
        // '--protocol' argument on the command line or inferred from
        // the repository URL.
        def protocols = [
                http: "wagon-http",
                scp: "wagon-ssh",
                scpexe: "wagon-ssh-external",
                ftp: "wagon-ftp",
                webdav: "wagon-webdav" ]
        def protocol = protocols.http

        def repoDefn = distributionInfo.remoteRepos[repoName]
        repoDefn.args.remove "portal"

        // Add a configurer to the repository definition if the username and password
        // have been defined.
        def projectConfig = grailsSettings.config.grails.project
        def retval = processAuthConfig.call(repo.name) { username, password ->
            if (username) {
                if (projectConfig.repos."${repo.name}".custom) {
                    println "WARN: username and password defined in config as well as a 'custom' entry - ignoring the provided username and password."
                }
                else {
                    println "Using configured username and password from grails.project.repos.${repo.name}"
                    repoDefn.configurer = { authentication username: username, password: password }
                    repoDefn.args.remove "username"
                    repoDefn.args.remove "password"
                }
            }
        }

        if (retval) return retval

        if (argsMap["protocol"]) {
            protocol = protocols[argsMap["protocol"]]
        }
        else if (repo.uri) {
            if (!repo.uri.scheme) {
                println "Invalid URL for repository '${repo.name}': ${repo.uri}"
                exit 1
                return 1
            }

            if (protocols[repo.uri.scheme]) {
                protocol = protocols[repo.uri.scheme]
            }
            else {
                println "WARNING: unknown protocol '${repo.uri.scheme}' for repository '${repo.name}'"
            }
        }

        if (argsMap["prompt-auth"]) {
            def inputHelper = new CommandLineHelper()
            username = usernput(
                    inputHelper,
                    "Username for repository: ",
                    "You haven't configured the plugin repository username - required in non-interactive mode")
            password = userInput(
                    inputHelper,
                    "Password for repository: ",
                    "You haven't configured the plugin repository password - required in non-interactive mode")

            repoDfn.configurer = { authentication username: username, password: password }
        }

        deployer = classLoader.loadClass("grails.plugins.publish.maven.MavenDeployer").newInstance(ant, repoDefn, protocol)
    }
    else {
        println "Unknown type '$type' defined for repository '$repoName'"
        exit 1
    }

    if (!argsMap["ping-only"]) {
        event "DeployPluginStart", [ pluginInfo, pluginZip, pomFileLocation ]

        def pomFile = pomFileLocation as File
        if (deployer.isVersionAlreadyPublished(pomFile)) {
			if (argsMap["no-overwrite"]) {
				println "This version of the plugin has already been published."
				event "StatusFinal", ["Plugin publication cancelled with clean exit."]
				exit(0)
			} else {
				if (argsMap["allow-overwrite"]) {
					println "This version of the plugin has already been published, it will be overwritten."
				} else if (isInteractive) {
					def inputHelper = new CommandLineHelper()
					def answer = userInput(
							inputHelper,
							"This version has already been published. Do you want to replace it " +
								"(not recommended except for snapshots)? (y,N) ",
							"This version of the plugin has already been published.")
					if (!answer?.equalsIgnoreCase("y")) {
						event "StatusFinal", ["Plugin publication cancelled."]
						exit(1)
					}
				} else {
					println "This version of the plugin has already been published"
					println "Use the --allow-overwrite option to publish the plugin."
					event "StatusFinal", ["Plugin publication cancelled."]
					exit(1)
				}
            }
        }

        try {
            deployer.deployPlugin(pluginZip as File, new File(basedir, "plugin.xml"), new File(pomFileLocation), isRelease)
        }
        catch (Exception ex) {
            event "StatusError", ["Failed to publish plugin: ${ex.message}"]
            exit 1
        }

        event "DeployPluginEnd", [ pluginInfo, pluginZip, pomFileLocation ]
    }

    // What's the URL of the portal to ping? The explicit 'portal' argument
    // takes precedence, then the portal configured for the current repository,
    // and finally the public Grails plugin portal.
    def portalName = argsMap["portal"] ?: repo.defaultPortal
    def portalDefn = null
    if (portalName) {
        // Pick the configured portal with the given name, assuming one
        // exists with that name.
        portalDefn = distributionInfo.portals[portalName]

        if (!portalDefn?.url) {
            println "No portal defined with ID '${portalName}'"
            println "Plugin has been published, but the plugin portal has not been notified."
            exit 1
        }
    }
    else {
        // We don't ping the grails.org portal if a repository has been specified
        // but that repository has no default portal configured.
        println "No default portal defined for repository '${repoName}' - skipping portal notification"
        event "PublishPluginEnd", [ pluginInfo ]
        return
    }

    // Add the plugin name to the URL, making sure first that the base portal URI
    // ends with '/'. Otherwise the resolve won't do what we want.
    def portalUrl = new URI(portalDefn.url)
    if (!portalUrl.path.endsWith("/")) portalUrl = new URI(portalUrl.toString() + "/")
    portalUrl = portalUrl.resolve(pluginInfo.name)

    // Now that we have a URL, simply send a PUT request with the appropriate
    // JSON content.
    println "Notifying plugin portal '${portalUrl}' of release..."

    if (!argsMap["dry-run"]) {
        event "PingPortalStart", [ pluginInfo, portalUrl, repo.uri.toString() ]

        def username = portalDefn.username
        def password = portalDefn.password

        if (!username) {
            def inputHelper = new CommandLineHelper()
            username = userInput(
                    inputHelper,
                    "Username for portal (leave empty if authentication not required): ",
                    "You haven't configured the plugin portal username - required in non-interactive mode")
            password = userInput(
                    inputHelper,
                    "Password for portal (leave empty if authentication not required): ",
                    "You haven't configured the plugin portal password - required in non-interactive mode")
        }

        def converterConfig = new org.codehaus.groovy.grails.web.converters.configuration.ConvertersConfigurationInitializer()
        converterConfig.initialize(grailsApp)
        def rest = classLoader.loadClass("grails.plugins.rest.client.RestBuilder").newInstance()
		def jsonParams = pluginInfo + [ url : repo.uri.toString() ]
        def resp = rest.put(portalUrl.toString()) {
            auth username, password
            json({ jsonParams })
        }
        switch(resp.status) {
            case 401:
                println "ERROR: Portal authentication failed. Are your username and password correct?"; break
            case 403:
                println "ERROR: You do not have permission to update the plugin portal."; break

            default:

                if(resp.class.name == "grails.plugins.rest.client.ErrorResponse") {
                    println "ERROR: Notification failed - status ${resp.status} - ${resp.json.message}"
                }
                else {
                    println "Notification successful"
                }
        }

        event "PingPortalEnd", [ pluginInfo, portalUrl, repo.uri.toString() ]
    }

    event "PublishPluginEnd", [ pluginInfo ]
}

private userInput(inputHelper, msg, nonInteractiveErrorMsg) {
    if (!isInteractive) {
        event "StatusError", [nonInteractiveErrorMsg]
        event "StatusFinal", ["Plugin publication cancelled."]
        exit 1
        return ""
    }
    else {
        return requiresSecureInput(msg) ? secureUserInput(inputHelper, msg) : inputHelper.userInput(msg)
    }
}

private secureUserInput(inputHelper, msg) {
    if (binding.variables.containsKey("grailsConsole")) {
        try {
            if (grailsConsole.metaClass.respondsTo(grailsConsole, "secureUserInput", msg)) {
                return grailsConsole.secureUserInput(msg)
            }
            else {
                return grailsConsole.reader.readLine(msg, new Character("*" as char))
            }
        }
        catch (ClassNotFoundException e) {
            return inputHelper.userInput(msg)
        }
    }
    else {
        return inputHelper.userInput(msg)
    }
}

private requiresSecureInput(msg) { msg.toLowerCase().contains("password") }

private processScm(scm) {
    // Configure authentication for the SCM provider if credentials provided
    // in build settings.
    def scmConfig = grailsSettings.config.grails.project.scm
    if (scmConfig.username) {
        scm.auth scmConfig.username, scmConfig.password
    }

    // Find out if the user wants to add any extra text to the standard
    // commit message.
    def inputHelper = new CommandLineHelper()
    def msg = argsMap["message"] ?: (argsMap["no-message"] || !isInteractive || argsMap["ping-only"] ?
            "" : inputHelper.userInput("Enter extra commit message text for this release (optional): "))
    if (msg) msg = "\n\n" + msg

    if (!scm.managed) {
        // Ignore SCM import if in non-interactive mode.
        if (!isInteractive) return

        // The project isn't under source control, so import it into the user's
        // preferred SCM system - unless the user explicitly doesn't want it added
        // to source control.
        def answer = inputHelper.userInput("Project is not under source control. Do you want to import it now? (Y,n) ")
        if (answer?.equalsIgnoreCase("n")) {
            return
        }

        scmImportProject(scm, inputHelper, msg)
    }
    else {
        // First check for any untracked files in the project. We don't want any
        // files accidentally missed from the commit! If there are some, we won't
        // allow a commit unless they are tracked or added to the project's ignores.
        //
        // TODO Allow the user to add each file to source control or ignores and
        // then continue with the commit. The user should also have the option of
        // cancelling without making any changes.
        if (scm.unmanagedFiles) {
            event "StatusError", ["You have untracked files. Please add them to source control or the ignore list before publishing the plugin."]
            event "StatusFinal", ["Plugin publication cancelled."]
            exit 1
            return
        }

        // Is the current code up to date? If not, we shouldn't commit and release.
        // TODO Support doing an update right here, right now.
        if (!scm.upToDate()) {
            event "StatusError", ["Your local source is not up to date. Please update it before publishing the plugin."]
            event "StatusFinal", ["Plugin publication cancelled."]
            exit 1
            return
        }

        def version = pluginInfo.version

        scm.commit "Releasing version ${version} of ${pluginInfo.name} plugin.${msg}"
        if (isRelease) scm.tag "v${version}", "Tagging the ${version} version of the plugin source."
        scm.synchronize()
    }
}

private scmImportProject(scm, inputHelper, msg) {
    // Get a URL for the repository to import this project into. The developer may
    // want to use the default Grails plugin source repository, which requires the
    // Subversion plugin. Alternatively, it could be another host such as GitHub or
    // Google Code. Finally, no URL may be given at all. This can make sense for
    // distributed version control systems in which you have a local copy of the
    // repository.
    def hostUrl = null
    if (!scmHost && pluginManager.hasGrailsPlugin("svn")) {
        def answer = inputHelper.userInput("Would you like to add this plugin's source to the Grails plugin source repository? (Y,n) ")
        if (answer?.equalsIgnoreCase("y")) {
            hostUrl = "https://svn.codehaus.org/grails-plugins/grails-${pluginInfo.name}"
        }
    }

    if (!hostUrl) {
        hostUrl = inputHelper.userInput("Please enter the URL of the remote SCM repository: ")
    }

    scmProvider.importIntoRepo hostUrl, "Initial import of plugin source code for the release of version ${pluginInfo.version}.${msg}"
}
