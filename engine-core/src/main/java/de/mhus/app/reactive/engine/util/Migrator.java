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
package de.mhus.app.reactive.engine.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.error.NotFoundException;
import org.summerclouds.common.core.operation.Monitor;
import org.summerclouds.common.core.pojo.AttributesStrategy;
import org.summerclouds.common.core.pojo.DefaultFilter;
import org.summerclouds.common.core.pojo.PojoModel;
import org.summerclouds.common.core.pojo.PojoParser;
import org.summerclouds.common.core.tool.MCast;
import org.summerclouds.common.core.tool.MCollection;
import org.summerclouds.common.core.tool.MString;
import org.summerclouds.common.core.util.MUri;
import org.summerclouds.common.core.util.Version;
import org.summerclouds.common.core.util.VersionRange;

import de.mhus.app.reactive.engine.Engine;
import de.mhus.app.reactive.model.engine.CaseLock;
import de.mhus.app.reactive.model.engine.PCase;
import de.mhus.app.reactive.model.engine.PCase.STATE_CASE;
import de.mhus.app.reactive.model.engine.PCaseInfo;
import de.mhus.app.reactive.model.engine.PNode;
import de.mhus.app.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.app.reactive.model.engine.PNode.TYPE_NODE;
import de.mhus.app.reactive.model.engine.PNodeInfo;
import de.mhus.app.reactive.model.engine.Result;

public class Migrator {

    private String process;
    private String pool;
    private String activity;
    private String[] ids;
    private Engine engine;
    private boolean test;
    private Monitor monitor;
    private String[] caseRules;
    private String[] nodeRules;

    private PojoModel nodeModel =
            new PojoParser()
                    .parse(PNode.class, new AttributesStrategy(false, true, "_", null))
                    .filter(new DefaultFilter(true, false, false, false, true))
                    .getModel();
    private PojoModel caseModel =
            new PojoParser()
                    .parse(PCase.class, new AttributesStrategy(false, true, "_", null))
                    .filter(new DefaultFilter(true, false, false, false, true))
                    .getModel();
    private VersionRange version;
    private MUri uri;
    private boolean verbose = false;
    private UUID uuid;

    public Migrator(Monitor monitor) {
        this.monitor = monitor;
    }

