/**
 * Copyright (C) 2018 Mike Hummel (mh@mhus.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mhus.app.reactive.util.engine;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.summerclouds.common.core.concurrent.Lock;
import org.summerclouds.common.core.error.TimeoutException;
import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.core.tool.MThread;
import org.summerclouds.common.db.sql.DataSourceProvider;
import org.summerclouds.common.db.sql.DbConnection;
import org.summerclouds.common.db.sql.DbResult;
import org.summerclouds.common.db.sql.DbStatement;

import de.mhus.app.reactive.model.engine.CaseLockProvider;

public class DatabaseLockProvider extends MLog implements CaseLockProvider {

    private DataSourceProvider ds;
    private String table;
    private String key;

    public DatabaseLockProvider(DataSourceProvider dsProvider, String tableName, String keyField) {
        this.ds = dsProvider;
        this.table = tableName;
        this.key = keyField;
    }

    @Override
    public boolean isCaseLocked(UUID caseId) {
        try (DbConnection con = ds.createConnection()) {
            DbStatement sth =
                    con.createStatement(
                            "SELECT "
                                    + key
                                    + " FROM "
                                    + table
                                    + " WHERE "
                                    + key
                                    + "=$key$ FOR UPDATE NOWAIT");
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("key", "case_" + caseId);
            sth.executeQuery(attributes);
            return false;
        } catch (Exception e) {
            log().d(e);
            return true;
        }
    }

    @Override
    public Lock lock(UUID caseId) throws TimeoutException {
        while (true) {
            Con con = tryLock("case_" + caseId);
            if (con != null) return new DbLock(con, caseId.toString());
            MThread.sleep(200);
        }
    }

    private Con tryLock(String value) {
        log().t("Try Lock", value);
        DbConnection con = null;
        try {
            con = ds.createConnection();
            DbStatement sth =
                    con.createStatement(
                            "SELECT "
                                    + key
                                    + " FROM "
                                    + table
                                    + " WHERE "
                                    + key
                                    + "=$key$ FOR UPDATE NOWAIT");
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("key", value);
            try {
                DbResult res = sth.executeQuery(attributes);
                if (res.next()) {
                    res.close();
                    log().t("=== Lock1", value);
                    return new Con(con, sth);
                }
                res.close();
            } catch (SQLException e) {
                if (!e.getMessage().contains("timeout")) throw e;
                sth.close();
                con.close();
                log().t("--- No0", value, e);
                return null;
            }
            DbStatement sthSet =
                    con.createStatement("INSERT INTO " + table + "(" + key + ") VALUES ($key$)");
            boolean done = false;
            try {
                sthSet.execute(attributes);
                done = true;
            } catch (SQLIntegrityConstraintViolationException e) {
            }
            if (!done) {
                con.close();
                log().t("--- No1", value);
                return null;
            }
            sthSet.close();
            con.commit();

            try {
                DbResult res = sth.executeQuery(attributes);
                if (res.next()) {
                    res.close();
                    log().t("=== Lock2", value);
                    return new Con(con, sth);
                }
                res.close();
            } catch (SQLException e) {
                if (!e.getMessage().contains("timeout")) throw e;
                sth.close();
                con.commit();
                con.close();
                log().t("--- No2", value, e);
                return null;
            }
            sth.close();
            con.commit();
            con.close();
            log().t("--- No3", value);
            return null;
        } catch (Exception e) {
            log().d(e);
            if (con != null) {
                try {
                    con.rollback();
                } catch (Exception e1) {
                    log().e(e1);
                }
                con.close();
            }
            log().t("--- No3", value);
            return null;
        }
    }

    @Override
    public Lock acquireCleanupMaster() {
        synchronized (this) {
            Con masterCleanup = tryLock("master_cleanup");
            if (masterCleanup == null) return null;
            return new DbLock(masterCleanup, "master_cleanup");
        }
    }

    @Override
    public Lock acquirePrepareMaster() {
        synchronized (this) {
            Con masterPrepare = tryLock("master_cleanup");
            if (masterPrepare == null) return null;
            return new DbLock(masterPrepare, "master_cleanup");
        }
    }

    @Override
    public Lock acquireEngineMaster() {
        synchronized (this) {
            while (true) {
                Con masterEngine = tryLock("master_cleanup");
                if (masterEngine != null) return new DbLock(masterEngine, "master_cleanup");
                MThread.sleep(300);
            }
        }
    }

    //    @Override
    //    public void releaseEngineMaster() {
    //        synchronized (this) {
    //            if (masterEngine != null)
    //                masterEngine.close();
    //            masterEngine = null;
    //        }
    //    }

    @Override
    public Lock lockOrNull(UUID caseId) {
        Con con = tryLock("case_" + caseId);
        if (con == null) return null;
        return new DbLock(con, caseId.toString());
    }

    @Override
    public boolean isReady() {
        return true; // ?
    }

    private class Con {
        DbConnection con;
        DbStatement sth;

        public Con(DbConnection con, DbStatement sth) {
            this.con = con;
            this.sth = sth;
        }

        public void close() {
            if (sth == null) return;
            sth.close();
            try {
                con.commit();
            } catch (Exception e) {
                log().e(e);
            }
            con.close();
            sth = null;
            con = null;
        }
    }

    private class DbLock implements Lock {

        private Con con;
        private String name;

        public DbLock(Con con, String name) {
            this.con = con;
            this.name = name;
        }

        @Override
        public Lock lock() {
            return this;
        }

        @Override
        public boolean lock(long timeout) {
            return con != null;
        }

        @Override
        public synchronized boolean unlock() {
            if (con == null) return true;
            con.close();
            return true;
        }

        @Override
        public void unlockHard() {
            unlock();
        }

        @Override
        public boolean isLocked() {
            return con != null;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOwner() {
            return "";
        }

        @Override
        public long getLockTime() {
            return 0;
        }

        @Override
        public boolean refresh() {
            return false;
        }

        @Override
        public long getCnt() {
            return 0;
        }

        @Override
        public String getStartStackTrace() {
            return null;
        }
    }
}
