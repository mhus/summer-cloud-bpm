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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.summerclouds.common.core.M;
import org.summerclouds.common.core.activator.Activator;
import org.summerclouds.common.core.cfg.CfgInt;
import org.summerclouds.common.core.error.MRuntimeException;
import org.summerclouds.common.core.error.NotFoundException;
import org.summerclouds.common.core.error.RC;
import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.core.node.INode;
import org.summerclouds.common.core.node.INodeFactory;
import org.summerclouds.common.core.node.MProperties;
import org.summerclouds.common.core.tool.MPeriod;
import org.summerclouds.common.core.tool.MSpring;
import org.summerclouds.common.core.tool.MString;
import org.summerclouds.common.core.tool.MSystem;
import org.summerclouds.common.core.tool.MThread;
import org.summerclouds.common.core.util.MObjectInputStream;
import org.summerclouds.common.core.util.MUri;
import org.summerclouds.common.db.sql.DbConnection;
import org.summerclouds.common.db.sql.DbPool;
import org.summerclouds.common.db.sql.DbResult;
import org.summerclouds.common.db.sql.DbStatement;

import de.mhus.app.reactive.model.engine.EngineConst;
import de.mhus.app.reactive.model.engine.PCase;
import de.mhus.app.reactive.model.engine.PCase.STATE_CASE;
import de.mhus.app.reactive.model.engine.PCaseInfo;
import de.mhus.app.reactive.model.engine.PNode;
import de.mhus.app.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.app.reactive.model.engine.PNode.TYPE_NODE;
import de.mhus.app.reactive.model.engine.PNodeInfo;
import de.mhus.app.reactive.model.engine.Result;
import de.mhus.app.reactive.model.engine.SearchCriterias;
import de.mhus.app.reactive.model.engine.StorageProvider;

public class SqlDbStorage extends MLog implements StorageProvider {

    private static final CfgInt CFG_INIT_RETRY_SEC =
            new CfgInt(SqlDbStorage.class, "initRetrySec", 30);

    private static final int MAX_INDEX_VALUES = Math.min(10, EngineConst.MAX_INDEX_VALUES);
    private static final String INDEX_COLUMNS =
            ",index0_,index1_,index2_,index3_,index4_,index5_,index6_,index7_,index8_,index9_";
    private static final String CASE_COLUMNS =
            "id_,uri_,name_,state_,custom_,customer_,process_,version_,pool_,created_,modified_,priority_,score_,milestone_"
                    + INDEX_COLUMNS;
    private static final String NODE_COLUMNS =
            "id_,case_,name_,assigned_,state_,type_,uri_,custom_,customer_,process_,version_,pool_,created_,modified_,priority_,score_,actor_,due_"
                    + INDEX_COLUMNS;
    private DbPool pool;
    private String prefix;

    private Activator activator;

    public SqlDbStorage(DbPool pool, String prefix) {
        this.pool = pool;
        this.prefix = prefix;
        init();
    }

    public void init() {
        while (true) {
            DbConnection con = null;
            try {
                URL url = MSystem.locateResource(this, "SqlDbStorage.xml");
                con = pool.getConnection();
                INode data = M.l(INodeFactory.class).read(url);
                data.setString("prefix", prefix);
                pool.getDialect().createStructure(data, con, null, false);
                return;
            } catch (Exception e) {
                log().e(e);
            } finally {
                try {
                    if (con != null) con.close();
                } catch (Throwable t) {
                    log().d(t);
                }
            }
            log().i("init failed", this, "Retry init of DB in " + CFG_INIT_RETRY_SEC.value() + " sec");
            MThread.sleep(CFG_INIT_RETRY_SEC.value() * 1000);
        }
    }

