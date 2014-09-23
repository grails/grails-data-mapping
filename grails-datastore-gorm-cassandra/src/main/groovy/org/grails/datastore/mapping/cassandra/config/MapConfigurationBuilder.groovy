package org.grails.datastore.mapping.cassandra.config



class MapConfigurationBuilder {
    
    Map properties = [:]        

    def invokeMethod(String name, args) {
        if (args.size() == 0) {
            return
        }
        
		if (args.size() == 1) {
			properties[name] = args[0]
		}
    }

    void evaluate(Closure callable) {
        if (!callable) {
            return
        }

        def originalDelegate = callable.delegate
        try {
            callable.delegate = this
            callable.resolveStrategy = Closure.DELEGATE_FIRST
            callable.call()
        } finally {
            callable.delegate = originalDelegate
        }
    }
}
