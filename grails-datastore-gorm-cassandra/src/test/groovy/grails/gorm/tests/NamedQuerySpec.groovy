package grails.gorm.tests

import groovy.time.TimeCategory
import spock.lang.Ignore

/**
 * @author graemerocher
 */
class NamedQuerySpec extends GormDatastoreSpec {

    void "Test named query with disjunction"() {
        given:
            def now = new Date()
            def t = TimeCategory
            def oldDate = now - 2000

            Publication.newInstance(title: 'New Paperback', datePublished: now, paperback: true).save(failOnError: true)
            Publication.newInstance(title: 'Old Paperback', datePublished: oldDate, paperback: true).save(failOnError: true)
            Publication.newInstance(title: 'New Hardback', datePublished: now, paperback: false).save(failOnError: true)
            Publication.newInstance(title: 'Old Hardback', datePublished: oldDate, paperback: false).save(failOnError: true)
            session.flush()
            session.clear()

        when:
            def publications = Publication.paperbackOrRecent.list()

        then:
            thrown UnsupportedOperationException
    }

    @Ignore
    void "Test max and offset parameter"() {
        given:
            (1..25).each {num ->
                PublicationTitlePartitionKey.newInstance(title: "Book Number ${num}",
                                        datePublished: new Date()).save()
            }

        when:
            def pubs = PublicationTitlePartitionKey.recentPublications.list(max: 10, allowFiltering: true)

        then:
            10 == pubs?.size()

        when:
            pubs = PublicationTitlePartitionKey.recentPublications.list(max: '10', offset: '5')

        then:
            thrown UnsupportedOperationException
    }

    void "Test that parameter to get is converted"() {

        given:
            def now = new Date()
            def newPublication = Publication.newInstance(title: "Some New Book", datePublished: now - 10).save(failOnError: true)
            def oldPublication = Publication.newInstance(title: "Some Old Book",
            datePublished: now - 900).save(flush:true, failOnError: true)
            session.clear()

        when:
            def publication = Publication.paperbacks.get(newPublication.id.toString())

        then:
            publication != null
            'Some New Book'== publication.title
    }

    void "Test named query with additional criteria closure"() {

        given:
            def now = new Date()
            //with title and datePublished as compound primary key, need a unique date to create a unique Publication
            6.times { cnt ->
                PublicationTitlePartitionKey.newInstance(title: "Some Book",
                                               datePublished: now - 10 - cnt).save(failOnError: true)
                PublicationTitlePartitionKey.newInstance(title: "Some Other Book",
                                               datePublished: now - 10 - cnt).save(failOnError: true)
                PublicationTitlePartitionKey.newInstance(title: "Some Book",
                                               datePublished: now - 900 - cnt, paperback:false).save(failOnError: true)
            }
            session.flush()
            session.clear()

        when:
            def publications = PublicationTitlePartitionKey.recentPublications {
                eq 'title', 'Some Book'
            }

        then:
            6 == publications?.size()

        when:
            publications = PublicationTitlePartitionKey.recentPublications {
                eq 'paperback', true
            }

        then:
            12 == publications?.size()

        when:
            def cnt = PublicationTitlePartitionKey.recentPublications.count {
                eq 'title', 'Some Book'
            }

        then:
            6 == cnt

        when:
            publications = PublicationTitlePartitionKey.recentPublications(max: 3) {
                eq 'paperback', true
            }

        then:
            3 == publications?.size()
    }

