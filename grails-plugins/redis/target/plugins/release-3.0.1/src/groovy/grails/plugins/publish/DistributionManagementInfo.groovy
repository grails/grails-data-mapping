package grails.plugins.publish

class DistributionManagementInfo {
    Map portals = [:]
    Map remoteRepos = [:]
    String localRepo

    void localRepository(String s) { localRepo = s }

    void remoteRepository(Map args, Closure c = null) {
        if (!args?.id) throw new Exception("Remote repository misconfigured: Please specify a repository 'id'. Eg. remoteRepository(id:'myRepo')")
        if (!args?.url) throw new Exception("Remote repository misconfigured: Please specify a repository 'url'. Eg. remoteRepository(url:'http://..')")
        remoteRepos[args.id] = new Expando(args: args, configurer: c)
    }

    void portal(Map args) {
        def options = [:] + args
        options.remove("id")

        portals[args.id] = options
    }
}