    @Override
    public void updateFullCase(PCase caze) throws IOException {
        DbConnection con = null;
        try {

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            caze.writeExternal(new ObjectOutputStream(outStream));
            ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());

            con = pool.getConnection();
            MProperties prop = new MProperties();
            prop.put("id", caze.getId());
            prop.put("content", inStream);
            prop.put("modified", new Date());
            prop.put("state", caze.getState());
            prop.put("closedCode", caze.getClosedCode());
            prop.put(
                    "closedMessage",
                    M.trunc(caze.getClosedMessage() == null ? "" : caze.getClosedMessage(), 400));
            prop.put("milestone", M.trunc(caze.getMilestone(), 200));

            prop.put("created", new Date(caze.getCreationDate()));
            prop.put("custom", M.trunc(caze.getCustomId(), 700));
            prop.put("customer", M.trunc(caze.getCustomerId(), 700));
            prop.put("name", caze.getCanonicalName());
            prop.put("uri", M.trunc(caze.getUri(), 700));
            MUri u = MUri.toUri(caze.getUri());
            prop.put("process", MString.beforeIndex(u.getLocation(), ':'));
            prop.put("version", MString.afterIndex(u.getLocation(), ':'));
            prop.put("pool", u.getPath());

            String sql =
                    "UPDATE "
                            + prefix
                            + "_case_ SET "
                            + "content_=$content$,"
                            + "modified_=$modified$,"
                            + "state_=$state$,"
                            + "milestone_=$milestone$,"
                            + "created_=$created$,"
                            + "custom_=$custom$,"
                            + "customer_=$customer$,"
                            + "name_=$name$,"
                            + "uri_=$uri$,"
                            + "process_=$crocess$,"
                            + "version_=$version$,"
                            + "pool_=$pool$,"
                            + "closed_code_=$closedCode$,"
                            + "closed_message_=$closedMessage$";

            if (caze.getIndexValues() != null) {
                String[] idx = caze.getIndexValues();
                for (int i = 0; i < MAX_INDEX_VALUES; i++)
                    if (idx.length > i && idx[i] != null) {
                        prop.put("index" + i, M.trunc(idx[i], 300));
                        sql = sql + ",index" + i + "_=$index" + i + "$";
                    }
            }

            sql = sql + " WHERE id_=$id$";

            DbStatement sta = con.createStatement(sql);
            sta.executeUpdate(prop);
            sta.close();

            con.commit();
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d("close connection failed", caze, t);
            }
        }
    }

    @Override
    public void saveCase(PCase caze) throws IOException {
        DbConnection con = null;
        try {
            con = pool.getConnection();
            boolean exists = false;
            MProperties prop = new MProperties();
            prop.put("id", caze.getId());
            {
                DbStatement sta =
                        con.createStatement("SELECT id_ FROM " + prefix + "_case_ WHERE id_=$id$");
                DbResult res = sta.executeQuery(prop);
                exists = res.next();
                res.close();
                sta.close();
            }
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            caze.writeExternal(new ObjectOutputStream(outStream));
            ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
            prop.put("content", inStream);
            prop.put("modified", new Date());
            prop.put("state", caze.getState());
            prop.put("closedCode", caze.getClosedCode());
            prop.put(
                    "closedMessage",
                    M.trunc(caze.getClosedMessage() == null ? "" : caze.getClosedMessage(), 400));
            prop.put("milestone", M.trunc(caze.getMilestone(), 200));

            if (!exists) {
                prop.put("created", new Date());
                prop.put("custom", M.trunc(caze.getCustomId(), 700));
                prop.put("customer", M.trunc(caze.getCustomerId(), 700));
                prop.put("name", caze.getCanonicalName());
                prop.put("uri", M.trunc(caze.getUri(), 700));
                MUri u = MUri.toUri(caze.getUri());
                prop.put("process", MString.beforeIndex(u.getLocation(), ':'));
                prop.put("version", MString.afterIndex(u.getLocation(), ':'));
                prop.put("pool", u.getPath());
            }

            if (exists) {
                String sql =
                        "UPDATE "
                                + prefix
                                + "_case_ SET "
                                + "content_=$content$,"
                                + "modified_=$modified$,"
                                + "state_=$state$,"
                                + "milestone_=$milestone$,"
                                + "closed_code_=$closedCode$,"
                                + "closed_message_=$closedMessage$";

                if (caze.getIndexValues() != null) {
                    String[] idx = caze.getIndexValues();
                    for (int i = 0; i < MAX_INDEX_VALUES; i++)
                        if (idx.length > i && idx[i] != null) {
                            prop.put("index" + i, M.trunc(idx[i], 300));
                            sql = sql + ",index" + i + "_=$index" + i + "$";
                        }
                }

                sql = sql + " WHERE id_=$id$";

                DbStatement sta = con.createStatement(sql);
                sta.executeUpdate(prop);
                sta.close();
            } else {

                if (caze.getIndexValues() != null) {
                    String[] idx = caze.getIndexValues();
                    for (int i = 0; i < MAX_INDEX_VALUES; i++)
                        if (idx.length > i) prop.put("index" + i, M.trunc(idx[i], 300));
                }

                DbStatement sta =
                        con.createStatement(
                                "INSERT INTO "
                                        + prefix
                                        + "_case_ ("
                                        + "id_,"
                                        + "content_,"
                                        + "created_,"
                                        + "modified_,"
                                        + "state_,"
                                        + "milestone_,"
                                        + "uri_,"
                                        + "closed_code_,"
                                        + "closed_message_,"
                                        + "name_,"
                                        + "custom_,"
                                        + "customer_,"
                                        + "process_,"
                                        + "version_,"
                                        + "pool_,"
                                        + "priority_,"
                                        + "score_,"
                                        + "index0_,"
                                        + "index1_,"
                                        + "index2_,"
                                        + "index3_,"
                                        + "index4_,"
                                        + "index5_,"
                                        + "index6_,"
                                        + "index7_,"
                                        + "index8_,"
                                        + "index9_"
                                        + ") VALUES ("
                                        + "$id$,"
                                        + "$content$,"
                                        + "$created$,"
                                        + "$modified$,"
                                        + "$state$,"
                                        + "$milestone$,"
                                        + "$uri$,"
                                        + "$closedCode$,"
                                        + "$closedMessage$,"
                                        + "$name$,"
                                        + "$custom$,"
                                        + "$customer$,"
                                        + "$process$,"
                                        + "$version$,"
                                        + "$pool$,"
                                        + "100,"
                                        + "0,"
                                        + "$index0$,"
                                        + "$index1$,"
                                        + "$index2$,"
                                        + "$index3$,"
                                        + "$index4$,"
                                        + "$index5$,"
                                        + "$index6$,"
                                        + "$index7$,"
                                        + "$index8$,"
                                        + "$index9$"
                                        + ")");
                sta.executeUpdate(prop);
                sta.close();
            }
            con.commit();
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d("close connection failed", caze, t);
            }
        }
    }

    @Override
    public PCase loadCase(UUID id) throws IOException, NotFoundException {
        PCase caze = null;
        DbConnection con = null;
        try {
            con = pool.getConnection();
            MProperties prop = new MProperties();
            prop.put("id", id);
            DbStatement sta =
                    con.createStatement("SELECT content_ FROM " + prefix + "_case_ WHERE id_=$id$");
            DbResult res = sta.executeQuery(prop);
            if (res.next()) {
                InputStream in = res.getBinaryStream("content_");
                caze = new PCase();
                Activator act = getActivator();
                MObjectInputStream ois = new MObjectInputStream(in, act);
                caze.readExternal(ois);
            }
            res.close();
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d("close connection failed", id, t);
            }
        }
        if (caze == null) throw new NotFoundException("case", id);
        return caze;
    }

    private synchronized Activator getActivator() {
        if (activator == null) {
            activator = MSpring.getDefaultActivator();
        }
        return activator;
    }

    @Override
    public void deleteCaseAndFlowNodes(UUID id) throws IOException {
        DbConnection con = null;
        try {
            con = pool.getConnection();
            MProperties prop = new MProperties();
            prop.put("id", id);
            {
                DbStatement sta =
                        con.createStatement("DELETE FROM " + prefix + "_case_ WHERE id_=$id$");
                sta.execute(prop);
            }
            {
                DbStatement sta =
                        con.createStatement("DELETE FROM " + prefix + "_node_ WHERE case_=$id$");
                sta.execute(prop);
            }
            con.commit();
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d("close connection failed", id, t);
            }
        }
    }

    @Override
    public void deleteFlowNode(UUID id) throws IOException {
        DbConnection con = null;
        try {
            con = pool.getConnection();
            MProperties prop = new MProperties();
            prop.put("id", id);
            {
                DbStatement sta =
                        con.createStatement("DELETE FROM " + prefix + "_node_ WHERE id_=$id$");
                sta.execute(prop);
            }
            con.commit();
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d("close connection failed", id, t);
            }
        }
    }

    @Override
    public void updateFullFlowNode(PNode flow) throws IOException {

        DbConnection con = null;
        try {
            con = pool.getConnection();

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            flow.writeExternal(new ObjectOutputStream(outStream));
            ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());

            MProperties prop = new MProperties();
            prop.put("id", flow.getId());
            prop.put("content", inStream);
            prop.put("modified", new Date());
            prop.put("state", flow.getState());
            prop.put("type", flow.getType());
            prop.put("signal", M.trunc(flow.getSignalsAsString(), 700));
            prop.put("message", M.trunc(flow.getMessagesAsString(), 700));
            prop.put("actor", M.trunc(flow.getActor(), 100));
            prop.put("due", new Date(flow.getDue()));

            PCaseInfo caze = loadCaseInfo(flow.getCaseId());
            if (caze == null)
                throw new IOException(
                        "Case " + flow.getCaseId() + " not found to update node " + flow.getId());
            prop.put("name", M.trunc(flow.getCanonicalName(), 700));
            prop.put("case", flow.getCaseId());
            prop.put("created", new Date(flow.getCreationDate()));
            prop.put("custom", M.trunc(caze.getCustomId(), 700));
            prop.put("customer", M.trunc(caze.getCustomerId(), 700));
            prop.put("uri", M.trunc(caze.getUri(), 700));
            MUri u = MUri.toUri(caze.getUri());
            prop.put("process", MString.beforeIndex(u.getLocation(), ':'));
            prop.put("version", MString.afterIndex(u.getLocation(), ':'));
            prop.put("pool", u.getPath());

            if (flow.getAssignedUser() != null) prop.put("assigned", flow.getAssignedUser());
            Entry<String, Long> scheduled = flow.getNextScheduled();
            long scheduledLong = 0;
            if (scheduled != null && scheduled.getValue() != null)
                scheduledLong = scheduled.getValue();
            prop.put("scheduled", scheduledLong);

            String sql =
                    "UPDATE "
                            + prefix
                            + "_node_ SET "
                            + "content_=$content$,"
                            + "modified_=$modified$,"
                            + "state_=$state$,"
                            + "type_=$type$,"
                            + "signal_=$signal$,"
                            + "message_=$message$,"
                            + "scheduled_=$scheduled$,"
                            + "assigned_=$assigned$,"
                            + "name_=$name$,"
                            + "case_=$case$,"
                            + "created_=$created$,"
                            + "custom_=$custom$,"
                            + "customer_=$customer$,"
                            + "uri_=$uri$,"
                            + "process_=$process$,"
                            + "version_=$version$,"
                            + "pool_=$pool$,"
                            + "actor_ = $actor$,"
                            + "due_ = $due$";

            if (flow.getIndexValues() != null) {
                String[] idx = flow.getIndexValues();
                for (int i = 0; i < MAX_INDEX_VALUES; i++)
                    if (idx.length > i && idx[i] != null) {
                        prop.put("index" + i, M.trunc(idx[i], 300));
                        sql = sql + ",index" + i + "_=$index" + i + "$";
                    }
            }

            sql = sql + " WHERE id_=$id$";

            DbStatement sta = con.createStatement(sql);
            sta.executeUpdate(prop);
            sta.close();

            con.commit();
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d("close connection failed", flow, t);
            }
        }
    }

    @Override
    public void saveFlowNode(PNode flow) throws IOException {

        DbConnection con = null;
        try {
            con = pool.getConnection();
            boolean exists = false;
            MProperties prop = new MProperties();
            prop.put("id", flow.getId());
            {
                DbStatement sta =
                        con.createStatement("SELECT id_ FROM " + prefix + "_node_ WHERE id_=$id$");
                DbResult res = sta.executeQuery(prop);
                exists = res.next();
                res.close();
                sta.close();
            }
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            flow.writeExternal(new ObjectOutputStream(outStream));
            ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
            prop.put("content", inStream);
            prop.put("modified", new Date());
            prop.put("state", flow.getState());
            prop.put("type", flow.getType());
            prop.put("signal", M.trunc(flow.getSignalsAsString(), 700));
            prop.put("message", M.trunc(flow.getMessagesAsString(), 700));
            prop.put("actor", M.trunc(flow.getActor(), 100));
            prop.put("due", new Date(flow.getDue()));

            if (!exists) {
                PCaseInfo caze = loadCaseInfo(flow.getCaseId());
                if (caze == null)
                    throw new IOException(
                            "Case "
                                    + flow.getCaseId()
                                    + " not found to create node "
                                    + flow.getId());
                prop.put("name", M.trunc(flow.getCanonicalName(), 700));
                prop.put("case", flow.getCaseId());
                prop.put("created", new Date());
                prop.put("custom", M.trunc(caze.getCustomId(), 700));
                prop.put("customer", M.trunc(caze.getCustomerId(), 700));
                prop.put("uri", M.trunc(caze.getUri(), 700));
                MUri u = MUri.toUri(caze.getUri());
                prop.put("process", MString.beforeIndex(u.getLocation(), ':'));
                prop.put("version", MString.afterIndex(u.getLocation(), ':'));
                prop.put("pool", u.getPath());
            }

            if (flow.getAssignedUser() != null) prop.put("assigned", flow.getAssignedUser());
            Entry<String, Long> scheduled = flow.getNextScheduled();
            long scheduledLong = 0;
            if (scheduled != null && scheduled.getValue() != null)
                scheduledLong = scheduled.getValue();
            prop.put("scheduled", scheduledLong);

            if (exists) {
                String sql =
                        "UPDATE "
                                + prefix
                                + "_node_ SET "
                                + "content_=$content$,"
                                + "modified_=$modified$,"
                                + "state_=$state$,"
                                + "type_=$type$,"
                                + "signal_=$signal$,"
                                + "message_=$message$,"
                                + "scheduled_=$scheduled$,"
                                + "assigned_=$assigned$,"
                                + "actor_ = $actor$,"
                                + "due_ = $due$";

                if (flow.getIndexValues() != null) {
                    String[] idx = flow.getIndexValues();
                    for (int i = 0; i < MAX_INDEX_VALUES; i++)
                        if (idx.length > i && idx[i] != null) {
                            prop.put("index" + i, M.trunc(idx[i], 300));
                            sql = sql + ",index" + i + "_=$index" + i + "$";
                        }
                }

                sql = sql + " WHERE id_=$id$";

                DbStatement sta = con.createStatement(sql);
                sta.executeUpdate(prop);
                sta.close();
            } else {

                if (flow.getIndexValues() != null) {
                    String[] idx = flow.getIndexValues();
                    for (int i = 0; i < MAX_INDEX_VALUES; i++)
                        if (idx.length > i) prop.put("index" + i, M.trunc(idx[i], 300));
                }

                DbStatement sta =
                        con.createStatement(
                                "INSERT INTO "
                                        + prefix
                                        + "_node_ ("
                                        + "id_,"
                                        + "case_,"
                                        + "content_,"
                                        + "created_,"
                                        + "modified_,"
                                        + "state_,"
                                        + "type_,"
                                        + "name_,"
                                        + "scheduled_,"
                                        + "assigned_,"
                                        + "signal_,"
                                        + "message_,"
                                        + "uri_,"
                                        + "custom_,"
                                        + "customer_,"
                                        + "process_,"
                                        + "version_,"
                                        + "pool_,"
                                        + "priority_,"
                                        + "score_,"
                                        + "index0_,"
                                        + "index1_,"
                                        + "index2_,"
                                        + "index3_,"
                                        + "index4_,"
                                        + "index5_,"
                                        + "index6_,"
                                        + "index7_,"
                                        + "index8_,"
                                        + "index9_,"
                                        + "actor_,"
                                        + "due_"
                                        + ") VALUES ("
                                        + "$id$,"
                                        + "$case$,"
                                        + "$content$,"
                                        + "$created$,"
                                        + "$modified$,"
                                        + "$state$,"
                                        + "$type$,"
                                        + "$name$,"
                                        + "$scheduled$,"
                                        + "$assigned$,"
                                        + "$signal$,"
                                        + "$message$,"
                                        + "$uri$,"
                                        + "$custom$,"
                                        + "$customer$,"
                                        + "$process$,"
                                        + "$version$,"
                                        + "$pool$,"
                                        + "100,"
                                        + "0,"
                                        + "$index0$,"
                                        + "$index1$,"
                                        + "$index2$,"
                                        + "$index3$,"
                                        + "$index4$,"
                                        + "$index5$,"
                                        + "$index6$,"
                                        + "$index7$,"
                                        + "$index8$,"
                                        + "$index9$,"
                                        + "$actor$,"
                                        + "$due$"
                                        + ")");
                sta.executeUpdate(prop);
                sta.close();
            }
            con.commit();
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d("close connection failed", flow, t);
            }
        }
    }

    @Override
    public PNode loadFlowNode(UUID id) throws IOException, NotFoundException {
        PNode node = null;
        DbConnection con = null;
        try {
            con = pool.getConnection();
            MProperties prop = new MProperties();
            prop.put("id", id);
            DbStatement sta =
                    con.createStatement("SELECT content_ FROM " + prefix + "_node_ WHERE id_=$id$");
            DbResult res = sta.executeQuery(prop);
            if (res.next()) {
                InputStream in = res.getBinaryStream("content_");
                node = new PNode();
                try {
                    Activator act = getActivator();
                    MObjectInputStream ois = new MObjectInputStream(in, act);
                    node.readExternal(ois);
                } catch (java.io.EOFException eofe) {
                    log().w("read strem for node {1} failed", node, eofe); // most because of extended parameters
                }
            }
            res.close();
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d("close connection failed", id, t);
            }
        }
        if (node == null) throw new NotFoundException("node", id);
        return node;
    }

    @Override
    public Result<PCaseInfo> getCases(STATE_CASE state) throws IOException {
        DbConnection con = null;
        try {
            con = pool.getConnection();
            MProperties prop = new MProperties();
            DbStatement sta = null;
            if (state == null) {
                sta = con.createStatement("SELECT " + CASE_COLUMNS + " FROM " + prefix + "_case_");
            } else {
                prop.put("state", state);
                sta =
                        con.createStatement(
                                "SELECT "
                                        + CASE_COLUMNS
                                        + " FROM "
                                        + prefix
                                        + "_case_ WHERE state_=$state$");
            }
            DbResult res = sta.executeQuery(prop);
            return new SqlResultCase(con, res);
        } catch (Exception e) {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d(t);
            }
            throw new IOException(e);
        }
    }

    @Override
    public Result<PNodeInfo> getFlowNodes(UUID caseId, STATE_NODE state) throws IOException {
        DbConnection con = null;
        try {
            con = pool.getConnection();
            MProperties prop = new MProperties();
            DbStatement sta = null;
            if (caseId == null && state == null) {
                sta = con.createStatement("SELECT " + NODE_COLUMNS + " FROM " + prefix + "_node_");
            } else if (caseId == null) {
                prop.put("state", state);
                sta =
                        con.createStatement(
                                "SELECT "
                                        + NODE_COLUMNS
                                        + " FROM "
                                        + prefix
                                        + "_node_ WHERE state_=$state$");
            } else if (state == null) {
                prop.setString("case", caseId.toString());
                sta =
                        con.createStatement(
                                "SELECT "
                                        + NODE_COLUMNS
                                        + " FROM "
                                        + prefix
                                        + "_node_ WHERE case_=$case$");
            } else {
                prop.setString("case", caseId.toString());
                prop.put("state", state);
                sta =
                        con.createStatement(
                                "SELECT "
                                        + NODE_COLUMNS
                                        + " FROM "
                                        + prefix
                                        + "_node_ WHERE state_=$state$ and case_=$case$");
            }
            DbResult res = sta.executeQuery(prop);
            return new SqlResultNode(con, res);
        } catch (Exception e) {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d(t);
            }
            throw new IOException(e);
        }
    }

    @Override
    public Result<PNodeInfo> getScheduledFlowNodes(STATE_NODE state, long scheduled, boolean order)
            throws IOException {
        DbConnection con = null;
        try {
            con = pool.getConnection();
            MProperties prop = new MProperties();
            DbStatement sta = null;
            if (state == null) {
                prop.setLong("scheduled", scheduled);
                sta =
                        con.createStatement(
                                "SELECT "
                                        + NODE_COLUMNS
                                        + " FROM "
                                        + prefix
                                        + "_node_ WHERE scheduled_ <= $scheduled$"
                                        + (order ? " ORDER BY priority_ ASC, modified_ DESC" : ""));
            } else {
                prop.setLong("scheduled", scheduled);
                prop.put("state", state);
                sta =
                        con.createStatement(
                                "SELECT "
                                        + NODE_COLUMNS
                                        + " FROM "
                                        + prefix
                                        + "_node_ WHERE state_=$state$ and scheduled_ <= $scheduled$"
                                        + (order ? " ORDER BY priority_ ASC, modified_ DESC" : ""));
            }
            DbResult res = sta.executeQuery(prop);
            return new SqlResultNode(con, res);
        } catch (Exception e) {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d(t);
            }
            throw new IOException(e);
        }
    }

    @Override
    public Result<PNodeInfo> getSignalFlowNodes(STATE_NODE state, String signal)
            throws IOException {
        DbConnection con = null;
        try {
            con = pool.getConnection();
            MProperties prop = new MProperties();
            DbStatement sta = null;
            if (state == null) {
                prop.setString("signal", "%" + PNode.getSignalAsString(signal) + "%");
                sta =
                        con.createStatement(
                                "SELECT "
                                        + NODE_COLUMNS
                                        + " FROM "
                                        + prefix
                                        + "_node_ WHERE signal_ like $signal$");
            } else {
                prop.setString("signal", "%" + PNode.getSignalAsString(signal) + "%");
                prop.put("state", state);
                sta =
                        con.createStatement(
                                "SELECT "
                                        + NODE_COLUMNS
                                        + " FROM "
                                        + prefix
                                        + "_node_ WHERE state_=$state$ and signal_ like $signal$");
            }
            DbResult res = sta.executeQuery(prop);
            return new SqlResultNode(con, res);
        } catch (Exception e) {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d(t);
            }
            throw new IOException(e);
        }
    }

    @Override
    public Result<PNodeInfo> getMessageFlowNodes(UUID caseId, STATE_NODE state, String message)
            throws IOException {
        DbConnection con = null;
        try {
            con = pool.getConnection();
            MProperties prop = new MProperties();
            DbStatement sta = null;
            if (state == null && caseId == null) {
                prop.setString("message", "%" + PNode.getMessageAsString(message) + "%");
                sta =
                        con.createStatement(
                                "SELECT "
                                        + NODE_COLUMNS
                                        + " FROM "
                                        + prefix
                                        + "_node_ WHERE message_ like $message$");
            } else if (caseId == null) {
                prop.setString("message", "%" + PNode.getMessageAsString(message) + "%");
                prop.put("state", state);
                sta =
                        con.createStatement(
                                "SELECT "
                                        + NODE_COLUMNS
                                        + " FROM "
                                        + prefix
                                        + "_node_ WHERE state_=$state$ and message_ like $message$");
            } else if (state == null) {
                prop.setString("message", "%" + PNode.getMessageAsString(message) + "%");
                prop.put("case", caseId);
                sta =
                        con.createStatement(
                                "SELECT "
                                        + NODE_COLUMNS
                                        + " FROM "
                                        + prefix
                                        + "_node_ WHERE case_=$case$ and message_ like $message$");
            } else {
                prop.setString("message", "%" + PNode.getMessageAsString(message) + "%");
                prop.put("case", caseId);
                prop.put("state", state);
                sta =
                        con.createStatement(
                                "SELECT "
                                        + NODE_COLUMNS
                                        + " FROM "
                                        + prefix
                                        + "_node_ WHERE case_=$case$ and state_=$state$ and message_ like $message$");
            }

            DbResult res = sta.executeQuery(prop);
            return new SqlResultNode(con, res);
        } catch (Exception e) {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d(t);
            }
            throw new IOException(e);
        }
    }

    @Override
    public Result<PNodeInfo> searchFlowNodes(SearchCriterias search) throws IOException {
        DbConnection con = null;
        try {
            StringBuilder sql =
                    new StringBuilder("SELECT " + NODE_COLUMNS + " FROM " + prefix + "_node_ ");
            boolean whereAdded = false;
            MProperties prop = new MProperties();
            if (search.unassigned) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                sql.append("assigned_ is null ");
            } else if (search.assigned != null) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                prop.setString("user", search.assigned);
                sql.append("assigned_=$user$ ");
            }

            if (search.nodeState != null) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                prop.put("state", search.nodeState);
                sql.append("state_=$state$ ");
            }

            if (search.type != null) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                prop.put("type", search.type);
                sql.append("type_=$type$ ");
            }

            if (search.uri != null) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                addFilter("uri", search.uri, prop, sql);
            }

            if (search.name != null) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                addFilter("name", search.name, prop, sql);
            }

            if (search.custom != null) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                addFilter("custom", search.custom, prop, sql);
            }

            if (search.customer != null) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                addFilter("customer", search.customer, prop, sql);
            }

            if (search.process != null) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                addFilter("process", search.process, prop, sql);
            }

            if (search.version != null) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                addFilter("version", search.version, prop, sql);
            }

            if (search.priority != Integer.MAX_VALUE) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                prop.put("priority", search.priority);
                sql.append("priority_=$priority$ ");
            }

            if (search.score != Integer.MIN_VALUE) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                prop.put("score", search.score);
                sql.append("score_ >= $score$ ");
            }

            if (search.pool != null) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                addFilter("pool", search.pool, prop, sql);
            }

            if (search.caseId != null) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                prop.put("case", search.caseId);
                sql.append("case_=$case$ ");
            }

            if (search.index != null) {
                boolean first = true;
                for (int i = 0; i < MAX_INDEX_VALUES; i++) {
                    if (search.index.length > i && search.index[i] != null) {
                        if (first) {
                            addJoin(whereAdded, search, sql);
                            sql.append(" (");
                            whereAdded = true;
                        } else {
                            sql.append("OR ");
                        }
                        // prop.setString("index" + i, search.index[i]);
                        addFilter("index" + i, search.index[i], prop, sql);
                        first = false;
                    }
                }
                if (!first) sql.append(")");
            }

            if (search.actors != null && search.actors.length > 0) {
                addJoin(whereAdded, search, sql);
                sql.append(" (");
                whereAdded = true;
                boolean first = true;
                for (String actor : search.actors) {
                    if (!first) sql.append("OR ");
                    prop.put("actor", actor);
                    sql.append("actor_=$actor$ ");
                    first = false;
                }
                sql.append(")");
            }

            if (search.due >= 0) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                // sql.append(" (");
                addMathFilter(
                        "due",
                        "<=",
                        new Date(
                                System.currentTimeMillis()
                                        + search.due * MPeriod.DAY_IN_MILLISECONDS),
                        prop,
                        sql);
                // sql.append(" OR due_ = 0) "); - not needed, 0 is lesser then timestamp
            }

            // after where: order
            if (search.order != null) {
                sql.append("ORDER BY ").append(search.order.name().toLowerCase()).append("_ ");
                if (!search.orderAscending) sql.append("DESC ");
            }

            // after order: limit
            if (search.limit > 0) {
                sql.append("LIMIT ").append(search.limit).append(" ");
            }

            con = pool.getConnection();
            DbStatement sta = con.createStatement(sql.toString());
            DbResult res = sta.executeQuery(prop);
            return new SqlResultNode(con, res);
        } catch (Exception e) {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d(t);
            }
            throw new IOException(e);
        }
    }

    @Override
    public Result<PCaseInfo> searchCases(SearchCriterias search) throws IOException {
        DbConnection con = null;
        try {
            StringBuilder sql =
                    new StringBuilder("SELECT " + CASE_COLUMNS + " FROM " + prefix + "_case_ ");
            boolean whereAdded = false;
            MProperties prop = new MProperties();

            if (search.caseState != null) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                prop.put("state", search.caseState);
                sql.append("state_=$state$ ");
            }

            if (search.uri != null) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                addFilter("uri", search.uri, prop, sql);
            }

            if (search.name != null) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                addFilter("name", search.name, prop, sql);
            }

            if (search.custom != null) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                addFilter("custom", search.custom, prop, sql);
            }

            if (search.customer != null) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                addFilter("customer", search.customer, prop, sql);
            }

            if (search.process != null) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                addFilter("process", search.process, prop, sql);
            }

            if (search.version != null) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                addFilter("version", search.version, prop, sql);
            }

            if (search.priority != Integer.MAX_VALUE) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                prop.put("priority", search.priority);
                sql.append("priority_=$priority$ ");
            }

            if (search.score != Integer.MIN_VALUE) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                prop.put("score", search.score);
                sql.append("score_ >= $score$ ");
            }

            if (search.pool != null) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                addFilter("pool", search.pool, prop, sql);
            }

            if (search.milestone != null) {
                addJoin(whereAdded, search, sql);
                whereAdded = true;
                addFilter("milestone", search.milestone, prop, sql);
            }

            if (search.index != null) {
                boolean first = true;
                for (int i = 0; i < MAX_INDEX_VALUES; i++) {
                    if (search.index.length > i && search.index[i] != null) {
                        if (first) {
                            addJoin(whereAdded, search, sql);
                            sql.append(" (");
                            whereAdded = true;
                        } else {
                            sql.append("OR ");
                        }
                        // prop.setString("index" + i, search.index[i]);
                        addFilter("index" + i, search.index[i], prop, sql);
                        first = false;
                    }
                }
                sql.append(") ");
            }

            // after where: order
            if (search.order != null) {
                sql.append("ORDER BY ").append(search.order.name().toLowerCase()).append("_ ");
                if (!search.orderAscending) sql.append("DESC ");
            }

            // after order: limit
            if (search.limit > 0) {
                sql.append("LIMIT ").append(search.limit).append(" ");
            }

            con = pool.getConnection();
            DbStatement sta = con.createStatement(sql.toString());
            DbResult res = sta.executeQuery(prop);
            return new SqlResultCase(con, res);
        } catch (Exception e) {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d(t);
            }
            throw new IOException(e);
        }
    }

    private void addJoin(boolean whereAdded, SearchCriterias search, StringBuilder sql) {
        if (whereAdded) {
            if (search.or) sql.append("OR ");
            else sql.append("AND ");
        } else sql.append("WHERE ");
    }

    private void addFilter(String name, String value, MProperties prop, StringBuilder sql) {
        prop.put(name, value);

        if (value.startsWith("*") || value.endsWith("*")) {
            sql.append(name + "_ like $" + name + "$ ");
            if (value.startsWith("*")) value = "%" + value.substring(1);
            if (value.endsWith("*")) value = value.substring(0, value.length() - 1) + "%";
            prop.put(name, value);
        } else sql.append(name + "_=$" + name + "$ ");
    }

    private void addMathFilter(
            String name, String comp, Object value, MProperties prop, StringBuilder sql) {
        prop.put(name, value);

        sql.append(name + "_ " + comp + " $" + name + "$ ");
    }

    @Override
    public Map<String, String> loadEngine() throws IOException {
        DbConnection con = null;
        try {
            HashMap<String, String> out = new HashMap<>();

            con = pool.getConnection();
            MProperties prop = new MProperties();
            DbStatement sta =
                    con.createStatement("SELECT id_,content_ FROM " + prefix + "_engine_");
            DbResult res = sta.executeQuery(prop);
            while (res.next()) {
                out.put(res.getString("id_"), res.getString("content_"));
            }
            res.close();
            return out;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d(t);
            }
        }
    }

    @Override
    public String getEngineValue(String key) throws IOException {
        DbConnection con = null;
        try {
            String value = null;
            con = pool.getConnection();
            MProperties prop = new MProperties();
            prop.put("id", key);
            DbStatement sta =
                    con.createStatement(
                            "SELECT content_ FROM " + prefix + "_engine_ WHERE id_=$id$");
            DbResult res = sta.executeQuery(prop);
            if (res.next()) value = res.getString("content_");
            res.close();
            return value;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d(key, t);
            }
        }
    }

    @Override
    public void setEngineValue(String key, String value) throws IOException {
        String currentValue = getEngineValue(key); // TODO could be optimized
        DbConnection con = null;
        try {
            con = pool.getConnection();
            MProperties prop = new MProperties();
            prop.put("id", key);
            prop.put("value", value);
            prop.put("now", new Date());
            DbStatement sta =
                    con.createStatement(
                            currentValue == null
                                    ? "INSERT INTO "
                                            + prefix
                                            + "_engine_ (id_,content_,created_,modified_) VALUES ($id$,$value$,$now$,$now$)"
                                    : "UPDATE "
                                            + prefix
                                            + "_engine_ SET content_=$value$,modified_=$now$ WHERE id_=$id$");
            int res = sta.executeUpdate(prop);
            if (res != 1) throw new Exception("Not Updated");

            con.commit();
            con.close();

        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d(key, t);
            }
        }
    }

    @Override
    public void deleteEngineValue(String key) throws IOException {
        DbConnection con = null;
        try {
            con = pool.getConnection();
            MProperties prop = new MProperties();
            prop.put("id", key);
            DbStatement sta =
                    con.createStatement("DELETE FROM " + prefix + "_engine_ WHERE id_=$id$");
            sta.executeUpdate(prop);

            con.commit();

        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d(key, t);
            }
        }
    }

    private static class SqlResultCase implements Result<PCaseInfo>, Iterator<PCaseInfo> {

        private DbResult res;
        private boolean hasNext = false;
        private DbConnection con;

        public SqlResultCase(DbConnection con, DbResult res) throws Exception {
            this.con = con;
            this.res = res;
            hasNext = res.next();
        }

        @Override
        public String toString() {
            return MSystem.toString(this, hasNext);
        }

        @Override
        public Iterator<PCaseInfo> iterator() {
            return this;
        }

        @Override
        public synchronized void close() {
            if (res == null || con == null) return;
            try {
                res.close();
            } catch (Exception e) {
            }
            try {
                con.close();
            } catch (Throwable e) {
            }
            res = null;
            con = null;
        }

        @Override
        public boolean hasNext() {
            if (!hasNext) {
                close();
            }
            return hasNext;
        }

        @Override
        public PCaseInfo next() {
            if (res == null) return null;
            try {
                PCaseInfo out = newPCase(res);
                hasNext = res.next();
                return out;
            } catch (Throwable e) {
                hasNext = false;
                close();
                throw new MRuntimeException(RC.ERROR, e);
            }
        }
    }

    private static class SqlResultNode implements Result<PNodeInfo>, Iterator<PNodeInfo> {

        private DbResult res;
        private boolean hasNext = false;
        private DbConnection con;

        public SqlResultNode(DbConnection con, DbResult res) throws Exception {
            this.con = con;
            this.res = res;
            hasNext = res.next();
        }

        @Override
        public String toString() {
            return MSystem.toString(this, hasNext);
        }

        @Override
        public Iterator<PNodeInfo> iterator() {
            return this;
        }

        @Override
        public synchronized void close() {
            if (res == null || con == null) return;
            try {
                res.close();
            } catch (Exception e) {
            }
            try {
                con.close();
            } catch (Throwable e) {
            }
            res = null;
            con = null;
        }

        @Override
        public boolean hasNext() {
            if (!hasNext) {
                close();
            }
            return hasNext;
        }

        @Override
        public PNodeInfo next() {
            if (res == null) return null;
            try {
                PNodeInfo out = newPNode(res);
                hasNext = res.next();
                return out;
            } catch (Throwable e) {
                hasNext = false;
                close();
                throw new MRuntimeException(RC.ERROR, e);
            }
        }
    }

    protected static STATE_CASE toCaseState(int index) {
        if (index < 0 || index >= STATE_CASE.values().length) return STATE_CASE.CLOSED;
        return STATE_CASE.values()[index];
    }

    protected static PNodeInfo newPNode(DbResult res) throws Exception {
        Timestamp due = res.getTimestamp("due_");
        PNodeInfo out =
                new PNodeInfo(
                        UUID.fromString(res.getString("id_")),
                        UUID.fromString(res.getString("case_")),
                        res.getString("name_"),
                        res.getString("assigned_"),
                        toNodeState(res.getInt("state_")),
                        toNodeType(res.getInt("type_")),
                        res.getString("uri_"),
                        res.getString("custom_"),
                        res.getString("customer_"),
                        res.getTimestamp("created_").getTime(),
                        res.getTimestamp("modified_").getTime(),
                        res.getInt("priority_"),
                        res.getInt("score_"),
                        res.getString("actor_"),
                        due == null ? 0 : due.getTime(),
                        new String[] {
                            res.getString("index0_"),
                            res.getString("index1_"),
                            res.getString("index2_"),
                            res.getString("index3_"),
                            res.getString("index4_"),
                            res.getString("index5_"),
                            res.getString("index6_"),
                            res.getString("index7_"),
                            res.getString("index8_"),
                            res.getString("index9_")
                        });
        return out;
    }

    protected static PCaseInfo newPCase(DbResult res) throws Exception {
        PCaseInfo out =
                new PCaseInfo(
                        UUID.fromString(res.getString("id_")),
                        res.getString("uri_"),
                        res.getString("name_"),
                        toCaseState(res.getInt("state_")),
                        res.getString("custom_"),
                        res.getString("customer_"),
                        res.getTimestamp("created_").getTime(),
                        res.getTimestamp("modified_").getTime(),
                        res.getInt("priority_"),
                        res.getInt("score_"),
                        new String[] {
                            res.getString("index0_"),
                            res.getString("index1_"),
                            res.getString("index2_"),
                            res.getString("index3_"),
                            res.getString("index4_"),
                            res.getString("index5_"),
                            res.getString("index6_"),
                            res.getString("index7_"),
                            res.getString("index8_"),
                            res.getString("index9_")
                        },
                        res.getString("milestone_"));
        return out;
    }

    protected static STATE_NODE toNodeState(int index) {
        if (index < 0 || index >= STATE_NODE.values().length) return STATE_NODE.CLOSED;
        return STATE_NODE.values()[index];
    }

    protected static TYPE_NODE toNodeType(int index) {
        if (index < 0 || index >= TYPE_NODE.values().length) return TYPE_NODE.NODE;
        return TYPE_NODE.values()[index];
    }

    public void dumpCases() {
        DbConnection con = null;
        try {
            con = pool.getConnection();
            MProperties prop = new MProperties();
            DbStatement sta = con.createStatement("SELECT * FROM " + prefix + "_case_");
            DbResult res = sta.executeQuery(prop);
            while (res.next()) {
                System.out.println("CASE:");
                for (String name : res.getColumnNames())
                    if (!name.toLowerCase().equals("content_"))
                        System.out.println("  " + name + ": " + res.getString(name));
            }
        } catch (Exception e) {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d(t);
            }
            throw new RuntimeException(e);
        }
    }

    public void dumpNodes() {
        DbConnection con = null;
        try {
            con = pool.getConnection();
            MProperties prop = new MProperties();
            DbStatement sta = con.createStatement("SELECT * FROM " + prefix + "_node_");
            DbResult res = sta.executeQuery(prop);
            while (res.next()) {
                System.out.println("NODE:");
                for (String name : res.getColumnNames())
                    if (!name.toLowerCase().equals("content_"))
                        System.out.println("  " + name + ": " + res.getString(name));
            }
        } catch (Exception e) {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d(t);
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public PCaseInfo loadCaseInfo(UUID caseId) throws IOException {
        DbConnection con = null;
        try {
            con = pool.getConnection();
            MProperties prop = new MProperties();
            DbStatement sta = null;
            prop.put("id", caseId);
            sta =
                    con.createStatement(
                            "SELECT " + CASE_COLUMNS + " FROM " + prefix + "_case_ WHERE id_=$id$");
            DbResult res = sta.executeQuery(prop);
            if (!res.next()) {
                res.close();
                con.close();
                return null;
            }
            PCaseInfo out = newPCase(res);
            res.close();
            return out;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d("close connection failed", caseId, t);
            }
        }
    }

    @Override
    public PNodeInfo loadFlowNodeInfo(UUID nodeId) throws IOException {
        DbConnection con = null;
        try {
            con = pool.getConnection();
            MProperties prop = new MProperties();
            DbStatement sta = null;
            prop.put("id", nodeId);
            sta =
                    con.createStatement(
                            "SELECT " + NODE_COLUMNS + " FROM " + prefix + "_node_ WHERE id_=$id$");
            DbResult res = sta.executeQuery(prop);
            if (!res.next()) {
                res.close();
                con.close();
                return null;
            }
            PNodeInfo out = newPNode(res);
            res.close();
            return out;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d("close connection failed", nodeId, t);
            }
        }
    }

    @Override
    public boolean setNodePriority(UUID nodeId, int priority) throws IOException {
        DbConnection con = null;
        try {
            con = pool.getConnection();
            MProperties prop = new MProperties();
            DbStatement sta = null;
            prop.put("id", nodeId);
            prop.put("value", priority);
            sta =
                    con.createStatement(
                            "UPDATE " + prefix + "_node_ SET priority_=$value$ WHERE id_=$id$");
            int res = sta.executeUpdate(prop);
            return res == 1;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d("close connection failed", nodeId, t);
            }
        }
    }

    @Override
    public boolean setNodeScope(UUID nodeId, int scope) throws IOException {
        DbConnection con = null;
        try {
            con = pool.getConnection();
            MProperties prop = new MProperties();
            DbStatement sta = null;
            prop.put("id", nodeId);
            prop.put("value", scope);
            sta =
                    con.createStatement(
                            "UPDATE " + prefix + "_node_ SET scope_=$value$ WHERE id_=$id$");
            int res = sta.executeUpdate(prop);
            return res == 1;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d("close connection failed", nodeId, t);
            }
        }
    }

    @Override
    public boolean setCasePriority(UUID caseId, int priority) throws IOException {
        DbConnection con = null;
        try {
            con = pool.getConnection();
            MProperties prop = new MProperties();
            DbStatement sta = null;
            prop.put("id", caseId);
            prop.put("value", priority);
            sta =
                    con.createStatement(
                            "UPDATE " + prefix + "_case_ SET priority_=$value$ WHERE id_=$id$");
            int res = sta.executeUpdate(prop);
            con.close();
            return res == 1;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d("close connection failed", caseId, t);
            }
        }
    }

    @Override
    public boolean setCaseScope(UUID caseId, int scope) throws IOException {
        DbConnection con = null;
        try {
            con = pool.getConnection();
            MProperties prop = new MProperties();
            DbStatement sta = null;
            prop.put("id", caseId);
            prop.put("value", scope);
            sta =
                    con.createStatement(
                            "UPDATE " + prefix + "_case_ SET scope_=$value$ WHERE id_=$id$");
            int res = sta.executeUpdate(prop);
            return res == 1;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            try {
                if (con != null) con.close();
            } catch (Throwable t) {
                log().d("close connection failed", caseId, t);
            }
        }
    }
}
