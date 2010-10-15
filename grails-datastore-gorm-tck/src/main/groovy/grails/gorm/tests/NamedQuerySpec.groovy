package grails.gorm.tests

import spock.lang.Ignore

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Aug 31, 2010
 * Time: 10:07:42 AM
 * To change this template use File | Settings | File Templates.
 */
class NamedQuerySpec extends GormDatastoreSpec {

    void "Test named query with disjunction"() {
      given:
        def now = new Date()
        def oldDate = now - 2000

        assert Publication.newInstance(title: 'New Paperback', datePublished: now, paperback: true).save()
        assert Publication.newInstance(title: 'Old Paperback', datePublished: oldDate, paperback: true).save()
        assert Publication.newInstance(title: 'New Hardback', datePublished: now, paperback: false).save()
        assert Publication.newInstance(title: 'Old Hardback', datePublished: oldDate, paperback: false).save()
        session.flush()
        session.clear()

      when:
        def publications = Publication.paperbackOrRecent.list()

      then:
        assert 3 == publications?.size()
    }


    void "Test max and offset parameter"() {
      given:
        (1..25).each {num ->
            Publication.newInstance(title: "Book Number ${num}",
                    datePublished: new Date()).save()
        }

      when:
        def pubs = Publication.recentPublications.list(max: 10, offset: 5)

      then:
        10 == pubs?.size()

      when:
        pubs = Publication.recentPublications.list(max: '10', offset: '5')

      then:
        10 == pubs?.size()
    }
  
    void "Test that parameter to get is converted"() {

      given:
        def now = new Date()
        def newPublication = Publication.newInstance(title: "Some New Book", datePublished: now - 10).save()
        assert newPublication
        def oldPublication = Publication.newInstance(title: "Some Old Book",
        datePublished: now - 900).save(flush:true)
        assert oldPublication

        session.clear()

      when:
        def publication = Publication.recentPublications.get(newPublication.id.toString())

      then:
        publication != null
        'Some New Book'== publication.title
    }
  
    void "Test named query with additional criteria closure"() {

      given:
          def now = new Date()
          6.times {
              assert Publication.newInstance(title: "Some Book",
              datePublished: now - 10).save()
              assert Publication.newInstance(title: "Some Other Book",
              datePublished: now - 10).save()
              assert Publication.newInstance(title: "Some Book",
              datePublished: now - 900).save()
          }
          session.flush()
          session.clear()


      when:
        def publications = Publication.recentPublications {
            eq 'title', 'Some Book'
        }
      then:
        assert 6 == publications?.size()

      when:
        publications = Publication.recentPublications {
            like 'title', 'Some%'
        }
      then:
        assert 12 == publications?.size()


      when:
        def cnt = Publication.recentPublications.count {
            eq 'title', 'Some Book'
        }
      then:
        assert 6 == cnt
      
      when:
        publications = Publication.recentPublications(max: 3) {
            like 'title', 'Some%'
        }
      then:
        assert 3 == publications?.size()

    }
  
    void "Test passing parameters to additional criteria"() {
      given:
        def now = new Date()

        6.times { cnt ->
            new Publication(title: "Some Old Book #${cnt}",
                      datePublished: now - 1000, paperback: true).save(failOnError: true).id
            new Publication(title: "Some New Book #${cnt}",
                      datePublished: now, paperback: true).save(failOnError: true).id
        }

        session?.flush()

      when:
        def results = Publication.publishedAfter(now - 5) {
            eq 'paperback', true
        }

      then:
        assert 6 == results?.size()

      when:
        results = Publication.publishedAfter(now - 5, [max: 2, offset: 1]) {
            eq 'paperback', true
        }

      then:
        assert 2 == results?.size()

      when:
        results = Publication.publishedBetween(now - 5, now + 1) {
            eq 'paperback', true
        }
      then:
        assert 6 == results?.size()

      when:
        results = Publication.publishedBetween(now - 5, now + 1, [max: 2, offset: 1]) {
            eq 'paperback', true
        }
      then:
        assert 2 == results?.size()

      when:
        results = Publication.publishedAfter(now - 1005) {
            eq 'paperback', true
        }
      then:
        assert 12 == results?.size()

      when:
        results = Publication.publishedAfter(now - 5) {
            eq 'paperback', false
        }
      then:
        assert 0 == results?.size()

      when:
        results = Publication.publishedAfter(now - 5, [max: 2, offset: 1]) {
            eq 'paperback', false
        }
      then:
        assert 0 == results?.size()

      when:
        results = Publication.publishedBetween(now - 5, now + 1) {
            eq 'paperback', false
        }
      then:
        assert 0 == results?.size()

      when:
        results = Publication.publishedBetween(now - 5, now + 1, [max: 2, offset: 1]) {
            eq 'paperback', false
        }
      then:
        assert 0 == results?.size()

      when:
        results = Publication.publishedAfter(now - 1005) {
            eq 'paperback', false
        }
      then:
        assert 0 == results?.size()
    }
  
