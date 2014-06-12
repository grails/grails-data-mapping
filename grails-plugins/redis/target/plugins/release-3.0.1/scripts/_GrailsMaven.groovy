/*
 * Copyright 2004-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import grails.util.BuildScope
import groovy.xml.NamespaceBuilder
import groovy.xml.MarkupBuilder

import org.apache.ivy.util.ChecksumHelper
import org.codehaus.groovy.grails.cli.CommandLineHelper

scriptScope = BuildScope.WAR
scriptEnv = "production"

includeTargets << grailsScript("_GrailsPackage")

// Open source licences.
globalLicenses = [
    // General, permissive "copyfree" licenses
    APACHE:    [ name: "Apache License 2.0", url: "http://www.apache.org/licenses/LICENSE-2.0.txt" ],
    BSD2:      [ name: "Simplified BSD License (2 Clause)", url: "http://opensource.org/licenses/BSD-2-Clause"],
    BSD3:      [ name: "New BSD License (3 Clause)", url: "http://opensource.org/licenses/BSD-3-Clause"],
    MIT:       [ name: "MIT License", url: "http://opensource.org/licenses/MIT"],
    //GNU Family
    GPL2:      [ name: "GNU General Public License 2", url: "http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt"],
    GPL3:      [ name: "GNU General Public License 3", url: "http://www.gnu.org/licenses/gpl.txt"],
    AGPL3:     [ name: "GNU Affero General Public License 3", url: "http://www.gnu.org/licenses/agpl-3.0.html"],
    'LGPL2.1': [ name: "GNU Lesser General Public License 2.1", url: "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html"],
    LGPL3:     [ name: "GNU Lesser General Public License 3", url: "http://www.gnu.org/licenses/lgpl.html"],
    // Other
    EPL1:       [ name: "Eclipse Public License v1.0", url: "http://opensource.org/licenses/EPL-1.0"],
    MPL2:       [ name: "Mozilla Public License v2.0", url: "http://opensource.org/licenses/MPL-2.0"],
]

artifact = NamespaceBuilder.newInstance(ant, 'antlib:org.apache.maven.artifact.ant')

target(mavenInstall:"Installs a plugin or application into your local Maven cache") {
    depends(init)
    def deployFile = isPlugin ? new File(pluginZip) : grailsSettings.projectWarFile
    def ext = isPlugin ? deployFile.name[-3..-1] : "war"
    installOrDeploy(deployFile, ext, false, [local:distributionInfo.localRepo])
}

target(mavenDeploy:"Deploys the plugin to a Maven repository") {
    depends(init)
    def protocols = [http:   "wagon-http",
                     scp:    "wagon-ssh",
                     scpexe: "wagon-ssh-external",
                     ftp:    "wagon-ftp",
                     webdav: "wagon-webdav" ]

    def protocol = protocols.http
    def repoName = argsMap.repository ?: grailsSettings.config.grails.project.repos.default
    def repo = repoName ? distributionInfo.remoteRepos[repoName] : null
    if (argsMap.protocol) {
        protocol = protocols[argsMap.protocol]
    }
    else if (repo) {
        def url = repo?.args?.url
        if (url) {
            def i = url.indexOf('://')
            def urlProt = url[0..i-1]
            protocol = protocols[urlProt] ?: protocol
        }
    }

    def retval = processAuthConfig.call(repoName) { username, password ->
        if (username) {
            def projectConfig = grailsSettings.config.grails.project
            if (projectConfig.repos."${repoName}".custom) {
                event "StatusError", ["Warning: Username and password defined in config as well as a 'custom' entry - ignoring the provided username and password."]
            }
            else {
                event "StatusUpdate", ["Using configured username and password from grails.project.repos.${repoName}"]
                repo.configurer = { authentication username: username, password: password }
                repo.args.remove "username"
                repo.args.remove "password"
            }
        }
    }

    if (retval) exit retval

    artifact.'install-provider'(artifactId:protocol, version:"1.0-beta-2")

    def deployFile = isPlugin ? new File(pluginZip) : grailsSettings.projectWarFile
    def ext = isPlugin ? deployFile.name[-3..-1] : "war"
    try {
        installOrDeploy(deployFile, ext, true, [remote:repo, local:distributionInfo.localRepo])
    }
    catch(e) {
        event "StatusError", ["Error deploying artifact: ${e.message}"]
        event "StatusError", ["Have you specified a configured repository to deploy to (--repository argument) or specified distributionManagement in your POM?"]
        exit 1
    }
}

target(init: "Initialisation for maven deploy/install") {
    depends(checkGrailsVersion, packageApp, processDefinitions)

    isPlugin = pluginManager?.allPlugins?.any { it.basePlugin }

    if (!isPlugin) {
        includeTargets << grailsScript("_GrailsWar")
        war()
    }

    generatePom()
}

target(processDefinitions: "Reads the repository definition configuration.") {
    def projectConfig = grailsSettings.config.grails.project
    distributionInfo = classLoader.loadClass("grails.plugins.publish.DistributionManagementInfo").newInstance()

    if (projectConfig.dependency.distribution instanceof Closure) {
        // Deal with the DSL form of configuration, which is the old approach.
        def callable = grailsSettings.config.grails.project.dependency.distribution?.clone()
        callable.delegate = distributionInfo
        callable.resolveStrategy = Closure.DELEGATE_FIRST
        try {
            callable.call()
        }
        catch (e) {
            event "StatusError", ["Error reading dependency distribution settings: ${e.message}"]
            exit 1
        }
    }
    else if (projectConfig.repos || projectConfig.portal) {
        // Handle standard configuration.
        for (entry in projectConfig.portal) {
            // Add this portal to the distribution info. The key is the portal ID
            // while the value is a map of options that must include 'url'.
            distributionInfo.portals[entry.key] = entry.value
        }

        for (entry in projectConfig.repos) {
            // Skip 'default' since that's used to specify the name of the default
            // repository to use for a project.
            if (entry.key == "default") continue

            // Add this repository to the distribution info. The key is the repository
            // ID while the value is a map containing the repository configuration.
            def props = entry.value + [id: entry.key]
            def c = props.remove("custom")
            distributionInfo.remoteRepos[entry.key] = new Expando(args: props, configurer: c)
        }

        distributionInfo.localRepo = projectConfig.mavenCache ?: null
    }
}

target(generatePom: "Generates a pom.xml file for the current project unless './pom.xml' exists.") {
    depends(packageApp)

    // Get hold of the plugin instance for this plugin if it's a plugin
    // project. If it isn't, then these variables will be null.
    def plugin = pluginManager?.allPlugins?.find { it.basePlugin }
    def pluginInstance = plugin?.pluginClass?.newInstance()

    if (plugin) {
        includeTargets << grailsScript("_GrailsPluginDev")
        packagePlugin()

        // This script variable doesn't exist pre-Grails 2.0
        if (!binding.variables.containsKey("pluginInfo")) {
            pluginInfo = pluginSettings.getPluginInfo(basedir)
        }

        if (!plugin.version) {
            event "StatusError", ["Cannot generate POM: no version specified for this plugin"]
            exit 1
        }
    }

    pomFileLocation = "${grailsSettings.projectTargetDir}/pom.xml"
    basePom = new File(basedir, "pom.xml")

    if (basePom.exists()) {
        def forcePomGeneration = argsMap.forcePomGeneration ?: false
        if (forcePomGeneration) {
            event("StatusUpdate", ["Forcing POM generation, ignoring 'pom.xml' in the root of the project."])
        } else {
            pomFileLocation = basePom.absolutePath
            event("StatusUpdate", ["Skipping POM generation because 'pom.xml' exists in the root of the project."])
            return 1
        }
    }

    event("StatusUpdate", ["Generating POM file..."])
    new File(pomFileLocation).withWriter('UTF-8') { w ->
        def xml = new MarkupBuilder(w)

        xml.mkp.pi xml: [version: "1.0", encoding: "UTF-8"]
        xml.project(xmlns: "http://maven.apache.org/POM/4.0.0",
                'xmlns:xsi': "http://www.w3.org/2001/XMLSchema-instance",
                'xsi:schemaLocation': "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd") {
            modelVersion "4.0.0"
            if (plugin) {
                def group = "org.grails.plugins"
                if (getOptionalProperty(pluginInstance, 'group')) {
                    group = pluginInstance.group
                }
                else if (getOptionalProperty(pluginInstance, 'groupId')) {
                    group = pluginInstance.groupId
                }

                groupId group
                artifactId plugin.fileSystemShortName
                packaging pluginInfo.packaging == "binary" ? "jar" : "zip"
                version plugin.version

                // I think description() and url() resolve against the AntBuilder
                // by default, so we have to call them explicitly on the MarkupBuilder.
                if (getOptionalProperty(pluginInstance, "title")) name pluginInstance.title
                if (getOptionalProperty(pluginInstance, "description")) delegate.description pluginInstance.description
                if (getOptionalProperty(pluginInstance, "documentation")) delegate.url pluginInstance.documentation
                if (getOptionalProperty(pluginInstance, "license")) {
                    def l = null
                    if (pluginInstance.license instanceof Map) {
                        l = pluginInstance.license
                    }
                    else {
                        l = globalLicenses[pluginInstance.license]
                    }
                    if (l) {
                        licenses {
                            license {
                                name l.name
                                delegate.url l.url
                            }
                        }
                    }
                    else {
                        event("StatusUpdate", [ "Unknown license: ${pluginInstance.license}" ])
                    }
                }
                if (getOptionalProperty(pluginInstance, "organization")) {
                    organization {
                        name pluginInstance.organization.name
                        delegate.url pluginInstance.organization.url
                    }
                }

                // Handle the developers
                def devs = []
                if (getOptionalProperty(pluginInstance, "author")) {
                    def author = [ name: pluginInstance.author ]
                    if (getOptionalProperty(pluginInstance, "authorEmail")) {
                        author["email"] = pluginInstance.authorEmail
                    }

                    devs << author
                }
                if (getOptionalProperty(pluginInstance, "developers")) {
                    devs += pluginInstance.developers
                }

                if (devs) {
                    developers {
                        for (d in devs) {
                            developer {
                                name d.name
                                if (d.email) email d.email
                            }
                        }
                    }
                }

                // Handle the issue tracker
                if (getOptionalProperty(pluginInstance, "issueManagement")) {
                    def trackerInfo = pluginInstance.issueManagement
                    issueManagement {
                        if (trackerInfo.system) system trackerInfo.system
                        if (trackerInfo.url) delegate.url trackerInfo.url
                    }
                }

                // Source control
                if (getOptionalProperty(pluginInstance, "scm")) {
                    def scmInfo = pluginInstance.scm
                    scm {
                        if (scmInfo.connection) connection scmInfo.connection
                        if (scmInfo.developerConnection) developerConnection scmInfo.developerConnection
                        if (scmInfo.tag) tag scmInfo.tag
                        if (scmInfo.url) delegate.url scmInfo.url
                    }
                }
            }
            else {
                groupId buildConfig.grails.project.groupId ?: (config?.grails?.project?.groupId ?: grailsAppName)
                artifactId grailsAppName
                packaging "war"
                version grailsAppVersion
                name grailsAppName
            }

            def excludeResolver = grailsSettings.dependencyManager.excludeResolver
            def excludeInfo = excludeResolver.resolveExcludes()

            if (plugin) {
                dependencies {
                    def excludeHandler = { dep ->
                        if (dep.transitive == false) {
                            def excludes = excludeInfo[dep]
                            if (excludes != null) {
                                exclusions {
                                    for(exc in excludes) {
                                        exclusion {
                                            groupId exc.group
                                            artifactId exc.name
                                        }
                                    }
                                }
                            }
                        }
                        else if (dep.excludes) {
                            exclusions {
                                for(er in dep.excludes) {
                                    exclusion {
                                        if (er.group != '*') {
                                            groupId er.group
                                        }
                                        else {
                                            def excludes = excludeInfo[dep]
                                            if (excludes != null) {
                                                def resolvedExclude = excludes.find { it.name == er.name }
                                                if (resolvedExclude != null) {
                                                    groupId resolvedExclude.group
                                                }
                                            }

                                        }
                                        artifactId er.name
                                    }
                                }
                            }
                        }
                    }
                    corePlugins = pluginManager.allPlugins.findAll { it.pluginClass.name.startsWith("org.codehaus.groovy.grails.plugins") }*.name

                    def dependencyManager = grailsSettings.dependencyManager

                    def allowedScopes = ['runtime','compile', 'provided']
                    for (scope in allowedScopes) {
                        def appDeps = dependencyManager.getApplicationDependencies(scope)
                        def pluginDeps = dependencyManager.getPluginDependencies(scope)
                        for(dep in appDeps) {
                            if (scope in allowedScopes && dep.exported) {
                                dependency {
                                    groupId dep.group
                                    artifactId dep.name
                                    version dep.version
                                    delegate.scope(scope)

                                    excludeHandler(dep)
                                }
                            }
                        }
                        for(dep in pluginDeps) {
                            if (scope in allowedScopes && dep.exported) {
                                dependency {
                                    groupId dep.group
                                    artifactId dep.name
                                    version dep.version
                                    type "zip"
                                    delegate.scope(scope)

                                    excludeHandler(dep)
                                }
                            }
                        }                        
                    }
                }
            }
        }
    }
    event("StatusUpdate",["POM generated: ${pomFileLocation}"])
}

target(checkGrailsVersion: "Checks for Grails 2 and above - issues warning if Grails version is lower.") {
    if (isInteractive) {
        def m = grailsVersion =~ /^(\d+)\.\d+.*$/
        if (m && m[0][1].toInteger() < 2) {
            def inputHelper = new CommandLineHelper()
            def answer = inputHelper.userInput(
                    "WARNING! For full Grails 2.0 compatibility you should use Grails 2.0 " +
                    "or above with this command. Do you wish to continue? (y,N) ")
            if (!answer || !answer[0]?.equalsIgnoreCase("y")) {
                event "StatusFinal", ["Command cancelled."]
                exit 1
            }
        }
    }
}

processAuthConfig = { repoName, c ->
    // Get credentials for authentication if defined in the config.
    def projectConfig = grailsSettings.config.grails.project
    def username = projectConfig.repos."${repoName}".username
    def password = projectConfig.repos."${repoName}".password

    // Check whether only one of the authentication parameters has been set. If
    // so, exit with an error.
    if (!username ^ !password) {
        event("StatusError", ["grails.project.repos.${repoName}.username and .password must both be defined or neither."])
        return 1
    }

    c(username, password)
    return 0
}

private installOrDeploy(File file, ext, boolean deploy, repos = [:]) {
    if (!deploy) {
        ant.checksum file:pomFileLocation, algorithm:"sha1", todir:projectTargetDir
        ant.checksum file:file, algorithm:"sha1", todir:projectTargetDir
    }

    def pomCheck = generateChecksum(new File(pomFileLocation))
    def fileCheck = generateChecksum(file)

    artifact."${ deploy ? 'deploy' : 'install' }"(file: file) {
        if (isPlugin) {
            attach file:"${basedir}/plugin.xml",type:"xml", classifier:"plugin"
        }

        if (!deploy) {
            attach file:"${projectTargetDir}/pom.xml.sha1",type:"pom.sha1"
            attach file:"${projectTargetDir}/${file.name}.sha1",type:"${ext}.sha1"
        }

        pom(file: pomFileLocation)
        if (repos.remote) {
            def repo = repos.remote
            if (repo.configurer) {
                remoteRepository(repo.args, repo.configurer)
            }
            else {
                remoteRepository(repo.args)
            }
        }
        if (repos.local) {
            localRepository(path:repos.local)
        }
    }
}

private generateChecksum(File file) {
    def checksum = new File(file.parentFile.absolutePath, "${file.name}.sha1")
    checksum.write ChecksumHelper.computeAsString(file, "sha1")
    return checksum
}


private getOptionalProperty(obj, prop) {
    return obj.hasProperty(prop) ? obj."$prop" : null
}
