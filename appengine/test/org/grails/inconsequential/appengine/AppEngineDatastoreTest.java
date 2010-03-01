package org.grails.inconsequential.appengine;

import com.google.appengine.api.datastore.*;
import org.grails.inconsequential.Key;
import org.grails.inconsequential.appengine.testsupport.AppEngineDatastoreTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Guillaume Laforge
 */
public class AppEngineDatastoreTest extends AppEngineDatastoreTestCase {

    private DatastoreService service = DatastoreServiceFactory.getDatastoreService();

    private Map<String, String> personOne;
    private Map<String, String> personTwo;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        personOne = new HashMap<String, String>();
        personOne.put("firstname", "Guillaume");
        personOne.put("lastname", "Laforge");

        personTwo = new HashMap<String, String>();
        personTwo.put("firstname", "Jar Jar");
        personTwo.put("lastname", "Binks");
    }

    public void testStore() {
        AppEngineDatastore engineDatastore = new AppEngineDatastore();

        engineDatastore.store(null, "persons", personOne);
        engineDatastore.store(null, "persons", personTwo);

        List<Entity> results = service.prepare(new Query("persons")).asList(FetchOptions.Builder.withLimit(100));
        assertEquals(2, results.size());
    }

    public void testStoreAndRetreiveOneEntity() {
        AppEngineDatastore engineDatastore = new AppEngineDatastore();

        Key key = (Key) engineDatastore.store(null, "persons", personOne);

        Map<String, Object> result = engineDatastore.retrieve(null, key);
        assertEquals("Guillaume", result.get("firstname"));
        assertEquals("Laforge", result.get("lastname"));
    }

    public void testStoreAndRetreiveTwoEntities() {
        AppEngineDatastore engineDatastore = new AppEngineDatastore();

        Key keyGuillaume = (Key) engineDatastore.store(null, "persons", personOne);
        Key keyJarJar = (Key) engineDatastore.store(null, "persons", personTwo);

        List<Map<String, Object>> result = engineDatastore.retrieve(null, keyGuillaume, keyJarJar);
        assertEquals(2, result.size());
    }

    public void testStoreAndDelete() {
        AppEngineDatastore engineDatastore = new AppEngineDatastore();

        Key keyGuillaume = (Key) engineDatastore.store(null, "persons", personOne);

        Map<String, Object> result = engineDatastore.retrieve(null, keyGuillaume);
        assertNotNull(result);

        engineDatastore.delete(null, keyGuillaume);

        Map<String, Object> resultAfterDeletion = engineDatastore.retrieve(null, keyGuillaume);
        assertNull(resultAfterDeletion);
    }
}
