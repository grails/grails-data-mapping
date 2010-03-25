package org.grails.inconsequential.appengine;

import com.google.appengine.api.datastore.*;
import org.grails.inconsequential.*;
import org.grails.inconsequential.Key;
import org.grails.inconsequential.Transaction;
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

        Key key = engineDatastore.store(null, "persons", personOne);

        Map<String, Object> result = engineDatastore.retrieve(null, key);
        assertEquals("Guillaume", result.get("firstname"));
        assertEquals("Laforge", result.get("lastname"));
    }

    public void testStoreAndRetreiveTwoEntities() {
        AppEngineDatastore engineDatastore = new AppEngineDatastore();

        Key keyGuillaume = engineDatastore.store(null, "persons", personOne);
        Key keyJarJar = engineDatastore.store(null, "persons", personTwo);

        List<Map<String, Object>> result = engineDatastore.retrieve(null, keyGuillaume, keyJarJar);
        assertEquals(2, result.size());
    }

    public void testStoreAndDelete() {
        AppEngineDatastore engineDatastore = new AppEngineDatastore();

        Key keyGuillaume = engineDatastore.store(null, "persons", personOne);

        Map<String, Object> result = engineDatastore.retrieve(null, keyGuillaume);
        assertNotNull(result);

        engineDatastore.delete(null, keyGuillaume);

        Map<String, Object> resultAfterDeletion = engineDatastore.retrieve(null, keyGuillaume);
        assertNull(resultAfterDeletion);
    }

    public void testTransactionCommit() {
        AppEngineDatastore engineDatastore = new AppEngineDatastore();
        Connection connection = engineDatastore.connect(new HashMap<String, String>());
        Context context = connection.createContext();

        // start a transaction
        Transaction transaction = context.beginTransaction();

        // add a new person in the store
        Key keyGuillaume = engineDatastore.store(context, "persons", personOne);

        // commit the transaction
        transaction.commit();

        Map<String, Object> result = engineDatastore.retrieve(context, keyGuillaume);
        // if the transaction was committed successfully, we should find a result in the store
        assertNotNull(result);
    }

    public void testTransactionRollback() {
        AppEngineDatastore engineDatastore = new AppEngineDatastore();
        Connection connection = engineDatastore.connect(new HashMap<String, String>());
        Context context = connection.createContext();

        Transaction transaction = context.beginTransaction();

        // add a new person in the store
        Key keyGuillaume = engineDatastore.store(context, "persons", personOne);

        transaction.rollback();

        Map<String, Object> result = engineDatastore.retrieve(context, keyGuillaume);
        // as the transaction was rollbacked, we shouldn't find a result in the store
        assertNull(result);

    }
}
