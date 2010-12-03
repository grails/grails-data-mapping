package org.springframework.datastore.mapping.riak.engine;

import org.springframework.datastore.mapping.engine.PropertyValueIndexer;
import org.springframework.datastore.mapping.model.MappingContext;
import org.springframework.datastore.mapping.model.PersistentProperty;
import org.springframework.data.keyvalue.riak.core.RiakTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jon Brisbin <jon.brisbin@npcinternational.com>
 */
public class RiakPropertyValueIndexer implements PropertyValueIndexer<Long> {

  private RiakTemplate riakTemplate;
  private MappingContext mappingContext;
  private RiakEntityPersister riakEntityPersister;
  private PersistentProperty property;

  public RiakPropertyValueIndexer(RiakTemplate riakTemplate, MappingContext mappingContext, RiakEntityPersister riakEntityPersister, PersistentProperty property) {
    this.riakTemplate = riakTemplate;
    this.mappingContext = mappingContext;
    this.riakEntityPersister = riakEntityPersister;
    this.property = property;
  }

  public void index(Object value, Long primaryKey) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public List<Long> query(Object value) {
    return query(value, 0, -1);
  }

  public List<Long> query(Object value, int offset, int max) {
    String js = "function(sdata) {\n" +
        "  var data = Riak.mapValuesJson(sdata);\n" +
        "  ejsLog(\"/tmp/mapred.log\", JSON.stringify(sdata));\n" +
        "  ejsLog(\"/tmp/mapred.log\", JSON.stringify(data));\n" +
        "  return [data];\n" +
        "}";
    return new ArrayList<Long>();
  }

  public String getIndexName(Object value) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void deindex(Object value, Long primaryKey) {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}
