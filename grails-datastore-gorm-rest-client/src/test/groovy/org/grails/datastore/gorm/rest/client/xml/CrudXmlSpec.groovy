package org.grails.datastore.gorm.rest.client.xml

import grails.persistence.Entity
import org.codehaus.groovy.grails.web.servlet.HttpHeaders
import org.grails.datastore.gorm.rest.client.RestClientDatastoreSpec
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess

/**
 */
class CrudXmlSpec extends RestClientDatastoreSpec{
    void "Test the get method issues a GET request and binds the result to the entity"() {
        when:"An entity is retrieved with the get method"
            RestTemplate rt = Book.getRestBuilder().restTemplate
            final mockServer = MockRestServiceServer.createServer(rt)
            mockServer.expect(requestTo("http://localhost:8080/book/1"))
                    .andExpect(method(HttpMethod.GET))
                    .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML.toString()))
                    .andRespond(withSuccess('<book id="1"><title>The Stand</title><pages>200</pages></book>', MediaType.APPLICATION_XML))

            Book b = Book.get(1)

        then:"The number of books that exist is correct"
            b != null
            b.id == 1L
            b.title == "The Stand"
            b.pages == 200
            mockServer.verify()

    }

    void "Test the getAll method issues a GET request and binds the result to the entity"() {
        when:"An entity is retrieved with the get method"
            RestTemplate rt = Book.getRestBuilder().restTemplate
            final mockServer = MockRestServiceServer.createServer(rt)
            mockServer.expect(requestTo("http://localhost:8080/book/1"))
                    .andExpect(method(HttpMethod.GET))
                    .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML.toString()))
                    .andRespond(withSuccess('<book id="1"><title>The Stand</title><pages>200</pages></book>', MediaType.APPLICATION_XML))

            List<Book> books = Book.getAll([1])

        then:"The number of books that exist is correct"
            books
            books[0] != null
            books[0].id == 1L
            books[0].title == "The Stand"
            books[0].pages == 200
            mockServer.verify()

    }

    void "Test the saveAll method issues a POST request with the correct data"() {
        when:"A new entity is saved"
            RestTemplate rt = Book.getRestBuilder().restTemplate
            final mockServer = MockRestServiceServer.createServer(rt)
            mockServer.expect(requestTo("http://localhost:8080/book"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().contentType(MediaType.APPLICATION_XML))
                    .andExpect(content().string('<?xml version="1.0" encoding="UTF-8"?><book><pages /><title>The Stand</title></book>'))
                    .andRespond(withSuccess('<book id="1"><title>The Stand</title><pages>200</pages></book>', MediaType.APPLICATION_XML))
    
            mockServer.expect(requestTo("http://localhost:8080/book/2"))
                    .andExpect(method(HttpMethod.PUT))
                    .andExpect(content().contentType(MediaType.APPLICATION_XML))
                    .andExpect(content().string('<?xml version="1.0" encoding="UTF-8"?><book id="2"><pages /><title>The Shining</title></book>'))
                    .andRespond(withSuccess('<?xml version="1.0" encoding="UTF-8"?><book id="2"><pages /><title>The Shining</title></book>', MediaType.APPLICATION_XML))
    
    
            def b = new Book(title: "The Stand")
            def b2 = new Book(title: "The Shining")
            b2.id = 2L
            def ids = Book.withSession { session ->
                def identifiers = Book.saveAll([b, b2])
                session.flush()
                return identifiers
            }


        then:"The number of books that exist is correct"
            b.id == 1L
            ids == [1L, 2L]
            mockServer.verify()

    }

    void "Test the save method issues a POST request with the correct data"() {
        when:"A new entity is saved"
            RestTemplate rt = Book.getRestBuilder().restTemplate
            final mockServer = MockRestServiceServer.createServer(rt)
            mockServer.expect(requestTo("http://localhost:8080/book"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().contentType(MediaType.APPLICATION_XML))
                    .andExpect(content().string('<?xml version="1.0" encoding="UTF-8"?><book><pages /><title>The Stand</title></book>'))
                    .andRespond(withSuccess('<?xml version="1.0" encoding="UTF-8"?><book id="1"><pages /><title>The Stand</title></book>', MediaType.APPLICATION_XML))

            def b = new Book(title: "The Stand")
            b.save()

        then:"The number of books that exist is correct"
            b.id == 1L
            mockServer.verify()

    }

    void "Test the insert method issues a POST request with the correct data"() {
        when:"A new entity is saved"
            RestTemplate rt = Book.getRestBuilder().restTemplate
            final mockServer = MockRestServiceServer.createServer(rt)
            mockServer.expect(requestTo("http://localhost:8080/book"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(content().contentType(MediaType.APPLICATION_XML))
                    .andExpect(content().string('<?xml version="1.0" encoding="UTF-8"?><book id="1"><pages /><title>The Stand</title></book>'))
                    .andRespond(withSuccess('<?xml version="1.0" encoding="UTF-8"?><book id="1"><pages /><title>The Stand</title></book>', MediaType.APPLICATION_XML))

            def b = new Book(title: "The Stand")
            b.id = 1L
            b.insert(flush:true)

        then:"The number of books that exist is correct"
            b.id == 1L
            mockServer.verify()

    }

    void "Test the save method issues a PUT request for an update with the correct data"() {
        when:"A new entity is saved"
            RestTemplate rt = Book.getRestBuilder().restTemplate
            final mockServer = MockRestServiceServer.createServer(rt)
            mockServer.expect(requestTo("http://localhost:8080/book/1"))
                    .andExpect(method(HttpMethod.PUT))
                    .andExpect(content().contentType(MediaType.APPLICATION_XML))
                    .andExpect(content().string('<?xml version="1.0" encoding="UTF-8"?><book id="1"><pages /><title>The Stand</title></book>'))
                    .andRespond(withSuccess('<?xml version="1.0" encoding="UTF-8"?><book id="1"><pages /><title>The Stand</title></book>', MediaType.APPLICATION_XML))

            def b = new Book(title: "The Stand")
            b.id = 1L
            b.save(flush:true)

        then:"The number of books that exist is correct"
            b.id == 1L
            mockServer.verify()

    }

    void "Test the delete method issues a DELETE request for an update with the correct data"() {
        when:"A new entity is saved"
            RestTemplate rt = Book.getRestBuilder().restTemplate
            final mockServer = MockRestServiceServer.createServer(rt)
            mockServer.expect(requestTo("http://localhost:8080/book/1"))
                    .andExpect(method(HttpMethod.DELETE))
                    .andRespond(withStatus(HttpStatus.NO_CONTENT))

            def b = new Book(title: "The Stand")
            b.id = 1L
            b.delete(flush:true)

        then:"The number of books that exist is correct"
            b.id == 1L
            mockServer.verify()

    }

    void "Test the deleteAll method issues a DELETE request for an update with the correct data"() {
        when:"A new entity is saved"
            RestTemplate rt = Book.getRestBuilder().restTemplate
            final mockServer = MockRestServiceServer.createServer(rt)
            mockServer.expect(requestTo("http://localhost:8080/book/1"))
                    .andExpect(method(HttpMethod.DELETE))
                    .andRespond(withStatus(HttpStatus.NO_CONTENT))

            def b = new Book(title: "The Stand")
            b.id = 1L
            Book.withSession { session ->
                Book.deleteAll([b])
                session.flush()
            }



        then:"The number of books that exist is correct"
            b.id == 1L
            mockServer.verify()

    }
    @Override
    List<Class> getDomainClasses() {
        return [Book]
    }
}

@Entity
class Book {
    Long id
    Long version
    String title
    Integer pages

    static mapping = {
        contentType "application/xml"
        accept "application/xml"
//        async false
    }
}
