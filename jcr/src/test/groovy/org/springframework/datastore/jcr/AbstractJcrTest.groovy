package org.springframework.datastore.jcr

import org.junit.BeforeClass
import javax.jcr.Repository
import org.springmodules.jcr.RepositoryFactoryBean
import org.apache.jackrabbit.core.TransientRepository
import javax.jcr.Session
import javax.jcr.SimpleCredentials
import org.junit.After
import org.junit.AfterClass

/**
 * Test harness for JCR tests
 *
 * @author Erawat Chamanont
 * @since 1.0
 */
class AbstractJcrTest{

  protected static def conn = null;

  //setup JCR Environments
  @BeforeClass
  public static void setupJCR(){
    def ds = new JcrDatastore()
    def connectionDetails = [username:"username",
                              password:"password",
                              workspace:"default",
                              configuration:"classpath:repository.xml",
                              homeDir:"/temp/repo"];
    conn = ds.connect(connectionDetails)

  }

  @AfterClass
  public static void tearDown() {
    conn.disconnect();
  }
}