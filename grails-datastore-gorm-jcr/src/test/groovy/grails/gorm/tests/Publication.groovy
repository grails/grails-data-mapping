@grails.persistence.Entity
class Publication implements Serializable{
   String id
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