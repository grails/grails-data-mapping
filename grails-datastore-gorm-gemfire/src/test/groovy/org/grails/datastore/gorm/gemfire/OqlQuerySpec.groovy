package org.grails.datastore.gorm.gemfire

import grails.gorm.tests.GormDatastoreSpec
import grails.gorm.tests.Plant

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: Oct 7, 2010
 * Time: 2:40:43 PM
 * To change this template use File | Settings | File Templates.
 */
class OqlQuerySpec extends GormDatastoreSpec{

  void "test executeQuery method"() {
    given:
      new Plant(name:"rose", goesInPatch:false).save()
      new Plant(name:"oak", goesInPatch:false).save()
      new Plant(name:"daisy", goesInPatch:true).save(flush:true)

    when:
      def results = Plant.executeQuery("goesInPatch = false")

    then:
      results.size() == 2
  }

  void "test executeQuery method with positional parameters"() {
    given:
      new Plant(name:"rose", goesInPatch:false).save()
      new Plant(name:"oak", goesInPatch:false).save()
      new Plant(name:"daisy", goesInPatch:true).save(flush:true)

    when:
      def results = Plant.executeQuery('goesInPatch = $1', [false])

    then:
      results.size() == 2
  }

 void "test findAll method"() {
    given:
      new Plant(name:"rose", goesInPatch:false).save()
      new Plant(name:"oak", goesInPatch:false).save()
      new Plant(name:"daisy", goesInPatch:true).save(flush:true)

    when:
      def results = Plant.findAll("goesInPatch = false")

    then:
      results.size() == 2
  }

   void "test findAll method with arguments"() {
      given:
        new Plant(name:"rose", goesInPatch:false).save()
        new Plant(name:"oak", goesInPatch:false).save()
        new Plant(name:"daisy", goesInPatch:true).save(flush:true)

      when:
        def results = Plant.findAll("goesInPatch = false", [sort:'name'])

      then:
        results.size() == 2
        results[0].name == 'oak'
        results[1].name == 'rose'

      when:
        results = Plant.findAll("goesInPatch = false", [sort:'name', order:"desc"])

      then:
        results.size() == 2
        results[0].name == 'rose'
        results[1].name == 'oak'     
   }

  void "test findAll method with positional parameters"() {
    given:
      new Plant(name:"rose", goesInPatch:false).save()
      new Plant(name:"oak", goesInPatch:false).save()
      new Plant(name:"daisy", goesInPatch:true).save(flush:true)

    when:
      def results = Plant.findAll('goesInPatch = $1', [false])

    then:
      results.size() == 2
  }


  void "test findAll method with positional parameters and arguments"() {
    given:
      new Plant(name:"rose", goesInPatch:false).save()
      new Plant(name:"oak", goesInPatch:false).save()
      new Plant(name:"daisy", goesInPatch:true).save(flush:true)

    when:
      def results = Plant.findAll('goesInPatch = $1', [false], [sort:'name'])

    then:
      results.size() == 2
      results[0].name == 'oak'
      results[1].name == 'rose'

  }

  void "test find method"() {
    given:
      new Plant(name:"rose", goesInPatch:false).save()
      new Plant(name:"oak", goesInPatch:false).save()
      new Plant(name:"daisy", goesInPatch:true).save(flush:true)

    when:
      def result = Plant.find("name = 'rose'")

    then:
      result != null
      result.name == 'rose'
  }

  void "test find method with positional parameters"() {
    given:
      new Plant(name:"rose", goesInPatch:false).save()
      new Plant(name:"oak", goesInPatch:false).save()
      new Plant(name:"daisy", goesInPatch:true).save(flush:true)

    when:
      def result = Plant.find('name = $1', ['rose'])

    then:
      result != null
      result.name == 'rose'
  }
}