    void "Test passing parameters to additional criteria"() {
        given:
            def now = new Date()

            6.times { cnt ->
                new PublicationTitlePartitionKey(title: "Some Old Book #${cnt}",
                                datePublished: now - 1000, paperback: true).save(failOnError: true).id
                new PublicationTitlePartitionKey(title: "Some New Book #${cnt}",
                                datePublished: now, paperback: true).save(failOnError: true).id
            }

            session?.flush()

        when:
            def results = PublicationTitlePartitionKey.publishedAfter(now - 5) {
                eq 'paperback', true
            }

        then:
            6 == results?.size()

        when:
            results = PublicationTitlePartitionKey.publishedAfter(now - 5, [max: 2]) {
                eq 'paperback', true
            }

        then:
            2 == results?.size()

        when:
            results = PublicationTitlePartitionKey.publishedBetween(now - 5, now + 1) {
                eq 'paperback', true
            }

        then:
            6 == results?.size()

        when:
            results = PublicationTitlePartitionKey.publishedBetween(now - 5, now + 1, [max: 2]) {
                eq 'paperback', true
            }

        then:
            2 == results?.size()

        when:
            results = PublicationTitlePartitionKey.publishedAfter(now - 1005) {
                eq 'paperback', true
            }

        then:
            12 == results?.size()

        when:
            results = PublicationTitlePartitionKey.publishedAfter(now - 5) {
                eq 'paperback', false
            }

        then:
            0 == results?.size()

        when:
            results = PublicationTitlePartitionKey.publishedAfter(now - 5, [max: 2]) {
                eq 'paperback', false
            }

        then:
            0 == results?.size()

        when:
            results = PublicationTitlePartitionKey.publishedBetween(now - 5, now + 1) {
                eq 'paperback', false
            }

        then:
            0 == results?.size()

        when:
            results = PublicationTitlePartitionKey.publishedBetween(now - 5, now + 1, [max: 2]) {
                eq 'paperback', false
            }

        then:
            0 == results?.size()

        when:
            results = PublicationTitlePartitionKey.publishedAfter(now - 1005) {
                eq 'paperback', false
            }

        then:
            0 == results?.size()
    }

    void "Test get method followed named query chaining"() {
        given:
            def now = new Date()

            def oldPaperBackWithBookInTitleId =  new Publication(
                title: "Book 1",
                datePublished: now - 1000, paperback: true).save().id
            def newPaperBackWithBookInTitleId =  new Publication(
                title: "Book 2",
                datePublished: now, paperback: true).save().id

            session.flush()
            session.clear()

        when:
            def publication = Publication.paperbacks().publishedAfter(now - 5).get(oldPaperBackWithBookInTitleId) 

        then:
            publication == null

        when:
            publication = Publication.publishedAfter(now - 5).paperbacks().get(oldPaperBackWithBookInTitleId)

        then:
            publication == null

        when:
            publication = Publication.publishedAfter(now - 5).paperbacks().get(newPaperBackWithBookInTitleId)

        then:
            publication != null

        when:
            publication = Publication.publishedAfter(now - 5).paperbacks().get(newPaperBackWithBookInTitleId)

        then:
            publication != null
    }

    void "Test named query with findBy*() dynamic finder"() {

        given:
            def now = new Date()
            PublicationTitlePartitionKey.newInstance(title: "Book 1", datePublished: now - 900).save(failOnError: true)
            def recentBookTitle = PublicationTitlePartitionKey.newInstance(
                title: "Book 1",
                datePublished: now - 10).save(flush:true).title
            session.clear()

        when:
            def publication = PublicationTitlePartitionKey.recentPublications.findByTitle('Book 1')

        then:
            publication != null
            recentBookTitle == publication.title
    }

    void "Test named query with findAllBy*() dyamic finder"() {
        given:
            def now = new Date()
            3.times {cnt ->
                new PublicationTitlePartitionKey(title: "Some Recent Book",
                                       datePublished: now - 10 - cnt).save(failOnError: true)
                new PublicationTitlePartitionKey(title: "Some Other Book",
                                       datePublished: now - 10 - cnt).save(failOnError: true)
                new PublicationTitlePartitionKey(title: "Some Book",
                                       datePublished: now - 900 -cnt).save(flush:true, failOnError: true)
            }
            session.clear()

        when:
            def publications = PublicationTitlePartitionKey.recentPublications.findAllByTitle('Some Recent Book')

        then:
            3 == publications?.size()
            'Some Recent Book' == publications[0].title
            'Some Recent Book' == publications[1].title
            'Some Recent Book' == publications[2].title
    }

    @Ignore  // queries on associations not yet supported
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

    @Ignore  // queries on associations not yet supported
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

    @Ignore  // queries on associations not yet supported
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