    public void setSelectedIds(String[] ids) {
        this.ids = ids;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public void suspend() throws IOException, MException {
        monitor.setSteps(0);
        for (PCaseInfo info : getCases()) {
            if (info.getState() != STATE_CASE.SUSPENDED
                    && info.getState() != STATE_CASE.CLOSED
                    && filter(info)) {
                monitor.println("*** Suspend " + info);
                if (!test) {
                    try (CaseLock lock = engine.getCaseLock(info, "migrator.suspend")) {
                        monitor.incrementStep();
                        engine.suspendCase(info.getId());
                        engine.prepareMigrateCase(lock);
                    }
                }
            }
        }
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    public void migrate() throws NotFoundException, IOException {

        if (caseRules == null && nodeRules == null) return;

        monitor.println("URI:      ", uri);
        monitor.println("Pool:     ", pool);
        monitor.println("Process:  ", process);
        monitor.println("Activity: ", activity);
        monitor.println("Version:  ", version);

        monitor.setSteps(0);
        for (PCaseInfo caseInfo : getCases()) {
            if (filter(caseInfo)) {

                if (caseRules != null) {
                    PCase caze = engine.getCaseWithoutLock(caseInfo.getId());
                    if (caze.getState() == STATE_CASE.SUSPENDED
                            || caze.getState() == STATE_CASE.CLOSED) {
                        monitor.incrementStep();
                        monitor.println(">>> Migrate " + caseInfo);
                        if (!test) {
                            migrateCase(caze);
                            engine.storageUpdateFull(caze);
                        } else {
                            System.out.println(caze);
                            System.out.println(caze.getParameters());
                        }
                    } else if (test) monitor.println("--- Incorrect state " + caseInfo);
                }
                if (nodeRules != null) {
                    for (PNodeInfo nodeInfo : engine.storageGetFlowNodes(caseInfo.getId(), null)) {
                        if (filter(nodeInfo)) {
                            PNode node = engine.getNodeWithoutLock(nodeInfo.getId());
                            if (node.getState() == STATE_NODE.SUSPENDED
                                    || node.getState() == STATE_NODE.CLOSED) {
                                monitor.println(">>> Migrate " + nodeInfo);
                                if (!test) {
                                    migrateNode(node);
                                    engine.storageUpdateFull(node);
                                } else {
                                    monitor.println(node);
                                    monitor.println(node.getParameters());
                                }
                            } else if (test) monitor.println("--- Incorrect state " + nodeInfo);
                        }
                    }
                }
            }
        }
    }

    private Result<PCaseInfo> getCases() throws IOException {
        if (uuid == null) return engine.storageGetCases(null);
        try {
            if (caseRules != null) {
                final PCaseInfo entry = engine.storageGetCaseInfo(uuid);
                if (entry != null)
                    return new Result<PCaseInfo>() {
                        @Override
                        public Iterator<PCaseInfo> iterator() {
                            ArrayList<PCaseInfo> list = new ArrayList<>(1);
                            list.add(entry);
                            return list.iterator();
                        }

                        @Override
                        public void close() {}
                    };
            }
            if (nodeRules != null) {
                PNodeInfo node = engine.storageGetFlowNodeInfo(uuid);
                if (node != null) {
                    final PCaseInfo entry = engine.storageGetCaseInfo(node.getCaseId());
                    if (entry != null)
                        return new Result<PCaseInfo>() {
                            @Override
                            public Iterator<PCaseInfo> iterator() {
                                ArrayList<PCaseInfo> list = new ArrayList<>(1);
                                list.add(entry);
                                return list.iterator();
                            }

                            @Override
                            public void close() {}
                        };
                }
                final PCaseInfo entry = engine.storageGetCaseInfo(uuid);
                if (entry != null)
                    if (entry != null)
                        return new Result<PCaseInfo>() {
                            @Override
                            public Iterator<PCaseInfo> iterator() {
                                ArrayList<PCaseInfo> list = new ArrayList<>(1);
                                list.add(entry);
                                return list.iterator();
                            }

                            @Override
                            public void close() {}
                        };
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
        return new Result<PCaseInfo>() {
            @Override
            public Iterator<PCaseInfo> iterator() {
                ArrayList<PCaseInfo> list = new ArrayList<>(0);
                return list.iterator();
            }

            @Override
            public void close() {}
        };
    }

    public void resume() throws IOException, MException {
        monitor.setSteps(0);
        for (PCaseInfo info : getCases()) {
            if (info.getState() == STATE_CASE.SUSPENDED && filter(info)) {
                monitor.incrementStep();
                monitor.println("*** Resume " + info);
                if (!test) engine.resumeCase(info.getId());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void migrateNode(PNode node) {

        for (String rule : nodeRules) {
            try {
                if (MString.isIndex(rule, ':')) {
                    String action = MString.beforeIndex(rule, ':');
                    rule = MString.afterIndex(rule, ':');
                    String k = null;
                    String v = null;
                    if (MString.isIndex(rule, '=')) {
                        k = MString.beforeIndex(rule, '=');
                        v = MString.afterIndex(rule, '=');
                    }
                    monitor.println("=== Node ", node.getId(), " Action ", action, " Rule ", rule);
                    switch (action) {
                        case "name":
                            nodeModel.getAttribute("name").set(node, rule, true);
                            break;
                        case "canonical":
                            nodeModel.getAttribute("canonicalname").set(node, rule, true);
                            break;
                        case "type":
                            nodeModel.getAttribute("type").set(node, TYPE_NODE.valueOf(rule), true);
                            break;
                        case "actor":
                            nodeModel.getAttribute("actor").set(node, rule, true);
                            break;
                        case "status":
                            node.setSuspendedState(STATE_NODE.valueOf(rule));
                            break;
                        case "":
                        case "string":
                            node.getParameters().put(k, v);
                            break;
                        case "date":
                            node.getParameters().put(k, MCast.toDate(v, null));
                            break;
                        case "long":
                            node.getParameters().put(k, MCast.tolong(v, 0));
                            break;
                        case "int":
                        case "integer":
                            node.getParameters().put(k, MCast.toint(v, 0));
                            break;
                        case "bool":
                        case "boolean":
                            node.getParameters().put(k, MCast.toboolean(v, false));
                            break;
                        case "uuid":
                            node.getParameters().put(k, UUID.fromString(v));
                            break;
                        case "double":
                            node.getParameters().put(k, MCast.todouble(v, 0));
                            break;
                        case "rm":
                            node.getParameters().remove(rule);
                            break;
                        case "message":
                            if (rule.equals("null")) {
                                node.setMessage(null);
                            } else {
                                if (node.getMessage() == null) node.setMessage(new HashMap<>());
                                node.getMessage().put(k, v);
                            }
                            break;
                        default:
                            monitor.println("*** Unknown action " + action);
                    }
                }
            } catch (Throwable t) {
                monitor.println("*** Rule: " + rule);
                monitor.println(t);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void migrateCase(PCase caze) {
        for (String rule : caseRules) {
            try {
                if (MString.isIndex(rule, ':')) {
                    String action = MString.beforeIndex(rule, ':');
                    rule = MString.afterIndex(rule, ':');
                    String k = null;
                    String v = null;
                    if (MString.isIndex(rule, '=')) {
                        k = MString.beforeIndex(rule, '=');
                        v = MString.afterIndex(rule, '=');
                    }
                    monitor.println("=== Case ", caze.getId(), " Action ", action, " Rule ", rule);
                    switch (action) {
                        case "uri":
                            caseModel.getAttribute("uri").set(caze, rule, true);
                            break;
                        case "name":
                            caseModel.getAttribute("name").set(caze, rule, true);
                            break;
                        case "canonical":
                            caseModel.getAttribute("canonicalname").set(caze, rule, false);
                            break;
                        case "milestone":
                            caze.setMilestone(rule);
                            break;
                        case "closeCode":
                            caseModel
                                    .getAttribute("closecode")
                                    .set(caze, MCast.toint(rule, 0), true);
                            break;
                        case "closeMessage":
                            caseModel.getAttribute("closemessage").set(caze, rule, true);
                            break;
                        case "status":
                            caze.setState(STATE_CASE.valueOf(rule));
                            break;
                        case "":
                        case "string":
                            caze.getParameters().put(k, v);
                            break;
                        case "date":
                            caze.getParameters().put(k, MCast.toDate(v, null));
                            break;
                        case "long":
                            caze.getParameters().put(k, MCast.tolong(v, 0));
                            break;
                        case "int":
                        case "integer":
                            caze.getParameters().put(k, MCast.toint(v, 0));
                            break;
                        case "bool":
                        case "boolean":
                            caze.getParameters().put(k, MCast.toboolean(v, false));
                            break;
                        case "double":
                            caze.getParameters().put(k, MCast.todouble(v, 0));
                            break;
                        case "uuid":
                            caze.getParameters().put(k, UUID.fromString(v));
                            break;
                        case "rm":
                            caze.getParameters().remove(rule);
                            break;
                        default:
                            monitor.println("*** Unknown action ", action);
                    }
                }
            } catch (Throwable t) {
                monitor.println("*** Rule: " + rule);
                monitor.println(t);
            }
        }
    }

    private boolean filter(PNodeInfo info) {

        boolean filtered = false;
        if (activity != null) {
            filtered = true;
            if (!info.getCanonicalName().equals(activity)) return false;
        }

        if (ids != null && nodeRules != null) {
            filtered = true;
            if (!MCollection.contains(ids, info.getId().toString())) return false;
        }
        if (uuid != null) {
            if (!info.getId().equals(uuid) && !info.getCaseId().equals(uuid)) {
                return false;
            }
        }

        if (!filtered) return false;

        return true;
    }

    private boolean filter(PCaseInfo info) {
        boolean filtered = false;
        if (uri != null) {
            filtered = true;
            MUri u = MUri.toUri(info.getUri());
            String p = u.getLocation();
            String v = MString.afterIndex(p, ':');
            p = MString.beforeIndex(p, ':');
            String oPool = MString.beforeIndex(u.getPath(), '/', u.getPath());

            if (!p.equals(process)) {
                if (verbose) monitor.println("--- Ignore by process name ", u);
                return false;
            }
            if (version != null && !version.includes(new Version(v))) {
                if (verbose) monitor.println("--- Ignore by process version ", u);
                return false;
            }
            if (MString.isSet(pool) && !pool.equals(oPool)) {
                if (verbose) monitor.println("--- Ignore by pool name ", u);
                return false;
            }
        }
        if (uuid != null) {
            filtered = true;
            if (!info.getId().equals(uuid)) {
                if (verbose) monitor.println("--- Ignore by UUID ", uuid);
                return false;
            }
        }

        if (ids != null && nodeRules != null) {
            try {
                for (PNodeInfo nodeInfo : engine.storageGetFlowNodes(info.getId(), null)) {
                    if (MCollection.contains(ids, nodeInfo.getId().toString())) return true;
                }
            } catch (Throwable t) {
            }
        }

        if (ids != null && caseRules != null) {
            filtered = true;
            if (!MCollection.contains(ids, info.getId().toString())) {
                if (verbose) monitor.println("--- Ignore by id ", info.getId());
                return false;
            }
        }

        if (!filtered) return false;

        return true;
    }

    public void setCaseRules(String[] caseRules) {
        this.caseRules = caseRules;
    }

    public void setNodeRules(String[] nodeRules) {
        this.nodeRules = nodeRules;
    }

    public void setUri(MUri uri) {
        this.uri = uri;
        process = uri.getLocation();
        version = null;
        activity = null;
        if (MString.isIndex(process, ':')) {
            version = new VersionRange(MString.afterIndex(process, ':'));
            process = MString.beforeIndex(process, ':');
        }
        pool = uri.getPath();
        if (MString.isIndex(pool, '/')) {
            activity = MString.afterIndex(pool, '/');
            pool = MString.beforeIndex(pool, '/');
        }
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }
}
