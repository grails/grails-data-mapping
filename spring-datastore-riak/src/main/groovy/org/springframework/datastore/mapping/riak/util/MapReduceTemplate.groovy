package org.springframework.datastore.mapping.riak.util

import org.antlr.stringtemplate.AttributeRenderer
import org.antlr.stringtemplate.StringTemplate
import org.antlr.stringtemplate.StringTemplateGroup

/**
 * @author Jon Brisbin <jon.brisbin@npcinternational.com>
 */
class MapReduceTemplate {

  static StringTemplateGroup stg
  static {
    stg = new StringTemplateGroup("mapReduce")
  }

  StringTemplate template

  MapReduceTemplate(String uri) {
    template = stg.getInstanceOf(uri)
    template.registerRenderer(Date, [toString: { d -> d.time }] as AttributeRenderer)
    template.registerRenderer(String, [toString: { s -> "\"$s\"" }] as AttributeRenderer)
  }

  String run(Map params) {
    params.each { k, v ->
      template.setAttribute(k, v)
    }
  }
}
