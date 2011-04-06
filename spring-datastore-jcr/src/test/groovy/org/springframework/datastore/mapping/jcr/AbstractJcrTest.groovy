package org.springframework.datastore.mapping.jcr

import org.junit.AfterClass
import org.junit.BeforeClass
import org.springframework.context.support.GenericApplicationContext

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
        def ctx = new GenericApplicationContext()
        ctx.refresh()
        ds = new JcrDatastore(ctx)
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