package org.springframework.datastore.jcr

import org.junit.BeforeClass
import javax.jcr.Repository

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
  protected static def ds = null;
  //setup JCR Environments
  @BeforeClass
  public static void setupJCR(){
    ds = new JcrDatastore()
    def connectionDetails = [username:"username",
                              password:"password",
                              workspace:"default",
                              configuration:"classpath:repository.xml",
                              homeDir:"/temp/repo"];
    conn = ds.connect(connectionDetails)

  }

/*  @AfterClass
  public static void tearDown() {
    conn.disconnect();
  }*/
}