    void 'Test uniqueResult'() {
        given:
            def now = new Date()

            def pub1 = new PublicationTitlePartitionKey(title: "Ten Day Old Paperback",
                            datePublished: now - 10,
                            paperback: true).save(flush: true)
            def pub2 = new PublicationTitlePartitionKey(title: "One Hundred Day Old Paperback",
                            datePublished: now - 100,
                            paperback: true).save(flush: true)
            session.clear()

        when:
            def result = PublicationTitlePartitionKey.lastPublishedBefore(now - 200).findByTitleInList([pub1.title, pub2.title], [fetchSize: Integer.MAX_VALUE])

        then:
            !result

        when:
            result = PublicationTitlePartitionKey.lastPublishedBefore(now - 50).findByTitleInList([pub1.title, pub2.title], [fetchSize: Integer.MAX_VALUE])

        then:
            'One Hundred Day Old Paperback' == result?.title
    }

    void "Test findWhere method after chaining named queries"() {
        given:
            def now = new Date()

            new PublicationTitlePartitionKey(title: "Book 1",
                            datePublished: now - 10, paperback: false).save()
            new PublicationTitlePartitionKey(title: "Book 2",
                            datePublished: now - 1000, paperback: true).save()
            new PublicationTitlePartitionKey(title: "Book 3",
                            datePublished: now - 10, paperback: true).save()

//            new PublicationTitlePartitionKey(title: "Some Title",
//                            datePublished: now - 10, paperback: false).save()
//            new PublicationTitlePartitionKey(title: "Some Title",
//                            datePublished: now - 1000, paperback: false).save()
            new PublicationTitlePartitionKey(title: "Some Title",
                            datePublished: now - 10, paperback: true).save(flush:true)
            session.clear()

        when:
            def results = PublicationTitlePartitionKey.recentPublications().paperbacks().findAllWhere(title: "Some Title")

        then:
            1 == results?.size()
    }

    void "Test named query passing multiple parameters to a nested query"() {
        given:
            def now = new Date()

            new PublicationTitlePartitionKey(title: "Some Book 1",
                            datePublished: now - 10, paperback: false).save()
            new PublicationTitlePartitionKey(title: "Some Book 2",
                            datePublished: now - 1000, paperback: true).save()
            new PublicationTitlePartitionKey(title: "Some Book 3",
                            datePublished: now - 2, paperback: true).save()

            new PublicationTitlePartitionKey(title: "Some Title 1",
                            datePublished: now - 2, paperback: false).save()
            new PublicationTitlePartitionKey(title: "Some Title 2",
                            datePublished: now - 1000, paperback: false).save()
            new PublicationTitlePartitionKey(title: "Some Title 3",
                            datePublished: now - 2, paperback: true).save(flush:true)
            session.clear()

        when:
            def results = PublicationTitlePartitionKey.thisWeeksPaperbacks().list(allowFiltering:true)

        then:
            2 == results?.size()
    }

