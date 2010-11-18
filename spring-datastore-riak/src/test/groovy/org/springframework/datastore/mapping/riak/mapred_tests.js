var log = {
  debug: function(msg) {
    if (typeof msg == 'object') {
      msg = JSON.stringify(msg);
    }
    ejsLog('/tmp/mapred.log', 'DEBUG: ' + msg);
  }
}

var RiakHelper = {
  map: function(data, ifclause) {
    var entry = Riak.mapValuesJson(data)[0];
    var r = [];
    if (ifclause(entry)) {
      entry.id = data.key;
      r.push(entry);
    }
    return r;
  },
  eq: function(entry, name, value) {
    return (typeof entry[name] != 'undefined') && (entry[name] == value);
  }
}