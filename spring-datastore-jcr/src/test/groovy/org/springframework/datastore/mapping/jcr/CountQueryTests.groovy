package org.springframework.datastore.mapping.jcr

import org.junit.Test
import org.springframework.datastore.mapping.query.Query
import static org.springframework.datastore.mapping.query.Restrictions.eq
import org.junit.BeforeClass
import org.junit.AfterClass
import org.junit.After

/**
 * @author Erawat Chamanont
 * @since 1.0
 */
class CountQueryTests {

  protected static def conn = null
  protected static def ds = null

  @BeforeClass
  public static void setupJCR() {
    ds = new JcrDatastore()
    def connectionDetails = [username: "username",
            password: "password",
            workspace: "default",
            configuration: "classpath:repository.xml",
            homeDir: "/temp/repo"];
    conn = ds.connect(connectionDetails)
  }

  @AfterClass
  public static void tearDown() {
    conn.disconnect();
  }

  @After
  public void clearNodes() {
    def session = conn.getNativeInterface();
    def wp = session.getWorkspace();
    def qm = wp.getQueryManager();

    def q = qm.createQuery("//Book", javax.jcr.query.Query.XPATH);
    def qr = q.execute()
    def itr = qr.getNodes();
    itr.each { it.remove() }

    q = qm.createQuery("//Author", javax.jcr.query.Query.XPATH);
    qr = q.execute()
    itr = qr.getNodes();
    itr.each { it.remove() }
    session.save()
  }

  @Test
  void testDisjunctionAndCount() {
    ds.mappingContext.addPersistentEntity(Author)


    def a = new Author(name: "Stephen King")
    a.books = [
            new Book(title: "The Stand"),
            new Book(title: "It"),
            new Book(title: "The Shining")
    ]
    
    conn.persist(a)

    Query q = conn.createQuery(Book)
    q.disjunction().add(eq("title", "The Stand")).add(eq("title", "It"))
    q.projections().count()

    assert 2 == q.singleResult()

  }

  @Test
  void testSimpleQueryAndCount() {
    ds.mappingContext.addPersistentEntity(Author)

    def a = new Author(name: "Stephen King")
    a.books = [
            new Book(title: "The Stand"),
            new Book(title: "It")
    ]

    conn.persist(a)


    Query q = conn.createQuery(Book)

    q.eq("title", "It")
    q.projections().count()

    def result = q.singleResult()

    assert 1 == result

    q = conn.createQuery(Book)

    q.eq("title", "The Stand")
    q.projections().count()

    result = q.singleResult()

    assert 1 == result

  }

}