    @Ignore
    void "Test chaining named queries"() {

        given:
            def now = new Date()
            [true, false].each { isPaperback ->
                4.times { cnt ->
                    cnt = isPaperback ? cnt : cnt + 4
                    
                    PublicationTitlePartitionKey.newInstance(
                        title: "Book Some",
                        datePublished: now - 10 - cnt, paperback: isPaperback).save()
                    PublicationTitlePartitionKey.newInstance(
                        title: "Book Some Other",
                        datePublished: now - 10 - cnt, paperback: isPaperback).save()
                    PublicationTitlePartitionKey.newInstance(
                        title: "Some Other Title",
                        datePublished: now - 10 - cnt, paperback: isPaperback).save()
                    PublicationTitlePartitionKey.newInstance(
                        title: "Book Some",
                        datePublished: now - 1000 - cnt, paperback: isPaperback).save()
                    PublicationTitlePartitionKey.newInstance(
                        title: "Book Some Other",
                        datePublished: now - 1000 - cnt, paperback: isPaperback).save()
                    PublicationTitlePartitionKey.newInstance(
                        title: "Some Other Title",
                        datePublished: now - 1000 - cnt, paperback: isPaperback).save()
                }
            }
            session.flush()
            session.clear()

        when:
            def results = PublicationTitlePartitionKey.recentPublications().paperbacks().list()

        then: "The result size should be 16 when returned from chained queries"
            12 == results?.size()

        when:
            results = PublicationTitlePartitionKey.recentPublications().paperbacks().count()
        then:
            12 == results

        when:
            results = PublicationTitlePartitionKey.recentPublications.paperbacks.list()
        then:"The result size should be 16 when returned from chained queries"
            12 == results?.size()

        when:
            results = PublicationTitlePartitionKey.recentPublications.paperbacks.count()
        then:
            12 == results

        when:
            results = PublicationTitlePartitionKey.paperbacks().recentPublications().publicationsByTitle("Book Some").list()
        then: "The result size should be 8 when returned from chained queries"
            4 ==  results?.size()

        when:
            results = PublicationTitlePartitionKey.paperbacks().recentPublications().publicationsByTitle("Book Some").count()
        then:
            4 == results

        when:
            results = PublicationTitlePartitionKey.recentPublications().publicationsByTitle("Book Some").findAllByPaperback(true)
        then: "The result size should be 8"
            4 == results?.size()

        when:
            results = PublicationTitlePartitionKey.paperbacks.recentPublications.publicationsByTitle("Book Some").list()
        then:"The result size should be 8 when returned from chained queries"
            4 == results?.size()

        when:
            results = PublicationTitlePartitionKey.paperbacks.recentPublications.publicationsByTitle("Book Some").count()
        then:
            4 == results
    }

    void "test Chaining Queries With Params"() {
        given:           
            def now = new Date()
            def lastWeek = now - 7
            def longAgo = now - 1000
            
			use(TimeCategory) {
	            2.times { cnt ->
					cnt = (cnt + 1).hour
	                PublicationTitlePartitionKey.newInstance(title: 'Some Book',
	                                               datePublished: now + cnt).save(failOnError: true)
	                PublicationTitlePartitionKey.newInstance(title: 'Some Title',
	                                               datePublished: now + cnt).save(failOnError: true)
	            }
	            3.times { cnt ->
					cnt = (cnt + 1).hour
	                PublicationTitlePartitionKey.newInstance(title: 'Some Book',
	                                               datePublished: lastWeek + cnt).save(failOnError: true)
	                PublicationTitlePartitionKey.newInstance(title: 'Some Title',
	                                               datePublished: lastWeek + cnt).save(failOnError: true)
	            }
	            4.times { cnt ->
					cnt = (cnt + 1).hour
	                PublicationTitlePartitionKey.newInstance(title: 'Some Book',
	                                               datePublished: longAgo - cnt).save(failOnError: true)
	                PublicationTitlePartitionKey.newInstance(title: 'Some Title',
	                                               datePublished: longAgo - cnt).save(failOnError: true)
	            }
			}
			session.flush()
            session.clear()
        
        when:
            def results = PublicationTitlePartitionKey.publicationsByTitle('Some Book').publishedAfter(now - 2).list()
        then: "wrong number of books were returned from chained queries"
             2 == results?.size()
        
        when: 
            results = PublicationTitlePartitionKey.publicationsByTitle('Some Book').publishedAfter(now - 2).count()
        then: 
            2 == results

        when:
            results = PublicationTitlePartitionKey.publicationsByTitle('Some Book').publishedAfter(lastWeek - 2).list()
        
        then: 'wrong number of books were returned from chained queries'
             5 == results?.size()

        when:
            results = PublicationTitlePartitionKey.publicationsByTitle('Some Book').publishedAfter(lastWeek - 2).count()
        
        then:
             5 == results
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
            def publications = PublicationTitlePartitionKey.recentPublications.list(allowFiltering: true)
        then:
            0 == publications.size()
    }

