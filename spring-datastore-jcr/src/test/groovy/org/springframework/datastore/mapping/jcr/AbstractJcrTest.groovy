package org.springframework.datastore.mapping.jcr

import org.junit.AfterClass
import org.junit.BeforeClass

/**
 * Test harness for JCR tests
 *
 * @author Erawat Chamanont
 * @since 1.0
 */
class AbstractJcrTest {

    protected static conn
    protected static ds

    //setup JCR Environments
    @BeforeClass
    static void setupJCR() {
        ds = new JcrDatastore()
        def connectionDetails = [username:"username",
                                 password:"password",
                                 workspace:"default",
                                 configuration:"classpath:repository.xml",
                                 homeDir:"/temp/repo"]
        conn = ds.connect(connectionDetails)
    }

    @AfterClass
    static void tearDown() {
        conn.disconnect()
    }
}