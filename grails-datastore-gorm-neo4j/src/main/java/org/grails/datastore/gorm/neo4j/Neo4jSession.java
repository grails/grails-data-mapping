package org.grails.datastore.gorm.neo4j;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.iterators.FilterIterator;
import org.grails.datastore.gorm.neo4j.engine.CypherEngine;
import org.grails.datastore.gorm.neo4j.simplegraph.Relationship;
import org.grails.datastore.mapping.core.AbstractSession;
import org.grails.datastore.mapping.core.Datastore;
import org.grails.datastore.mapping.core.impl.PendingInsert;
import org.grails.datastore.mapping.core.impl.PendingOperation;
import org.grails.datastore.mapping.core.impl.PendingOperationExecution;
import org.grails.datastore.mapping.core.impl.PendingUpdate;
import org.grails.datastore.mapping.dirty.checking.DirtyCheckable;
import org.grails.datastore.mapping.engine.Persister;
import org.grails.datastore.mapping.model.MappingContext;
import org.grails.datastore.mapping.model.PersistentEntity;
import org.grails.datastore.mapping.transactions.Transaction;
import org.neo4j.cypher.export.DatabaseSubGraph;
import org.neo4j.cypher.export.SubGraphExporter;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.helpers.collection.IteratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import javax.persistence.FlushModeType;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.*;

/**
 * Created by stefan on 03.03.14.
 */
public class Neo4jSession extends AbstractSession<ExecutionEngine> {

    private static Logger log = LoggerFactory.getLogger(Neo4jSession.class);

    private CypherEngine cypherEngine;

    /** map node id -> hashmap of relationship types showing startNode id and endNode id */
    private Collection<Relationship> persistentRelationships = new HashSet<Relationship>();
    private Collection<Object> persistingInstances = new HashSet<Object>();
    private GraphDatabaseService graphDatabaseService;

    public Neo4jSession(Datastore datastore, MappingContext mappingContext, ApplicationEventPublisher publisher, boolean stateless, CypherEngine cypherEngine, GraphDatabaseService graphDatabaseService) {
        super(datastore, mappingContext, publisher, stateless);
        this.cypherEngine = cypherEngine;
        this.graphDatabaseService = graphDatabaseService;
    }

    @Override
    protected Persister createPersister(Class cls, MappingContext mappingContext) {
        final PersistentEntity entity = mappingContext.getPersistentEntity(cls.getName());
        return (entity != null ) ? new Neo4jEntityPersister(mappingContext, entity, this, publisher) : null;
    }

    @Override
    protected Transaction beginTransactionInternal() {
        return new Neo4jTransaction(cypherEngine);
    }

    @Override
    public Neo4jDatastore getDatastore() {
        return (Neo4jDatastore) super.getDatastore();
    }

    @Override
    public CypherEngine getNativeInterface() {
        return cypherEngine;
    }

    @Override
    protected void flushPendingInserts(Map<PersistentEntity, Collection<PendingInsert>> inserts) {

        Collection<PendingInsert> nodes = new ArrayList<PendingInsert>();
        Collection<PendingInsert> relationships = new ArrayList<PendingInsert>();
        for (Collection<PendingInsert> coll : inserts.values()) {
            for (PendingInsert pendingInsert: coll)  {
                if (pendingInsert instanceof NodePendingInsert) {
                    nodes.add(pendingInsert);
                }
                if (pendingInsert instanceof RelationshipPendingInsert) {
                    relationships.add(pendingInsert);
                }

            }
        }
        flushPendingOperations(nodes);

        flushPendingOperations(relationships);
    }


    /**
     * TODO: find clean solution instead of copying from base class

     * @param operations
     */
    private void flushPendingOperations(Collection operations) {

        for (Object o : operations) {
            PendingOperation pendingOperation = (PendingOperation) o;
            try {
                PendingOperationExecution.executePendingOperation(pendingOperation);
            } catch (RuntimeException e) {
                setFlushMode(FlushModeType.COMMIT);
                throw new InvalidDataAccessResourceUsageException("Do not flush() the Session after an exception occurs", e);
            }
        }
    }

/*    void setPersistentRelationships(Long id, Map<String, Collection<Collection<Long>>> relationships) {
        persistentRelationships.put(id, relationships)
    }

    Map<String, Collection<Collection<Long>>> getPersistentRelationships(Long id) {
        persistentRelationships.get(id)
    }*/

    @Override
    public void flush() {
        persistDirtyButUnsavedInstances();
        super.flush();
        persistingInstances.clear();

//        if (log.isDebugEnabled()) {
            // TODO: remove debugging stuff here
            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            new SubGraphExporter(new DatabaseSubGraph(graphDatabaseService)).export(printWriter);

            log.info(writer.toString());
            log.info("svg: " + Neo4jUtils.dumpGraphToSvg(graphDatabaseService));
//        }
    }

    /**
     * in case a known instance is modified and not explicitly saved, we track dirty state here and spool them for persisting
     */
    private void persistDirtyButUnsavedInstances() {
        Set pendingObjects = new HashSet();
        for ( Collection<PendingInsert> coll : getPendingInserts().values()) {
            for (PendingInsert pi: coll) {
                pendingObjects.add(pi.getEntityAccess().getEntity());
            }
        }
        for ( Collection<PendingUpdate> coll : getPendingUpdates().values()) {
            for (PendingUpdate pendingUpdate: coll) {
                pendingObjects.add(pendingUpdate.getEntityAccess().getEntity());
            }
        }

        for (Map<Serializable, Object> cache : firstLevelCache.values()) {
            for (Object obj: cache.values()) {
                if (obj instanceof DirtyCheckable) {
                    boolean isDirty = ((DirtyCheckable)obj).hasChanged();
                    if (isDirty && (!pendingObjects.contains(obj))) {
                        persist(obj);
                    }
                }
            }
        }
    }

    public boolean containsOrAddPersistentRelationship(long startNode, long endNode, String type) {
        Relationship r = new Relationship(startNode, endNode, type);
        boolean result = persistentRelationships.contains(r);
        persistentRelationships.add(r);
        return result;
    }

    public void addPersistentRelationships(Collection<Relationship> toAdd) {
        persistentRelationships.addAll(toAdd);
    }

    public void addPersistingInstance(Object obj) {
        persistingInstances.add(obj);
    }

    public boolean containsPersistingInstance(Object obj) {
        return persistingInstances.contains(obj);
    }

    public Relationship findPersistentRelationshipByType(String relType, Long id, boolean reversed) {
        return (Relationship) CollectionUtils.find(persistentRelationships, new RelationshipPredicate(relType, id, reversed));
    }

    public Iterable<Relationship> findPersistentRelationshipsByType(String relType, Long id, boolean reversed) {
        return IteratorUtil.asIterable(new FilterIterator(persistentRelationships.iterator(), new RelationshipPredicate(relType, id, reversed)));
    }

    public class RelationshipPredicate implements Predicate {

        private String type;
        private long id;
        private boolean reversed;

        public RelationshipPredicate(String type, long id, boolean reversed) {
            this.type = type;
            this.id = id;
            this.reversed = reversed;
        }

        @Override
        public boolean evaluate(Object object) {
            Relationship r = (Relationship) object;
            if (type.equals(r.getType())) {
                long startOrEndeId = reversed ? r.getEndNodeId() : r.getStartNodeId();
                if (startOrEndeId == id) {
                    return true;
                }
            }
            return false;
        }
    }

}