    void "Test named query with conjunction"() {
        given:
            def now = new Date()
            def oldDate = now - 2000

            Publication.newInstance(title: 'New Paperback', datePublished: now, paperback: true).save(failOnError: true)
            Publication.newInstance(title: 'Old Paperback', datePublished: oldDate, paperback: true).save(failOnError: true)
            Publication.newInstance(title: 'New Hardback', datePublished: now, paperback: false).save(failOnError: true)
            Publication.newInstance(title: 'Old Hardback', datePublished: oldDate, paperback: false).save(failOnError: true)
            session.flush()
            session.clear()

        when:
            def publications = Publication.paperbackAndRecent.list(allowFiltering: true)

        then:
            1 == publications?.size()
    }

    void "Test named query with list() method"() {

        given:
            def now = new Date()
            PublicationTitlePartitionKey.newInstance(title: "Some New Book",
                                           datePublished: now - 10).save(failOnError: true)
            PublicationTitlePartitionKey.newInstance(title: "Some Old Book",
                                           datePublished: now - 900).save(flush:true, failOnError: true)

            session.clear()

        when:
            def publications = PublicationTitlePartitionKey.recentPublications.list(allowFiltering: true)

        then:
            1 == publications?.size()
            'Some New Book' == publications[0].title
    }

    void "Test named query with findAll by boolean property"() {
        given:
            
            def now = new Date()

            PublicationTitlePartitionKey.newInstance(title: 'Some Book', datePublished: now - 900, paperback: false).save(failOnError: true)
            PublicationTitlePartitionKey.newInstance(title: 'Some Book', datePublished: now - 901, paperback: false).save(failOnError: true)
            PublicationTitlePartitionKey.newInstance(title: 'Some Book', datePublished: now - 10, paperback: true).save(failOnError: true)
            PublicationTitlePartitionKey.newInstance(title: 'Some Book', datePublished: now - 11, paperback: true).save(failOnError: true)

        when:
            def publications = PublicationTitlePartitionKey.recentPublications.findAllPaperbackByTitle('Some Book')

        then:
            2 == publications?.size()
            publications[0].title == 'Some Book'
            publications[1].title == 'Some Book'
    }

    void "Test named query with find by boolean property"() {

        given:
            def now = new Date()

            PublicationTitlePartitionKey.newInstance(title: 'Some Book', datePublished: now - 900, paperback: false).save(failOnError: true)
            PublicationTitlePartitionKey.newInstance(title: 'Some Book', datePublished: now - 901, paperback: false).save(failOnError: true)
            PublicationTitlePartitionKey.newInstance(title: 'Some Book', datePublished: now - 10, paperback: true).save(failOnError: true)
            PublicationTitlePartitionKey.newInstance(title: 'Some Book', datePublished: now - 11, paperback: true).save(failOnError: true)

        when:
            def publication = PublicationTitlePartitionKey.recentPublications.findPaperbackByTitle('Some Book')

        then:
            publication.title == 'Some Book'
    }

    void "Test named query with countBy*() dynamic finder"() {
        given:
            def now = new Date()
            3.times { cnt ->
                PublicationTitlePartitionKey.newInstance(title: "Some Book",
                                               datePublished: now - 10 - cnt).save(failOnError: true)
                PublicationTitlePartitionKey.newInstance(title: "Some Other Book",
                                               datePublished: now - 10 - cnt).save(failOnError: true)
                PublicationTitlePartitionKey.newInstance(title: "Some Book",
                                               datePublished: now - 900 - cnt).save(flush:true, failOnError: true)
            }
            session.clear()

        when:
            def numberOfNewBooksNamedSomeBook = PublicationTitlePartitionKey.recentPublications.countByTitle('Some Book')

        then:
            3 == numberOfNewBooksNamedSomeBook
    }

