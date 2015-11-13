package grails.gorm.tests

import org.hibernate.cfg.Configuration
import spock.lang.Issue
import grails.persistence.Entity
import org.grails.datastore.gorm.Setup
import org.grails.orm.hibernate.cfg.GrailsAnnotationConfiguration

/**
 */
class JoinTableWithSchemaMappingSpec extends GormDatastoreSpec{

    @Issue('GRAILS-8737')
    void "Test that a join table with schema and sequence generator works correctly"() {
        when:"A many-to-many with join table and schema definition is persisted"
            def author = new JoinTableSchemaAuthor(name: "Stephen King")
            author.books = [ new JoinTableSchemaBook(name:"The Stand"), new JoinTableSchemaBook(name: "The Shining")]
            author.save(flush:true)
            session.clear()
            author = JoinTableSchemaAuthor.get(author.id)

        then:"The results are correct"
            author != null
            author.books.size() == 2

    }

    @Issue('GRAILS-8737')
    void "Test that the schema created for a join table is correct"() {
        when:"The hibernate configuration is obtained"
            Configuration config = Setup.hibernateConfig
            final authorMapping = config.getClassMapping(JoinTableSchemaAuthor.name)


        then:"The results are correct"
            config != null
            authorMapping != null
            authorMapping.table.schema == 'www'
            authorMapping.getProperty("books").getValue().collectionTable.schema == 'www'

    }
    @Override
    List getDomainClasses() {
        [JoinTableSchemaAuthor,JoinTableSchemaBook]
    }
}

@Entity
class JoinTableSchemaBook {
    Long id
    Long version
    String name
    Set authors
    static hasMany = [authors:JoinTableSchemaAuthor]
    static belongsTo = JoinTableSchemaAuthor

    static constraints = {
    }
    static mapping = {
        table name: 'join_table_schema_book', schema: 'www'
//        id generator: 'seqhilo', params:[sequence:'book_id_seq', schema: 'www']
        version false;
    }

}
@Entity
class JoinTableSchemaAuthor {
    Long id
//    Long version
    String name
    Set books
    static hasMany = [books:JoinTableSchemaBook]

    static constraints = {
    }

    static mapping = {
        table name: 'join_table_schema_author', schema: 'www'
//        id generator: 'seqhilo', params:[sequence:'author_id_seq', schema: 'www']
        version false;
    }
}