    void "Test get method followed named query chaining"() {
      given:
        def now = new Date()

        def oldPaperBackWithBookInTitleId =  new Publication(title: "Book 1",
                datePublished: now - 1000, paperback: true).save().id
        def newPaperBackWithBookInTitleId =  new Publication(title: "Book 2",
                datePublished: now, paperback: true).save().id

        session.flush()
        session.clear()

      when:
        def publication = Publication.publicationsWithBookInTitle().publishedAfter(now - 5).get(oldPaperBackWithBookInTitleId)

      then:
        publication == null

      when:
        publication = Publication.publishedAfter(now - 5).publicationsWithBookInTitle().get(oldPaperBackWithBookInTitleId)

      then:
        publication == null

      when:
        publication = Publication.publishedAfter(now - 5).publicationsWithBookInTitle().get(newPaperBackWithBookInTitleId)

      then:
        publication != null

      when:
        publication = Publication.publishedAfter(now - 5).publicationsWithBookInTitle().get(newPaperBackWithBookInTitleId)

      then:
        publication != null
      
    }

    void "Test named query with findBy*() dynamic finder"() {

      given:
        def now = new Date()
        assert Publication.newInstance(title: "Book 1",
                    datePublished: now - 900).save()
        def recentBookId = Publication.newInstance(title: "Book 1",
                    datePublished: now - 10).save(flush:true).id
        session.clear()


      when:
        def publication = Publication.recentPublications.findByTitle('Book 1')

      then:
        publication != null
        recentBookId == publication.id
    }
  
    void "Test named query with findAllBy*() dyamic finder"() {
      given:
        def now = new Date()
        3.times {
            assert new Publication(title: "Some Recent Book",
                    datePublished: now - 10).save()
            assert new Publication(title: "Some Other Book",
                    datePublished: now - 10).save()
            assert new Publication(title: "Some Book",
                    datePublished: now - 900).save(flush:true)
        }
        session.clear()

      when:
        def publications = Publication.recentPublications.findAllByTitle('Some Recent Book')

      then:
        assert 3 == publications?.size()
        assert 'Some Recent Book' == publications[0].title
        assert 'Some Recent Book' == publications[1].title
        assert 'Some Recent Book' == publications[2].title
    }
  
    @spock.lang.Ignore  // queries on associations not yet supported
    void "Test named query with relationships in criteria"() {

        given:

          new PlantCategory(name:"leafy")
                                   .addToPlants(goesInPatch:true, name:"Lettuce")
                                   .save(flush:true)

          new PlantCategory(name:"groovy")
                                   .addToPlants(goesInPatch: true, name: 'Gplant')
                                   .save(flush:true)

          new PlantCategory(name:"grapes")
                                   .addToPlants(goesInPatch:false, name:"Gray")
                                   .save(flush:true)

          session.clear()

        when:
          def results = PlantCategory.withPlantsInPatch.list()

        then:
          2 == results.size()
          true == 'leafy' in results*.name
          true == 'groovy' in results*.name

        when:
          results = PlantCategory.withPlantsThatStartWithG.list()

        then:
          2 == results.size()
          true == 'groovy' in results*.name
          true == 'grapes' in results*.name

        when:
          results = PlantCategory.withPlantsInPatchThatStartWithG.list()

        then:
          1 == results.size()
          'groovy' == results[0].name
    }

