package org.grails.datastore.mapping.dynamodb.engine;

import java.util.HashMap;
import java.util.Map;

import org.grails.datastore.mapping.core.OptimisticLockingException;
import org.grails.datastore.mapping.dynamodb.DynamoDBDatastore;
import org.grails.datastore.mapping.dynamodb.util.DynamoDBConst;
import org.grails.datastore.mapping.dynamodb.util.DynamoDBUtil;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.springframework.dao.DataAccessException;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodb.model.AttributeValue;

/**
 * Implementation of HiLo generator for DynamoDB.
 * All HiLows are stored in a single dedicated AWS table. Id of each record is the corresponding table name of the
 * {@link org.grails.datastore.mapping.model.PersistentEntity}. The only attributes are the nextHi long attribute and the version.
 *
 * @author Roman Stepanenko
 */
public class DynamoDBHiLoIdGenerator implements DynamoDBIdGenerator {
    /**
     * @param table   table where the all the counters are stored
     * @param id       name of the domain for some {@link org.grails.datastore.mapping.model.PersistentEntity} for which this instance will be keeping the counter
     * @param datastore
     */
    public DynamoDBHiLoIdGenerator(String table, String id, int lowSize, DynamoDBDatastore datastore) {
        this.table = table;
        this.id = id;
        this.lowSize = lowSize;
        this.datastore = datastore;
    }

    public synchronized Object generateIdentifier(PersistentEntity persistentEntity, DynamoDBNativeItem nativeEntry) {
        if (!initialized) {
            initialize(persistentEntity);
        }
        if (current == max) {
            incrementDBAndRefresh(persistentEntity);
            reset();
        }

        long result = current;
        current = current + 1;
        return result;
    }

    private void reset() {
        current = currentHi * lowSize;
        max = current + lowSize;
    }

    private void incrementDBAndRefresh(PersistentEntity persistentEntity) {
        boolean done = false;
        int attempt = 0;
        while (!done) {
            attempt++;
            if (attempt > 10000) {//todo - make configurable at some point
                throw new IllegalArgumentException("exceeded number of attempts to load new Hi value value from db");
            }
            try {
                Map<String,AttributeValue> item = datastore.getDynamoDBTemplate().getConsistent(table, DynamoDBUtil.createIdKey(id));

                if (item == null) {//no record exist yet
                    currentHi = 1;
                    currentVersion = null;
                } else {
                    currentHi = Long.parseLong(DynamoDBUtil.getAttributeValueNumeric(item, DynamoDBConst.ID_GENERATOR_HI_LO_ATTRIBUTE_NAME));
                    currentVersion = Long.parseLong(DynamoDBUtil.getAttributeValueNumeric(item, "version"));
                }

                long nextHi = currentHi + 1;
                long nextVersion = currentVersion == null ? (long)1: currentVersion+1;

                createOrUpdate(nextHi, nextVersion, currentVersion, persistentEntity);
                currentVersion = nextVersion;

                done = true;
            } catch (OptimisticLockingException e) {
                //collition, it is expected to happen, we will try again
            }
        }
    }

    /**
     * Create table if needed.
     */
    private void initialize(PersistentEntity persistentEntity) {
        try {
            /*Map<String,AttributeValue> item =*/ datastore.getDynamoDBTemplate().getConsistent(table, DynamoDBUtil.createIdKey(id));
        } catch (DataAccessException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            //check if domain does not exist at all
            AmazonServiceException awsE = null;
            if (e instanceof AmazonServiceException) {
                awsE = (AmazonServiceException) e;
            } else if (e.getCause() instanceof AmazonServiceException) {
                awsE = (AmazonServiceException) e.getCause();
            }
            if (awsE != null && DynamoDBUtil.AWS_ERR_CODE_RESOURCE_NOT_FOUND.equals(awsE.getErrorCode())) {
                //table does not exist, must create it
                createHiLoTable(datastore, table);
            } else {
                throw new RuntimeException(e);
            }
        }

        current = 0;
        max = 0;

        initialized = true;
    }

    public static void createHiLoTable(DynamoDBDatastore datastore, String tableName) {
        datastore.getDynamoDBTemplate().createTable(
                tableName,
                DynamoDBUtil.createIdKeySchema(),
                DynamoDBUtil.createDefaultProvisionedThroughput(datastore));
    }

    private void createOrUpdate(long nextHi, long newVersion, Long expectedVersion, PersistentEntity persistentEntity) {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put(DynamoDBConst.ID_GENERATOR_HI_LO_ATTRIBUTE_NAME, new AttributeValue().withN(String.valueOf(nextHi)));
        item.put("version", new AttributeValue().withN(String.valueOf(newVersion)));
        DynamoDBUtil.addId(item, id);
        if (expectedVersion == null) {
            //since there is no record yet we can't assert on version
            datastore.getDynamoDBTemplate().putItem(table, item);
        } else {
            datastore.getDynamoDBTemplate().putItemVersioned(table, DynamoDBUtil.createIdKey(id), item, String.valueOf(expectedVersion), persistentEntity);
        }
    }

    private String id;
    private long current;
    private int lowSize;
    private long max;

    private boolean initialized;
    private long currentHi;
    private Long currentVersion;

    private DynamoDBDatastore datastore;
    private String table;
}
