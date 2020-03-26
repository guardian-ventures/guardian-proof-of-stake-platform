/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2019 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of this software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package nxt.db;

import nxt.Nxt;
import nxt.db.pool.ConnectionPool;
import nxt.db.pool.H2ConnectionPool;
import nxt.util.Logger;
import nxt.util.security.BlockchainPermission;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class BasicDb {

    public static final class DbProperties {

        private long maxCacheSize;
        private String dbUrl;
        private String dbType;
        private String dbDir;
        private String dbParams;
        private String dbUsername;
        private String dbPassword;
        private int maxConnections;
        private int loginTimeout;
        private int defaultLockTimeout;
        private int maxMemoryRows;

        public DbProperties maxCacheSize(int maxCacheSize) {
            this.maxCacheSize = maxCacheSize;
            return this;
        }

        public DbProperties dbUrl(String dbUrl) {
            this.dbUrl = dbUrl;
            return this;
        }

        public DbProperties dbType(String dbType) {
            this.dbType = dbType;
            return this;
        }

        public DbProperties dbDir(String dbDir) {
            this.dbDir = dbDir;
            return this;
        }

        public DbProperties dbParams(String dbParams) {
            this.dbParams = dbParams;
            return this;
        }

        public DbProperties dbUsername(String dbUsername) {
            this.dbUsername = dbUsername;
            return this;
        }

        public DbProperties dbPassword(String dbPassword) {
            this.dbPassword = dbPassword;
            return this;
        }

        public DbProperties maxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }

        public DbProperties loginTimeout(int loginTimeout) {
            this.loginTimeout = loginTimeout;
            return this;
        }

        public DbProperties defaultLockTimeout(int defaultLockTimeout) {
            this.defaultLockTimeout = defaultLockTimeout;
            return this;
        }

        public DbProperties maxMemoryRows(int maxMemoryRows) {
            this.maxMemoryRows = maxMemoryRows;
            return this;
        }

    }

    private ConnectionPool cp;
    private final String dbUrl;
    private final String dbUsername;
    private final String dbPassword;
    private final int maxConnections;
    private final int loginTimeout;
    private final int defaultLockTimeout;
    private final int maxMemoryRows;
    private volatile boolean initialized = false;

    public BasicDb(DbProperties dbProperties) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new BlockchainPermission("db"));
        }
        long maxCacheSize = dbProperties.maxCacheSize;
        if (maxCacheSize == 0) {
            maxCacheSize = Math.min(256, Math.max(16, (Runtime.getRuntime().maxMemory() / (1024 * 1024) - 128)/2)) * 1024;
        }
        String dbUrl = dbProperties.dbUrl;
        if (dbUrl == null) {
            String dbDir = Nxt.getDbDir(dbProperties.dbDir);
            dbUrl = String.format("jdbc:%s:%s;%s", dbProperties.dbType, dbDir, dbProperties.dbParams);
        }
        if (!dbUrl.contains("CACHE_SIZE=")) {
            dbUrl += ";CACHE_SIZE=" + maxCacheSize;
        }
        this.dbUrl = dbUrl;
        this.dbUsername = dbProperties.dbUsername;
        this.dbPassword = dbProperties.dbPassword;
        this.maxConnections = dbProperties.maxConnections;
        this.loginTimeout = dbProperties.loginTimeout;
        this.defaultLockTimeout = dbProperties.defaultLockTimeout;
        this.maxMemoryRows = dbProperties.maxMemoryRows;
    }

    public final void init(List<DbVersion> dbVersions) {
        Logger.logDebugMessage("Database jdbc url set to %s username %s", dbUrl, dbUsername);

        String implClassName = Nxt.getStringProperty("nxt.connectionPoolImpl");

        try {
            Class<?> poolImplClass = Class.forName(implClassName);
            cp = (ConnectionPool) poolImplClass.newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            Logger.logErrorMessage("Failed to create connection pool instance", e);
            cp = new H2ConnectionPool();
        }
        cp.initialize(dbUrl, dbUsername, dbPassword, maxConnections, loginTimeout);

        FullTextTrigger.setActive(true);

        try (Connection con = getPooledConnection();
             Statement stmt = con.createStatement()) {
            stmt.executeUpdate("SET DEFAULT_LOCK_TIMEOUT " + defaultLockTimeout);
            stmt.executeUpdate("SET MAX_MEMORY_ROWS " + maxMemoryRows);
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
        dbVersions.forEach(DbVersion::createSchema);
        dbVersions.forEach(DbVersion::init);
        initialized = true;
    }

    public final void shutdown() {
        if (!initialized) {
            return;
        }
        try {
            FullTextTrigger.setActive(false);
            Connection con = getPooledConnection();
            Statement stmt = con.createStatement();
            boolean compact = ! Nxt.getBooleanProperty("nxt.disableCompactOnShutdown");
            if (compact) {
                stmt.execute("SHUTDOWN COMPACT");
                Logger.logShutdownMessage("Database shutdown completed");
            } else {
                stmt.execute("SHUTDOWN");
                Logger.logShutdownMessage("Database shutdown without compact completed");
            }
        } catch (SQLException e) {
            Logger.logShutdownMessage(e.toString(), e);
        }
    }

    public final void analyzeTables() {
        try (Connection con = getPooledConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute("ANALYZE");
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public Connection getConnection(String schema) throws SQLException {
        Connection con = getPooledConnection();
        con.setAutoCommit(true);
        try (Statement stmt = con.createStatement()) {
            stmt.executeUpdate("SET SCHEMA " + schema);
            stmt.executeUpdate("SET SCHEMA_SEARCH_PATH " + schema + ", PUBLIC");
        }
        return con;
    }

    protected Connection getPooledConnection() throws SQLException {
        return cp.getConnection();
    }

    public final String getUrl() {
        return dbUrl;
    }

}