    @spock.lang.Ignore  // queries on associations not yet supported
    void "Test list distinct entities"() {

        given:
          new PlantCategory(name:"leafy")
                             .addToPlants(goesInPatch:true, name:"lettuce")
                             .addToPlants(goesInPatch:true, name:"cabbage")
                             .save(flush:true)

          new PlantCategory(name:"orange")
                                 .addToPlants(goesInPatch:true, name:"carrots")
                                 .addToPlants(goesInPatch:true, name:"pumpkin")
                                 .save(flush:true)

            new PlantCategory(name:"grapes")
                                 .addToPlants(goesInPatch:false, name:"red")
                                 .addToPlants(goesInPatch:false, name:"white")
                                 .save(flush:true)

          session.clear()


        when:
          def categories = plantCategoryClass.withPlantsInPatch().listDistinct()
          def names = categories*.name

        then:
          2 == categories.size()
          2 == names.size()
          true == 'leafy' in names
          true == 'orange' in names
    }

    @spock.lang.Ignore  // queries on associations not yet supported
    void "Another test on listing distinct entities"() {
      given:
        new PlantCategory(name:"leafy")
                           .addToPlants(goesInPatch:true, name:"lettuce")
                           .addToPlants(goesInPatch:true, name:"cabbage")
                           .save(flush:true)

        new PlantCategory(name:"orange")
                               .addToPlants(goesInPatch:true, name:"carrots")
                               .addToPlants(goesInPatch:true, name:"pumpkin")
                               .save(flush:true)

          new PlantCategory(name:"grapes")
                               .addToPlants(goesInPatch:false, name:"red")
                               .addToPlants(goesInPatch:false, name:"white")
                               .save(flush:true)

        session.clear()

      when:
        def categories = plantCategoryClass.withPlantsInPatch.listDistinct()
        def names = categories*.name

      then:
        3 == categories.size()
        3 == names.size()
        true == 'leafy' in names
        true == 'orange' in names
        true == 'grapes' in names
    }

    void "Test findWhere method after chaining named queries"() {
      given:
        def now = new Date()

        new Publication(title: "Book 1",
                datePublished: now - 10, paperback: false).save()
        new Publication(title: "Book 2",
                datePublished: now - 1000, paperback: true).save()
        new Publication(title: "Book 3",
                datePublished: now - 10, paperback: true).save()

        new Publication(title: "Some Title",
                datePublished: now - 10, paperback: false).save()
        new Publication(title: "Some Title",
                datePublished: now - 1000, paperback: false).save()
        new Publication(title: "Some Title",
                datePublished: now - 10, paperback: true).save(flush:true)
        session.clear()

      when:
        def results = Publication.recentPublications().publicationsWithBookInTitle().findAllWhere(paperback: true)

      then:
        assert 1 == results?.size()
    }

    void "Test named query passing multiple parameters to a nested query"() {
      given:
        def now = new Date()

        new Publication(title: "Some Book",
                        datePublished: now - 10, paperback: false).save()
        new Publication(title: "Some Book",
                        datePublished: now - 1000, paperback: true).save()
        new Publication(title: "Some Book",
                        datePublished: now - 2, paperback: true).save()

        new Publication(title: "Some Title",
                        datePublished: now - 2, paperback: false).save()
        new Publication(title: "Some Title",
                        datePublished: now - 1000, paperback: false).save()
        new Publication(title: "Some Title",
                        datePublished: now - 2, paperback: true).save(flush:true)
        session.clear()

      when:

        def results = Publication.thisWeeksPaperbacks().list()

      then:
        assert 2 == results?.size()
    }

