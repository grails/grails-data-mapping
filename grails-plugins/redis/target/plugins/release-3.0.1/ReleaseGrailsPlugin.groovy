/*
 * Copyright 2011-2013 the original author or authors
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
class ReleaseGrailsPlugin {
    def version = "3.0.1"
    def grailsVersion = "2.3 > *"
    def author = "Graeme Rocher"
    def authorEmail = "grocher@vmware.com"
    def title = "Release Plugin"
    def description = 'Allows you to publish Grails plugins, either to a public or private repository. It also supports deploying Grails applications and plugins to Maven repositories without the need to use Maven directly.'
    def license = "APACHE"
    def organization = [ name: "SpringSource", url: "http://www.springsource.org/" ]
    def developers = [ [ name: "Peter Ledbrook", email: "pledbrook@vmware.com" ] ]
    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPRELEASE" ]
    def scm = [ url: "https://github.com/grails-plugins/grails-release/" ]
    def documentation = "http://grails-plugins.github.com/grails-release/"
}
