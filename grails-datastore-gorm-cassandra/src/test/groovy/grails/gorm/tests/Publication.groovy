package grails.gorm.tests



import grails.gorm.CassandraEntity

@CassandraEntity
class Publication implements Serializable {    
    UUID id
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

        lastPublishedBefore { date ->
            uniqueResult = true
            le 'datePublished', date
            order 'datePublished', 'desc'
        }

        recentPublications {
            def now = new Date()
            gt 'datePublished', now - 365  
			allowFiltering true
        }

        publicationsByTitles { titles ->
            'in' 'title', titles
        }
        
        publicationsByTitle { title ->
            eq 'title', title
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
            allowFiltering true
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