    void "Test chaining named queries"() {

      given:
        def now = new Date()
        [true, false].each { isPaperback ->
            4.times {
                Publication.newInstance(title: "Book Some",
                        datePublished: now - 10, paperback: isPaperback).save()
                Publication.newInstance(title: "Book Some Other",
                        datePublished: now - 10, paperback: isPaperback).save()
                Publication.newInstance(title: "Some Other Title",
                        datePublished: now - 10, paperback: isPaperback).save()
                Publication.newInstance(title: "Book Some",
                        datePublished: now - 1000, paperback: isPaperback).save()
                Publication.newInstance(title: "Book Some Other",
                        datePublished: now - 1000, paperback: isPaperback).save()
                Publication.newInstance(title: "Some Other Title",
                        datePublished: now - 1000, paperback: isPaperback).save()
            }
        }
        session.flush()
        session.clear()

      when:
        def results = Publication.recentPublications().publicationsWithBookInTitle().list()

      then: "The result size should be 16 when returned from chained queries"
        assert 16 == results?.size() 

      when:
        results = Publication.recentPublications().publicationsWithBookInTitle().count()
      then:
        assert 16 == results


      when:
        results = Publication.recentPublications.publicationsWithBookInTitle.list()
      then:"The result size should be 16 when returned from chained queries"
        assert 16 == results?.size()

      when:
        results = Publication.recentPublications.publicationsWithBookInTitle.count()
      then:
        assert 16 == results

      when:
        results = Publication.paperbacks().recentPublications().publicationsWithBookInTitle().list()
      then: "The result size should be 8 when returned from chained queries"
        assert 8 ==  results?.size() 

      when:
        results = Publication.paperbacks().recentPublications().publicationsWithBookInTitle().count()
      then:
        assert 8 == results

      when:
        results = Publication.recentPublications().publicationsWithBookInTitle().findAllByPaperback(true)
      then: "The result size should be 8"
        assert 8 == results?.size()

      when:
        results = Publication.paperbacks.recentPublications.publicationsWithBookInTitle.list()
      then:"The result size should be 8 when returned from chained queries"
        assert 8 == results?.size()

      when:
        results = Publication.paperbacks.recentPublications.publicationsWithBookInTitle.count()
      then:
        assert 8 == results
    }

    void testChainingQueriesWithParams() {
        def Publication = ga.getDomainClass("Publication").clazz

        def now = new Date()
        def lastWeek = now - 7
        def longAgo = now - 1000
        2.times {
            assert Publication.newInstance(title: 'Some Book',
                    datePublished: now).save()
            assert Publication.newInstance(title: 'Some Title',
                    datePublished: now).save()
        }
        3.times {
            assert Publication.newInstance(title: 'Some Book',
                    datePublished: lastWeek).save()
            assert Publication.newInstance(title: 'Some Title',
                    datePublished: lastWeek).save()
        }
        4.times {
            assert Publication.newInstance(title: 'Some Book',
                    datePublished: longAgo).save()
            assert Publication.newInstance(title: 'Some Title',
                    datePublished: longAgo).save()
        }
        session.clear()

        def results = Publication.recentPublicationsByTitle('Some Book').publishedAfter(now - 2).list()
        assertEquals 'wrong number of books were returned from chained queries', 2, results?.size()

        results = Publication.recentPublicationsByTitle('Some Book').publishedAfter(now - 2).count()
        assertEquals 2, results

        results = Publication.recentPublicationsByTitle('Some Book').publishedAfter(lastWeek - 2).list()
        assertEquals 'wrong number of books were returned from chained queries', 5, results?.size()

        results = Publication.recentPublicationsByTitle('Some Book').publishedAfter(lastWeek - 2).count()
        assertEquals 5, results
    }

    void "Test referencing named query before any dynamic methods"() {

        /*
         * currently this will work:
         *   Publication.recentPublications().list()
         * but this will not:
         *   Publication.recentPublications.list()
         *
         * the static property isn't being added to the class until
         * the first dynamic method (recentPublications(), save(), list() etc...) is
         * invoked
         */
        given:
        when:
        def publications = Publication.recentPublications.list()
        then:
        assert 0 == publications.size()
    }



    void "Test named query with conjunction"() {
      given:
        def now = new Date()
        def oldDate = now - 2000

        assert Publication.newInstance(title: 'New Paperback', datePublished: now, paperback: true).save()
        assert Publication.newInstance(title: 'Old Paperback', datePublished: oldDate, paperback: true).save()
        assert Publication.newInstance(title: 'New Hardback', datePublished: now, paperback: false).save()
        assert Publication.newInstance(title: 'Old Hardback', datePublished: oldDate, paperback: false).save()
        session.flush()
        session.clear()

      when:
        def publications = Publication.paperbackAndRecent.list()

      then:
        1 == publications?.size()
    }

