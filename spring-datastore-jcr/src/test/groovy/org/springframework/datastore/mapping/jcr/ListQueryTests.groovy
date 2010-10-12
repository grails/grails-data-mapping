package org.springframework.datastore.mapping.jcr

import org.junit.Test
import org.springframework.datastore.mapping.query.Query
import org.junit.AfterClass
import org.junit.BeforeClass
import static org.springframework.datastore.mapping.query.Restrictions.*


/**
 *
 * @author Erawat Chamanont
 * @since 1.0
 */
class ListQueryTests {

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
    def session = conn.getNativeInterface();
    if (session.itemExists("/Author")) {
        javax.jcr.Node node = session.getRootNode().getNode("Author")
        node.remove()
        session.save()
    }
    conn.disconnect()
  }

  @Test
  void testListQuery() {
    ds.mappingContext.addPersistentEntity(Author)

    def a = new Author(name: "Stephen King")
    a.books = [
            new Book(title: "The Stand"),
            new Book(title: "It")]

    conn.persist(a)

    Query q = conn.createQuery(Book)

    def results = q.list()

    assert 2 == results.size()

    //assert "The Stand" == results[0].title
    //assert "It" == results[1].title

    assert null !=  results.find { it.title == "The Stand" }
    assert null !=  results.find { it.title == "It" }


    q.max 1

    results = q.list()

    assert 1 == results.size()
    //assert "The Stand" == results[0].title
  }

  @Test
  void testDisjunction() {
    ds.mappingContext.addPersistentEntity(Author)

    def a = new Author(name: "Stephen King")
    a.books = [
            new Book(title: "The Stand"),
            new Book(title: "It"),
            new Book(title: "The Shining")
    ]

    conn.persist(a)


    Query q = conn.createQuery(Book)
    
    q.disjunction().add(eq("title", "The Stand"))
                   .add(eq("title", "The Shining"))

    def results = q.list()

    assert 2 == results.size()
    assert null !=  results.find { it.title == "The Stand" }
    assert null !=  results.find { it.title == "The Shining" }

  }

  @Test
  void testIdProjection() {
    ds.mappingContext.addPersistentEntity(Author)

    def a = new Author(name: "Stephen King")
    a.books = [
            new Book(title: "The Stand"),
            new Book(title: "It"),
            new Book(title: "The Shining")
    ]

    conn.persist(a)


    Query q = conn.createQuery(Book)
    q.disjunction().add(eq("title", "The Stand"))
                   .add(eq("title", "It"))
    q.projections().id()


    def results = q.list()

    assert 2 == results.size()
    assert results[0] instanceof String
  }

  @Test
  void testSimpleQuery() {
    ds.mappingContext.addPersistentEntity(Author)

    def a = new Author(name: "Stephen King")
    a.books = [
            new Book(title: "The Stand"),
            new Book(title: "It")
    ]

    conn.persist(a)


    Query q = conn.createQuery(Book)

    q.eq("title", "It")

    def results = q.list()

    assert 1 == results.size()
    assert "It" == results[0].title

    q = conn.createQuery(Book)

    q.eq("title", "The Stand")

    results = q.list()

    assert 1 == results.size()
    assert "The Stand" == results[0].title

  }
}
