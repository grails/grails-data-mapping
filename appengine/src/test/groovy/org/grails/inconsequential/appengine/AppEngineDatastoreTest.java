package org.grails.inconsequential.appengine;

import com.google.appengine.api.datastore.*;
import org.grails.inconsequential.core.Key;
import org.grails.inconsequential.appengine.testsupport.AppEngineDatastoreTestCase;
import org.grails.inconsequential.core.*;
import org.grails.inconsequential.kv.KeyValueDatastoreConnection;
import org.grails.inconsequential.tx.Transaction;

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
        KeyValueDatastoreConnection conn = (KeyValueDatastoreConnection) engineDatastore.connect(null);
        conn.store("persons", personOne);
        conn.store("persons", personTwo);

        List<Entity> results = service.prepare(new Query("persons")).asList(FetchOptions.Builder.withLimit(100));
        assertEquals(2, results.size());
    }

    public void testStoreAndRetreiveOneEntity() {
        AppEngineDatastore engineDatastore = new AppEngineDatastore();

        KeyValueDatastoreConnection conn = (KeyValueDatastoreConnection) engineDatastore.connect(null);

        Key key = conn.store("persons", personOne);

        Map<String, Object> result = conn.retrieve(key);
        assertEquals("Guillaume", result.get("firstname"));
        assertEquals("Laforge", result.get("lastname"));
    }

    public void testStoreAndRetreiveTwoEntities() {
        AppEngineDatastore engineDatastore = new AppEngineDatastore();
        KeyValueDatastoreConnection conn = (KeyValueDatastoreConnection) engineDatastore.connect(null);

        Key keyGuillaume = conn.store("persons", personOne);
        Key keyJarJar = conn.store("persons", personTwo);

        List<Map<String, Object>> result = conn.retrieve(keyGuillaume, keyJarJar);
        assertEquals(2, result.size());
    }

    public void testStoreAndDelete() {
        AppEngineDatastore engineDatastore = new AppEngineDatastore();

        KeyValueDatastoreConnection conn = (KeyValueDatastoreConnection) engineDatastore.connect(null);

        Key keyGuillaume = conn.store("persons", personOne);

        Map<String, Object> result = conn.retrieve(keyGuillaume);
        assertNotNull(result);

        conn.delete(keyGuillaume);

        Map<String, Object> resultAfterDeletion = conn.retrieve(keyGuillaume);
        assertNull(resultAfterDeletion);
    }

    public void testTransactionCommit() {
        AppEngineDatastore engineDatastore = new AppEngineDatastore();
        KeyValueDatastoreConnection connection = (KeyValueDatastoreConnection) engineDatastore.connect(new HashMap<String, String>());

        // start a transaction
        Transaction transaction = connection.beginTransaction();

        // add a new person in the store
        Key keyGuillaume = connection.store("persons", personOne);

        // commit the transaction
        transaction.commit();

        Map<String, Object> result = connection.retrieve(keyGuillaume);
        // if the transaction was committed successfully, we should find a result in the store
        assertNotNull(result);
    }

    public void testTransactionRollback() {
        AppEngineDatastore engineDatastore = new AppEngineDatastore();
        KeyValueDatastoreConnection connection = (KeyValueDatastoreConnection) engineDatastore.connect(new HashMap<String, String>());

        org.grails.inconsequential.tx.Transaction transaction = connection.beginTransaction();

        // add a new person in the store
        Key keyGuillaume = connection.store("persons", personOne);

        transaction.rollback();

        Map<String, Object> result = connection.retrieve(keyGuillaume);
        // as the transaction was rollbacked, we shouldn't find a result in the store
        assertNull(result);

    }
}