    void "Test named query with list() method"() {

      given:
        def now = new Date()
        assert Publication.newInstance(title: "Some New Book",
                datePublished: now - 10).save()
        assert Publication.newInstance(title: "Some Old Book",
                datePublished: now - 900).save(flush:true)

        session.clear()

      when:

        def publications = Publication.recentPublications.list()

      then:
        assert 1 == publications?.size()
        assert 'Some New Book' == publications[0].title
    }



    // findby boolean queries not yet supported
    @Ignore
    void "Test named query with findAll by boolean property"() {
      given:
        def Publication = ga.getDomainClass("Publication").clazz

        def now = new Date()

        assert Publication.newInstance(title: 'Some Book', datePublished: now - 900, paperback: false).save()
        assert Publication.newInstance(title: 'Some Book', datePublished: now - 900, paperback: false).save()
        assert Publication.newInstance(title: 'Some Book', datePublished: now - 10, paperback: true).save()
        assert Publication.newInstance(title: 'Some Book', datePublished: now - 10, paperback: true).save()

      when:
        def publications = Publication.recentPublications.findAllPaperbackByTitle('Some Book')

      then:
        assert 2 == publications?.size()
        assert publications[0].title == 'Some Book'
        assert publications[1].title == 'Some Book'
    }

    // findby boolean queries not yet supported
    @Ignore
    void "Test named query with find by boolean property"() {

      given:
        def now = new Date()

        assert Publication.newInstance(title: 'Some Book', datePublished: now - 900, paperback: false).save()
        assert Publication.newInstance(title: 'Some Book', datePublished: now - 900, paperback: false).save()
        assert Publication.newInstance(title: 'Some Book', datePublished: now - 10, paperback: true).save()
        assert Publication.newInstance(title: 'Some Book', datePublished: now - 10, paperback: true).save()

      when:
        def publication = Publication.recentPublications.findPaperbackByTitle('Some Book')

      then:
        assert publication.title == 'Some Book'
    }


    void "Test named query with countBy*() dynamic finder"() {
      given:
        def now = new Date()
        3.times {
            assert Publication.newInstance(title: "Some Book",
                    datePublished: now - 10).save()
            assert Publication.newInstance(title: "Some Other Book",
                    datePublished: now - 10).save()
            assert Publication.newInstance(title: "Some Book",
                    datePublished: now - 900).save(flush:true)
        }
        session.clear()

      when:
        def numberOfNewBooksNamedSomeBook = Publication.recentPublications.countByTitle('Some Book')

      then:
        assert 3 == numberOfNewBooksNamedSomeBook
    }

    @Ignore // list order by not yet supported
    void "Test named query with listOrderBy*() dynamic finder"() {

      given:
        def now = new Date()

        assert Publication.newInstance(title: "Book 1", datePublished: now).save()
        assert Publication.newInstance(title: "Book 5", datePublished: now).save()
        assert Publication.newInstance(title: "Book 3", datePublished: now - 900).save()
        assert Publication.newInstance(title: "Book 2", datePublished: now - 900).save()
        assert Publication.newInstance(title: "Book 4", datePublished: now).save(flush:true)
        session.clear()

      when:
        def publications = Publication.recentPublications.listOrderByTitle()

      then:
        assert 3 == publications?.size()
        assert 'Book 1' == publications[0].title
        assert 'Book 4' == publications[1].title
        assert'Book 5'== publications[2].title

    }

    void "Test get with id of object which does not match criteria"() {

      given:
        def now = new Date()
        def hasBookInTitle = Publication.newInstance(title: "Book 1",
                datePublished: now - 10).save()
        assert hasBookInTitle
        def doesNotHaveBookInTitle = Publication.newInstance(title: "Some Publication",
                datePublished: now - 900).save(flush:true)



        session.clear()

      when:
        def result = Publication.publicationsWithBookInTitle.get(doesNotHaveBookInTitle.id)

      then:
        result == null
    }