    void "Test named query with listOrderBy*() dynamic finder"() {

        given:
            def now = new Date()

            PublicationTitlePartitionKey.newInstance(title: "Book 1", datePublished: now).save(failOnError: true)
            PublicationTitlePartitionKey.newInstance(title: "Book 1", datePublished: now - 1).save(failOnError: true)
            PublicationTitlePartitionKey.newInstance(title: "Book 1", datePublished: now - 2).save(failOnError: true)
            PublicationTitlePartitionKey.newInstance(title: "Book 2", datePublished: now).save(failOnError: true)
            PublicationTitlePartitionKey.newInstance(title: "Book 3", datePublished: now - 100).save(flush:true, failOnError: true)
            session.clear()

        when:
            def publications = PublicationTitlePartitionKey.publicationsByTitle("Book 1").listOrderByDatePublished()

        then:
            3 == publications?.size()
            'Book 1' == publications[0].title
            'Book 1' == publications[1].title
            'Book 1'== publications[2].title
			now - 2 == publications[0].datePublished
			now - 1 == publications[1].datePublished
			now == publications[2].datePublished
    }

    void "Test get with id of object which does not match criteria"() {

        given:
            def now = new Date()
            def hasBookInTitle = Publication.newInstance(
                title: "Book 1",
                datePublished: now - 10).save(failOnError: true)
            def doesNotHaveBookInTitle = Publication.newInstance(
                title: "Some Publication",
                datePublished: now - 900).save(flush:true, failOnError: true)

            session.clear()

        when:
            def result = Publication.publicationsByTitle("Book 1").get(doesNotHaveBookInTitle.id)

        then:
            result == null
    }

    void "Test get method returns correct object"() {

        given:
            def now = new Date()
            def newPublication = Publication.newInstance(
                title: "Some New Book",
                datePublished: now - 10).save(failOnError: true)
            def oldPublication = Publication.newInstance(
                title: "Some Old Book",
                datePublished: now - 900).save(flush:true, failOnError: true)

            session.clear()

        when:
            def publication = Publication.paperbacks.get(newPublication.id)

        then:
            publication != null
            'Some New Book' == publication.title
    }

    void "Test get method returns null"() {

        given:
            def now = new Date()
            def newPublication = Publication.newInstance(
                title: "Some New Book",
                datePublished: now - 10).save(failOnError: true)
            def oldPublication = Publication.newInstance(
                title: "Some Old Book",
                datePublished: now - 900).save(flush:true, failOnError: true)

            session.clear()

        when:
            def publication = Publication.paperbacks.get(UUID.randomUUID())

        then:
            publication == null
    }

    void "Test count method following named criteria"() {

        given:
            def now = new Date()
            def newPublication = PublicationTitlePartitionKey.newInstance(
                title: "Book Some New",
                datePublished: now - 10).save(failOnError: true)
            def oldPublication = PublicationTitlePartitionKey.newInstance(
                title: "Book Some Old",
                datePublished: now - 900).save(flush:true, failOnError: true)

            session.clear()

        when:
            def publicationsWithBookInTitleCount = PublicationTitlePartitionKey.publicationsByTitles(["Book Some New", "Book Some Old"]).count()
            def recentPublicationsCount = PublicationTitlePartitionKey.recentPublications.count{
				allowFiltering true
			}

        then:
            2 == publicationsWithBookInTitleCount
            1 == recentPublicationsCount
    }

    void "Test count with parameterized named query"() {

        given:
            def now = new Date()
            Publication.newInstance(title: "Book",
                                           datePublished: now - 10).save(failOnError: true)
            Publication.newInstance(title: "Book",
                                           datePublished: now - 11).save(failOnError: true)
            Publication.newInstance(title: "Book",
                                           datePublished: now - 900).save(flush:true, failOnError: true)

            session.clear()

        when:
            def recentPublicationsCount = Publication.recentPublicationsByTitle('Book').count()

        then:
            2 == recentPublicationsCount
    }

    void "Test max parameter"() {
        given:
            (1..25).each {num ->
                PublicationTitlePartitionKey.newInstance(title: "Book Number ${num}",
                                        datePublished: new Date()).save()
            }

        when:
            def pubs = PublicationTitlePartitionKey.recentPublications.list(max: 10, allowFiltering: true)
        then:
            10 == pubs?.size()
    }

    void "Test max results"() {
        given:
            (1..25).each {num ->
                PublicationTitlePartitionKey.newInstance(title: 'Book Title',
                                        datePublished: new Date() + num).save()
            }

        when:
            def pubs = PublicationTitlePartitionKey.publicationsByTitle("Book Title").latestBooks.list()

        then:
            10 == pubs?.size()
    }

