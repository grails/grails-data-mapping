package org.grails.datastore.mapping.simpledb.engine;

import java.util.LinkedList;
import java.util.List;

import org.grails.datastore.mapping.core.OptimisticLockingException;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.simpledb.util.SimpleDBConst;
import org.grails.datastore.mapping.simpledb.util.SimpleDBTemplate;
import org.grails.datastore.mapping.simpledb.util.SimpleDBUtil;
import org.springframework.dao.DataAccessException;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;

/**
 * Implementation of HiLo generator for SimpleDB.
 * All HiLows are stored in a single dedicated AWS domain. Id of each record is the corresponding domain name of the
 * {@link PersistentEntity}. The only attributes are the nextHi long attribute and the version.
 *
 * @author Roman Stepanenko
 */
public class SimpleDBHiLoIdGenerator implements SimpleDBIdGenerator {
    /**
     * @param domain   domain where the all the counters are stored
     * @param id       name of the domain for some {@link PersistentEntity} for which this instance will be keeping the counter
     * @param template
     */
    public SimpleDBHiLoIdGenerator(String domain, String id, int lowSize, SimpleDBTemplate template) {
        this.domain = domain;
        this.id = id;
        this.lowSize = lowSize;
        this.template = template;
    }

    public synchronized Object generateIdentifier(PersistentEntity persistentEntity, SimpleDBNativeItem nativeEntry) {
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
                Item item = template.getConsistent(domain, id);

                if (item == null) {//no record exist yet
                    currentHi = 1;
                    currentVersion = null;
                } else {
                    List<String> hiAttributes = SimpleDBUtil.collectAttributeValues(item, SimpleDBConst.ID_GENERATOR_HI_LO_ATTRIBUTE_NAME);
                    currentHi = Long.parseLong(hiAttributes.get(0));
                    currentVersion = Long.parseLong(SimpleDBUtil.collectAttributeValues(item, "version").get(0));
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
     * Create domain if needed.
     */
    private void initialize(PersistentEntity persistentEntity) {
        try {
            /*Item item =*/ template.getConsistent(domain, id);
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
            if (awsE != null && SimpleDBUtil.AWS_ERR_CODE_NO_SUCH_DOMAIN.equals(awsE.getErrorCode())) {
                //domain does not exist, must create it
                template.createDomain(domain);
            } else {
                throw new RuntimeException(e);
            }
        }

        current = 0;
        max = 0;

        initialized = true;
    }

    private void createOrUpdate(long nextHi, long newVersion, Long expectedVersion, PersistentEntity persistentEntity) {
        List<ReplaceableAttribute> newValues = new LinkedList<ReplaceableAttribute>();
        newValues.add(new ReplaceableAttribute(SimpleDBConst.ID_GENERATOR_HI_LO_ATTRIBUTE_NAME, String.valueOf(nextHi), true));
        newValues.add(new ReplaceableAttribute("version", String.valueOf(newVersion), true));
        if (expectedVersion == null) {
            //since there is no record yet we can't assert on version
            template.putAttributes(domain, id, newValues);
        } else {
            template.putAttributesVersioned(domain, id, newValues, String.valueOf(expectedVersion), persistentEntity);
        }
    }

    private String id;
    private long current;
    private int lowSize;
    private long max;

    private boolean initialized;
    private long currentHi;
    private Long currentVersion;

    private SimpleDBTemplate template;
    private String domain;
}