    void "Test get method returns correct object"() {

      given:
        def now = new Date()
        def newPublication = Publication.newInstance(title: "Some New Book",
                datePublished: now - 10).save()
        assert newPublication
        def oldPublication = Publication.newInstance(title: "Some Old Book",
                datePublished: now - 900).save(flush:true)
        assert oldPublication

        session.clear()

      when:
        def publication = Publication.recentPublications.get(newPublication.id)

      then:
        assert publication != null
        assert 'Some New Book' == publication.title
    }



    void "Test get method returns null"() {

      given:
        def now = new Date()
        def newPublication = Publication.newInstance(title: "Some New Book",
                datePublished: now - 10).save()
        assert newPublication
        def oldPublication = Publication.newInstance(title: "Some Old Book",
                datePublished: now - 900).save(flush:true)
        assert oldPublication

        session.clear()

      when:
        def publication = Publication.recentPublications.get(42 + oldPublication.id)

      then:
        assert publication == null
    }

    void "Test count method following named criteria"() {

      given:
        def now = new Date()
        def newPublication = Publication.newInstance(title: "Book Some New ",
                datePublished: now - 10).save()
        assert newPublication
        def oldPublication = Publication.newInstance(title: "Book Some Old ",
                datePublished: now - 900).save(flush:true)
        assert oldPublication

        session.clear()


      when:
        def publicationsWithBookInTitleCount = Publication.publicationsWithBookInTitle.count()
        def recentPublicationsCount = Publication.recentPublications.count()
      then:
        2 == publicationsWithBookInTitleCount
        1 == recentPublicationsCount
    }

    void "Test count with parameterized named query"() {

      given:
        def now = new Date()
        assert Publication.newInstance(title: "Book",
                datePublished: now - 10).save()
        assert Publication.newInstance(title: "Book",
                datePublished: now - 10).save()
        assert Publication.newInstance(title: "Book",
                datePublished: now - 900).save(flush:true)

        session.clear()

      when:
        def recentPublicationsCount = Publication.recentPublicationsByTitle('Book').count()

      then:
        assert 2 == recentPublicationsCount
    }

    void "Test max parameter"() {
      given:
        (1..25).each {num ->
            Publication.newInstance(title: "Book Number ${num}",
                    datePublished: new Date()).save()
        }

      when:
        def pubs = Publication.recentPublications.list(max: 10)
      then:
        assert 10 == pubs?.size()
    }

    void "Test max results"() {
      given:
        (1..25).each {num ->
            Publication.newInstance(title: 'Book Title',
                    datePublished: new Date() + num).save()
        }

      when:
        def pubs = Publication.latestBooks.list()

      then:
        assert 10 == pubs?.size()
    }


    void "Test findAllWhere method combined with named query"() {
      given:
        def now = new Date()
        (1..5).each {num ->
            3.times {
                assert Publication.newInstance(title: "Book Number ${num}",
                        datePublished: now).save()
            }
        }

      when:
        def pubs = Publication.recentPublications.findAllWhere(title: 'Book Number 2')
      then:
        assert 3 == pubs?.size()
    }

    void "Test findAllWhere method with named query and disjunction"() {

      given:
        def now = new Date()
        def oldDate = now - 2000

        assert Publication.newInstance(title: 'New Paperback', datePublished: now, paperback: true).save()
        assert Publication.newInstance(title: 'New Paperback', datePublished: now, paperback: true).save()
        assert Publication.newInstance(title: 'Old Paperback', datePublished: oldDate, paperback: true).save()
        assert Publication.newInstance(title: 'New Hardback', datePublished: now, paperback: false).save()
        assert Publication.newInstance(title: 'Old Hardback', datePublished: oldDate, paperback: false).save(flush:true)
        session.clear()

      when:
        def publications = Publication.paperbackOrRecent.findAllWhere(title: 'Old Paperback')
      then:
        assert 1 == publications?.size()

      when:
        publications = Publication.paperbackOrRecent.findAllWhere(title: 'Old Hardback')
      then:
        assert 0 == publications?.size()

      when:
        publications = Publication.paperbackOrRecent.findAllWhere(title: 'New Paperback')
      then:
        assert 2 == publications?.size()
    }