    void "Test findAllWhere method combined with named query"() {
        given:
            def now = new Date()
            (1..5).each {num ->
                3.times { cnt ->
                    Publication.newInstance(title: "Book Number ${num}",
                                            datePublished: now).save(failOnError: true)
                }
            }

        when:
            def pubs = Publication.recentPublications.findAllWhere(title: 'Book Number 2')

        then:
            3 == pubs?.size()
    }

	@Ignore //disjunction not supported
    void "Test findAllWhere method with named query and disjunction"() {

        given:
            def now = new Date()
            def oldDate = now - 2000

            Publication.newInstance(title: 'New Paperback', datePublished: now, paperback: true).save(failOnError: true)
            Publication.newInstance(title: 'New Paperback', datePublished: now, paperback: true).save(failOnError: true)
            Publication.newInstance(title: 'Old Paperback', datePublished: oldDate, paperback: true).save(failOnError: true)
            Publication.newInstance(title: 'New Hardback', datePublished: now, paperback: false).save(failOnError: true)
            Publication.newInstance(title: 'Old Hardback', datePublished: oldDate, paperback: false).save(flush:true, failOnError: true)
            session.clear()

        when:
            def publications = Publication.paperbackOrRecent.findAllWhere(title: 'Old Paperback')

        then:
            1 == publications?.size()

        when:
            publications = Publication.paperbackOrRecent.findAllWhere(title: 'Old Hardback')

        then:
            0 == publications?.size()

        when:
            publications = Publication.paperbackOrRecent.findAllWhere(title: 'New Paperback')

        then:
            2 == publications?.size()
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
            recentPub.id == pub?.id
    }

    void "Test named query with one parameter"() {

        given:
            def now = new Date()
            (1..5).each {num ->
                3.times {
                    Publication.newInstance(
                        title: "Book Number ${num}",
                        datePublished: now).save(failOnError: true)
                }
            }

        when:
            def pubs = Publication.recentPublicationsByTitle('Book Number 2').list()

        then:
            3 == pubs?.size()
    }

    void "Test named query with multiple parameters"() {

        given:
            def now = new Date()
            (1..5).each {num ->
                PublicationTitlePartitionKey.newInstance(
                    title: "Book Number ${num}",
                    datePublished: ++now).save(failOnError: true)
            }
        when:
            def pubs = PublicationTitlePartitionKey.publishedBetween(now-2, now).list(allowFiltering:true)

        then:
            3 == pubs?.size()
    }

    void "Test named query with multiple parameters and dynamic finder"() {
        given:
            def now = new Date()
            (1..5).each {num ->
                PublicationTitlePartitionKey.newInstance(
                    title: "Book Number ${num}",
                    datePublished: now + num).save(failOnError: true)
                PublicationTitlePartitionKey.newInstance(
                    title: "Another Book Number ${num}",
                    datePublished: now + num).save(failOnError: true)
            }

        when:
            def pubs = PublicationTitlePartitionKey.publishedBetween(now, now + 2).findAllByTitleInList(['Book Number 1', 'Another Book Number 2'])

        then:
            2 == pubs?.size()
    }

    void "Test named query with multiple parameters and map"() {

        given:
            def now = new Date()
            (1..10).each {num ->
                PublicationTitlePartitionKey.newInstance(
                    title: "Book Number ${num}",
                    datePublished: ++now).save(failOnError: true)
            }

        when:
            def pubs = PublicationTitlePartitionKey.publishedBetween(now-8, now-2).list(max: 4, allowFiltering: true)

        then:
            4 == pubs?.size()
    }

    void "Test findWhere with named query"() {

        given:
            def now = new Date()
            (1..5).each {num ->
                3.times {
                    PublicationTitlePartitionKey.newInstance(
                        title: "Book Number ${num} - $it",
                        datePublished: now).save(failOnError: true)
                }
            }

        when:
            def pub = PublicationTitlePartitionKey.recentPublications.findWhere(title: 'Book Number 2 - 1')
        then:
            'Book Number 2 - 1' == pub.title
    }
}

