package org.grails.datastore.gorm.neo4j;

import org.neo4j.cypher.export.DatabaseSubGraph;
import org.neo4j.cypher.export.SubGraphExporter;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * dump full graph to svg after a session is flushed
 * this is used from Setup.groovy to get debugging information while running tests
 */
public class DumpGraphOnSessionFlushListener implements ApplicationListener<SessionFlushedEvent> {

    private static Logger log = LoggerFactory.getLogger(DumpGraphOnSessionFlushListener.class);

    private final GraphDatabaseService graphDatabaseService;

    public DumpGraphOnSessionFlushListener(GraphDatabaseService graphDatabaseService) {
        this.graphDatabaseService = graphDatabaseService;
    }

    @Override
    public void onApplicationEvent(SessionFlushedEvent event) {
        dump();
    }

    public void dump() {
        Transaction tx = graphDatabaseService.beginTx(); // TODO: refactor to try-with-resources
        try {    // TODO: disabled due to tx issue: getallnodes sees deleted stuff in weird cases
            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            new SubGraphExporter(new DatabaseSubGraph(graphDatabaseService)).export(printWriter);
            log.info(writer.toString());
            log.info("svg: " + Neo4jUtils.dumpGraphToSvg(graphDatabaseService));
            tx.success();
        } finally {
            tx.close();
        }
    }
}
