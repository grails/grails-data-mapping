package org.grails.inconsequential.appengine;

import org.grails.inconsequential.core.Key;

/**
 * @author Guillaume Laforge
 */
public class AppEngineKey implements Key<com.google.appengine.api.datastore.Key> {

    private com.google.appengine.api.datastore.Key nativeKey;

    public AppEngineKey(com.google.appengine.api.datastore.Key nativeKey) {
        this.nativeKey = nativeKey;
    }

    public com.google.appengine.api.datastore.Key getNativeKey() {
        return nativeKey;
    }
}
