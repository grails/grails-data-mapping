package org.grails.inconsequential.appengine.testsupport;

import com.google.appengine.api.datastore.dev.LocalDatastoreService;
import com.google.appengine.tools.development.ApiProxyLocalImpl;
import com.google.apphosting.api.ApiProxy;

import junit.framework.TestCase;

import java.io.File;

/**
 * Base test case class for datastore tests taken from the
 * <a href="http://code.google.com/intl/fr/appengine/docs/java/howto/unittesting.html">Google App Engine testing documentation</a>.
 *
 * @author Guillaume Laforge
 */
public class AppEngineDatastoreTestCase extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ApiProxy.setEnvironmentForCurrentThread(new TestEnvironment());
        ApiProxyLocalImpl proxyLocal = new ApiProxyLocalImpl(new File(".")) { };
        proxyLocal.setProperty(LocalDatastoreService.NO_STORAGE_PROPERTY, Boolean.TRUE.toString());
        ApiProxy.setDelegate(proxyLocal);
    }

    @Override
    protected void tearDown() throws Exception {
        ApiProxyLocalImpl proxy = (ApiProxyLocalImpl) ApiProxy.getDelegate();
        LocalDatastoreService datastoreService = (LocalDatastoreService) proxy.getService(LocalDatastoreService.PACKAGE);
        datastoreService.clearProfiles();
        // not strictly necessary to null these out but there's no harm either
        ApiProxy.setDelegate(null);
        ApiProxy.setEnvironmentForCurrentThread(null);
        super.tearDown();
    }
}