package org.grails.inconsequential.appengine.testsupport;

import com.google.appengine.api.datastore.dev.LocalDatastoreService;
import com.google.appengine.tools.development.ApiProxyLocal;
import com.google.appengine.tools.development.ApiProxyLocalFactory;
import com.google.appengine.tools.development.LocalServerEnvironment;
import com.google.apphosting.api.ApiProxy;

import junit.framework.TestCase;

import java.io.File;

/**
 * Base test case class for datastore tests taken from the
 * <a href="http://code.google.com/intl/fr/appengine/docs/java/howto/unittesting.html">Google App Engine testing documentation</a>.
 *
 * @author Guillaume Laforge
 */
public abstract class AppEngineDatastoreTestCase extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ApiProxy.setEnvironmentForCurrentThread(new TestEnvironment());
        ApiProxyLocalFactory factory = new ApiProxyLocalFactory();
        ApiProxyLocal proxyLocal = factory.create(new LocalServerEnvironment() {
            public File getAppDir() {
                return new File(".");
            }

            public String getAddress() {
                return "localhost";
            }

            public int getPort() {
                return 8080;
            }

            public void waitForServerToStart() throws InterruptedException {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        proxyLocal.setProperty(LocalDatastoreService.NO_STORAGE_PROPERTY, Boolean.TRUE.toString());
        ApiProxy.setDelegate(proxyLocal);
    }

    @Override
    protected void tearDown() throws Exception {
        ApiProxyLocal proxy = (ApiProxyLocal) ApiProxy.getDelegate();
        LocalDatastoreService datastoreService = (LocalDatastoreService) proxy.getService(LocalDatastoreService.PACKAGE);
        datastoreService.clearProfiles();
        // not strictly necessary to null these out but there's no harm either
        ApiProxy.setDelegate(null);
        ApiProxy.setEnvironmentForCurrentThread(null);
        super.tearDown();
    }
}