    void "Test get with parameterized named query"() {

      given:
        def now = new Date()
        def recentPub = Publication.newInstance(title: "Some Title",
                            datePublished: now).save()
        def oldPub = Publication.newInstance(title: "Some Title",
                            datePublished: now - 900).save()

      when:
        def pub = Publication.recentPublicationsByTitle('Some Title').get(oldPub.id)
      then:
        pub == null

      when:
        pub = Publication.recentPublicationsByTitle('Some Title').get(recentPub.id)
      then:
        assert recentPub.id == pub?.id
    }

    void "Test named query with one parameter"() {

      given:
        def now = new Date()
        (1..5).each {num ->
            3.times {
                assert Publication.newInstance(title: "Book Number ${num}",
                        datePublished: now).save()
            }
        }

      when:
        def pubs = Publication.recentPublicationsByTitle('Book Number 2').list()

      then:
        assert 3 == pubs?.size()
    }

    void "Test named query with multiple parameters"() {

      given:
        def now = new Date()
        (1..5).each {num ->
            assert Publication.newInstance(title: "Book Number ${num}",
                    datePublished: ++now).save()
        }

      when:
        def pubs = Publication.publishedBetween(now-2, now).list()

      then:
        assert 3 == pubs?.size()
    }

    void "Test named query with multiple parameters and dynamic finder"() {
      given:
        def now = new Date()
        (1..5).each {num ->
            assert Publication.newInstance(title: "Book Number ${num}",
                    datePublished: now + num).save()
            assert Publication.newInstance(title: "Another Book Number ${num}",
                    datePublished: now + num).save()
        }

      when:
        def pubs = Publication.publishedBetween(now, now + 2).findAllByTitleLike('Book%')

      then:
        2 == pubs?.size()
    }

    void "Test named query with multiple parameters and map"() {

      given:
        def now = new Date()
        (1..10).each {num ->
            assert Publication.newInstance(title: "Book Number ${num}",
                    datePublished: ++now).save()
        }

      when:
        def pubs = Publication.publishedBetween(now-8, now-2).list(offset:2, max: 4)

      then:
        4 == pubs?.size()
    }

    void "Test findWhere with named query"() {

      given:
        def now = new Date()
        (1..5).each {num ->
            3.times {
                assert Publication.newInstance(title: "Book Number ${num}",
                        datePublished: now).save()
            }
        }

      when:
        def pub = Publication.recentPublications.findWhere(title: 'Book Number 2')
      then:
        assert 'Book Number 2' == pub.title
    }
}
class PlantCategory implements Serializable{
    Long id
    Long version
    Set plants
    String name

    static hasMany = [plants:Plant]

    static namedQueries = {
//        withPlantsInPatch {
//            plants {
//                eq 'goesInPatch', true
//            }
//        }
//        withPlantsThatStartWithG {
//            plants {
//                like 'name', 'G%'
//            }
//        }
//        withPlantsInPatchThatStartWithG {
//            withPlantsInPatch()
//            withPlantsThatStartWithG()
//        }
    }
}
class Plant implements Serializable{
    Long id
    Long version
    boolean goesInPatch
    String name


}

class Publication implements Serializable{
   Long id
   Long version
   String title
   Date datePublished
   Boolean paperback = true

   static mapping = {
     title index:true
     paperback index:true
     datePublished index:true
   }

   static namedQueries = {
       recentPublications {
           def now = new Date()
           gt 'datePublished', now - 365
       }

       publicationsWithBookInTitle {
           like 'title', 'Book%'
       }

       recentPublicationsByTitle { title ->
           recentPublications()
           eq 'title', title
       }

       latestBooks {
           maxResults(10)
           order("datePublished", "desc")
       }

       publishedBetween { start, end ->
           between 'datePublished', start, end
       }

       publishedAfter { date ->
           gt 'datePublished', date
       }

       paperbackOrRecent {
           or {
                def now = new Date()
                gt 'datePublished', now - 365
                paperbacks()
           }
       }

       paperbacks {
          eq 'paperback', true
       }

       paperbackAndRecent {
           paperbacks()
           recentPublications()
       }

       thisWeeksPaperbacks() {
           paperbacks()
           def today = new Date()
           publishedBetween(today - 7, today)
       }
   }
}