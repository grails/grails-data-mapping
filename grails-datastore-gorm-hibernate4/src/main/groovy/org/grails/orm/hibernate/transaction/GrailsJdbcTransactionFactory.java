package org.grails.orm.hibernate.transaction;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.grails.orm.hibernate.transaction.GrailsJdbcTransactionFactory.GrailsJdbcTransaction;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcConnectionAccess;
import org.hibernate.engine.transaction.internal.jdbc.JdbcIsolationDelegate;
import org.hibernate.engine.transaction.internal.jdbc.JdbcTransaction;
import org.hibernate.engine.transaction.spi.IsolationDelegate;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.engine.transaction.spi.TransactionFactory;
import org.hibernate.service.spi.Wrapped;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.util.ReflectionUtils;

/**
 * 
 * Hibernate4 TransactionFactory implementation for unwrapping TransactionAwareDataSourceProxy
 * when {@link org.hibernate.engine.transaction.spi.IsolationDelegate} is used.
 * 
 * IsolationDelegate need a new connection from the datasource.
 * 
 * This is required when there is no real JTA transaction manager in use and Spring's
 * {@link TransactionAwareDataSourceProxy} is used.
 * 
 * Without this solution, using Hibernate's TableGenerator identity strategies will fail to support transactions.
 * The id generator will commit the current transaction and break transactional behaviour.
 * 
 * @author Lari Hotari
 */
public class GrailsJdbcTransactionFactory  implements TransactionFactory<GrailsJdbcTransaction> {
    private static final long serialVersionUID = 1L;

    @Override
    public GrailsJdbcTransaction createTransaction(TransactionCoordinator transactionCoordinator) {
        return new GrailsJdbcTransaction( transactionCoordinator );
    }

    @Override
    public boolean canBeDriver() {
        return true;
    }

    @Override
    public ConnectionReleaseMode getDefaultReleaseMode() {
        return ConnectionReleaseMode.ON_CLOSE;
    }

    @Override
    public boolean compatibleWithJtaSynchronization() {
        return false;
    }

    @Override
    public boolean isJoinableJtaTransaction(TransactionCoordinator transactionCoordinator, GrailsJdbcTransaction transaction) {
        return false;
    }
    
    public static class GrailsJdbcTransaction extends JdbcTransaction {
        protected GrailsJdbcTransaction(TransactionCoordinator transactionCoordinator) {
            super(transactionCoordinator);
        }

        @Override
        public IsolationDelegate createIsolationDelegate() {
            return new GrailsJdbcIsolationDelegate(transactionCoordinator());
        }
    }
    
    public static class GrailsJdbcIsolationDelegate extends JdbcIsolationDelegate {
        public GrailsJdbcIsolationDelegate(TransactionCoordinator transactionCoordinator) {
            super(transactionCoordinator);
        }

        @Override
        protected JdbcConnectionAccess jdbcConnectionAccess() {
            JdbcConnectionAccess connectionAccess = super.jdbcConnectionAccess();
            Field connectionProviderField=ReflectionUtils.findField(connectionAccess.getClass(), "connectionProvider");
            if (connectionProviderField != null) {
                ReflectionUtils.makeAccessible(connectionProviderField);
                Object connectionProvider = ReflectionUtils.getField(connectionProviderField, connectionAccess);
                if (connectionProvider instanceof Wrapped) {
                    DataSource dataSource = ((Wrapped)connectionProvider).unwrap(DataSource.class);
                    if(dataSource instanceof TransactionAwareDataSourceProxy) {
                        DataSource unwrapped = ((TransactionAwareDataSourceProxy)dataSource).getTargetDataSource();
                        DatasourceConnectionProviderImpl dsProvider = new DatasourceConnectionProviderImpl();
                        Map<String, Object> configValues=new HashMap<String, Object>();
                        configValues.put(Environment.DATASOURCE, unwrapped);
                        dsProvider.configure(configValues);
                        return new ConnectionProviderJdbcConnectionAccess(dsProvider);                        
                    }
                }
            }
            return connectionAccess;
        }
    }
    
    private static class ConnectionProviderJdbcConnectionAccess implements JdbcConnectionAccess {
        private static final long serialVersionUID = 1L;
        private final ConnectionProvider connectionProvider;

        public ConnectionProviderJdbcConnectionAccess(ConnectionProvider connectionProvider) {
            this.connectionProvider = connectionProvider;
        }

        @Override
        public Connection obtainConnection() throws SQLException {
            return connectionProvider.getConnection();
        }

        @Override
        public void releaseConnection(Connection connection) throws SQLException {
            connectionProvider.closeConnection( connection );
        }

        @Override
        public boolean supportsAggressiveRelease() {
            return connectionProvider.supportsAggressiveRelease();
        }
    }    
}
