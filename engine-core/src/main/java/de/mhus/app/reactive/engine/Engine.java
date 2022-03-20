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
package de.mhus.app.reactive.engine;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.WeakHashMap;

import org.summerclouds.common.core.concurrent.Lock;
import org.summerclouds.common.core.error.AccessDeniedException;
import org.summerclouds.common.core.error.IResult;
import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.error.MRuntimeException;
import org.summerclouds.common.core.error.NotFoundException;
import org.summerclouds.common.core.error.RC;
import org.summerclouds.common.core.error.TimeoutException;
import org.summerclouds.common.core.error.TimeoutRuntimeException;
import org.summerclouds.common.core.error.UsageException;
import org.summerclouds.common.core.error.ValidationException;
import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.core.node.IProperties;
import org.summerclouds.common.core.node.MProperties;
import org.summerclouds.common.core.tool.MCast;
import org.summerclouds.common.core.tool.MPeriod;
import org.summerclouds.common.core.tool.MString;
import org.summerclouds.common.core.tool.MSystem;
import org.summerclouds.common.core.tool.MThread;
import org.summerclouds.common.core.tool.MTracing;
import org.summerclouds.common.core.tool.MValidator;
import org.summerclouds.common.core.tracing.IScope;
import org.summerclouds.common.core.util.MUri;
import org.summerclouds.common.core.util.MutableUri;

import de.mhus.app.reactive.engine.util.CaseLockImpl;
import de.mhus.app.reactive.engine.util.CaseLockProxy;
import de.mhus.app.reactive.engine.util.EngineListenerUtil;
import de.mhus.app.reactive.model.activity.AActivity;
import de.mhus.app.reactive.model.activity.AActor;
import de.mhus.app.reactive.model.activity.AElement;
import de.mhus.app.reactive.model.activity.APool;
import de.mhus.app.reactive.model.activity.AProcess;
import de.mhus.app.reactive.model.activity.AStartPoint;
import de.mhus.app.reactive.model.activity.ASwimlane;
import de.mhus.app.reactive.model.activity.AUserTask;
import de.mhus.app.reactive.model.annotations.ActivityDescription;
import de.mhus.app.reactive.model.annotations.Trigger;
import de.mhus.app.reactive.model.annotations.Trigger.TYPE;
import de.mhus.app.reactive.model.engine.AaaProvider;
import de.mhus.app.reactive.model.engine.CaseLock;
import de.mhus.app.reactive.model.engine.CaseLockProvider;
import de.mhus.app.reactive.model.engine.ContextRecipient;
import de.mhus.app.reactive.model.engine.EElement;
import de.mhus.app.reactive.model.engine.EEngine;
import de.mhus.app.reactive.model.engine.EPool;
import de.mhus.app.reactive.model.engine.EProcess;
import de.mhus.app.reactive.model.engine.EngineConst;
import de.mhus.app.reactive.model.engine.EngineListener;
import de.mhus.app.reactive.model.engine.InternalEngine;
import de.mhus.app.reactive.model.engine.PCase;
import de.mhus.app.reactive.model.engine.PCase.STATE_CASE;
import de.mhus.app.reactive.model.engine.PCaseInfo;
import de.mhus.app.reactive.model.engine.PCaseLock;
import de.mhus.app.reactive.model.engine.PEngine;
import de.mhus.app.reactive.model.engine.PNode;
import de.mhus.app.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.app.reactive.model.engine.PNode.TYPE_NODE;
import de.mhus.app.reactive.model.engine.PNodeInfo;
import de.mhus.app.reactive.model.engine.ProcessContext;
import de.mhus.app.reactive.model.engine.ProcessProvider;
import de.mhus.app.reactive.model.engine.Result;
import de.mhus.app.reactive.model.engine.RuntimeNode;
import de.mhus.app.reactive.model.engine.SearchCriterias;
import de.mhus.app.reactive.model.engine.StorageProvider;
import de.mhus.app.reactive.model.errors.EngineException;
import de.mhus.app.reactive.model.errors.TaskException;
import de.mhus.app.reactive.model.errors.TechnicalException;
import de.mhus.app.reactive.model.util.ActivityUtil;
import de.mhus.app.reactive.model.util.CloseActivity;
import de.mhus.app.reactive.model.util.InactiveStartPoint;
import de.mhus.app.reactive.model.util.IndexValuesProvider;
import de.mhus.app.reactive.model.util.LocalCaseLockProvider;
import de.mhus.app.reactive.model.util.NoPool;
import de.mhus.app.reactive.model.util.ValidateParametersBeforeExecute;

public class Engine extends MLog implements EEngine, InternalEngine {

    private StorageProvider storage;
    private StorageProvider archive;
    private ProcessProvider processProvider;
    private HashSet<UUID> executing = new HashSet<>();
    private EngineConfiguration config;
    private EngineListener fireEvent = null;
    private CaseLockProvider lockProvider = new LocalCaseLockProvider();
    private WeakHashMap<UUID, EngineCaseLock> caseLocks = new WeakHashMap<>();
    private long statisticCaseClosed;
    private long statisticRounds;
    private long statisticCaseStarted;

    public Engine(EngineConfiguration config) throws IOException {
        this.config = config;
        fireEvent = EngineListenerUtil.createEngineEventProcessor(config);
        storage = config.storage;
        archive = config.archive;
        processProvider = config.processProvider;

        config.persistent = new PEngine(storage);
        if (config.parameters != null) {
            config.persistent.getParameters().putAll(config.parameters);
            try {
                config.persistent.save();
            } catch (IOException e) {
                log().e(e);
            }
        }

        if (config.lockProvider == null) config.lockProvider = new LocalCaseLockProvider();
        lockProvider = config.lockProvider;
    }

    @Override
    public boolean isReady() {
        return lockProvider != null && lockProvider.isReady();
    }

    // ---

    public void step() throws IOException, NotFoundException {
        doPrepareNodes();
        doProcessNodes();
        doCleanupCases();
    }

    public void doPrepareNodes() throws IOException, NotFoundException {

        // Prepare
        long now = System.currentTimeMillis();

        // SCHEDULED NODES
        fireEvent.doStep("scheduled");
        for (PNodeInfo nodeId : storage.getScheduledFlowNodes(STATE_NODE.SCHEDULED, now, false)) {
            try {
                PNodeInfo nodeInfo = getFlowNodeInfo(nodeId.getId());
                PCaseLock lock =
                        getCaseLockOrNull(
                                nodeInfo, "global:node:scheduled:" + nodeInfo.getCanonicalName());
                if (lock == null) continue;
                try (lock) {
                    PNode node = lock.getFlowNode(nodeId.getId());
                    if (node.getState() != STATE_NODE.SCHEDULED) continue;
                    // set state back to ready
                    fireEvent.setScheduledToRunning(node);
                    node.setState(STATE_NODE.RUNNING);
                    lock.saveFlowNode(null, node, null);
                }
            } catch (Throwable t) {
                log().e("scheduled node {1} failed", nodeId, t);
            }
        }
        for (PNodeInfo nodeInfo : storage.getScheduledFlowNodes(STATE_NODE.WAITING, now, false)) {
            try {
                PCaseLock lock =
                        getCaseLockOrNull(
                                nodeInfo, "global:node:waiting:" + nodeInfo.getCanonicalName());
                if (lock == null) continue;
                try (lock) {
                    PCase caze = lock.getCase();
                    PNode node = lock.getFlowNode(nodeInfo.getId());
                    if (node.getState() != STATE_NODE.WAITING) continue;
                    if (caze.getState() == STATE_CASE.RUNNING) fireScheduledTrigger(lock, nodeInfo);
                    else if (caze.getState() == STATE_CASE.CLOSED) {
                        // stop node also
                        log().d("auto stop waiting node", nodeInfo);
                        node.setSuspendedState(node.getState());
                        node.setState(STATE_NODE.STOPPED);
                        storage.saveFlowNode(node);
                    }
                }
            } catch (Throwable t) {
                log().e("waiting node {1} failed", nodeInfo, t);
            }
        }
    }

    @SuppressWarnings("unlikely-arg-type")
    public int doProcessNodes() throws IOException, NotFoundException {

        int doneCnt = 0;
        long now = System.currentTimeMillis();
        statisticRounds++;

        // READY NODES
        fireEvent.doStep("execute");
        Result<PNodeInfo> result = storage.getScheduledFlowNodes(STATE_NODE.RUNNING, now, true);
        if (config.executeParallel) {
            int maxThreads = config.maxThreads;
            LinkedList<FlowNodeExecutor> threads = new LinkedList<>();
            for (PNodeInfo nodeInfo : result) {

                if (!isNodeActive(nodeInfo)) continue;

                PCaseLock lockx =
                        getCaseLockOrNull(
                                nodeInfo,
                                "node:" + nodeInfo.getCanonicalName(),
                                "id",
                                nodeInfo.getId());
                if (lockx == null || !(lockx instanceof EngineCaseLock)) {
                    if (lockx != null) lockx.close();
                    continue;
                }

                MTracing.get().cleanup(); //XXX

                EngineCaseLock lock = (EngineCaseLock) lockx;

                try {
                    PNode node = lock.getFlowNode(nodeInfo);
                    PCase caze = lock.getCase();
                    if (isProcessHealthy(caze) && isNodeActive(node)) {
                        if (caze.getState() == STATE_CASE.RUNNING) {
                            doneCnt++;
                            FlowNodeExecutor executor = new FlowNodeExecutor(lock, node);
                            threads.add(executor);
                            synchronized (executing) {
                                executing.add(node.getId());
                            }
                            Thread thread = new Thread(executor);
                            executor.thread = thread;
                            thread.setName(
                                    "reactive-executor: " + node.getId() + " " + node.getName());
                            thread.start();
                            lock = null;
                            if (threads.size() >= maxThreads) break;
                        } else if (caze.getState() == STATE_CASE.CLOSED) {
                            // stop node also
                            log().d("auto stop running node", nodeInfo);
                            node.setSuspendedState(node.getState());
                            node.setState(STATE_NODE.STOPPED);
                            storage.saveFlowNode(node);
                        }
                    }
                } catch (Throwable t) {
                    log().e("processing node {1} failed", nodeInfo, t);
                } finally {
                    if (lock != null) lock.close();
                }
            }

            while (threads.size() > 0) {
                threads.removeIf(e -> e.isFinished());
                //    MThread.sleep(200);
            }

            // sleep
            MThread.sleep(config.sleepBetweenProgress);

        } else {
            for (PNodeInfo nodeInfo : result) {

                if (!isNodeActive(nodeInfo)) continue;

                PCaseLock lock =
                        getCaseLockOrNull(
                                nodeInfo,
                                "node:" + nodeInfo.getCanonicalName(),
                                "id",
                                nodeInfo.getId());
                if (lock == null) continue;

                try (lock) {
                    PNode node = lock.getFlowNode(nodeInfo);
                    PCase caze = lock.getCase();
                    if (isProcessHealthy(caze) && isNodeActive(node)) {
                        if (caze.getState() == STATE_CASE.RUNNING) {
                            synchronized (executing) {
                                executing.add(nodeInfo.getId());
                            }
                            doneCnt++;
                            lock.doFlowNode(node);
                            synchronized (executing) {
                                executing.remove(nodeInfo);
                            }
                            // sleep
                            MThread.sleep(config.sleepBetweenProgress);

                        } else if (caze.getState() == STATE_CASE.CLOSED) {
                            // stop node also
                            node.setSuspendedState(node.getState());
                            node.setState(STATE_NODE.STOPPED);
                            storage.saveFlowNode(node);
                        }
                    }
                } catch (Throwable t) {
                    log().e("processing node {1} failed", nodeInfo, t);
                }
            }
        }
        result.close();

        fireEvent.doStep("execute finished");

        return doneCnt;
    }

    public boolean isNodeActive(PNode node) {
        return node != null && node.getState() == STATE_NODE.RUNNING;
    }

    public boolean isNodeActive(PNodeInfo node) {
        return node != null && node.getState() == STATE_NODE.RUNNING;
    }

    public boolean isProcessHealthy(PCase caze) {
        try {
            MUri uri = MUri.toUri(caze.getUri());
            EProcess process = getProcess(uri);
            if (process == null) {
                log().d("Process not available", caze, uri);
                return false;
            }
            EPool pool = getPool(process, uri);
            if (pool == null) {
                log().d("Pool not available", caze, uri);
                return false;
            }
            if (pool.getCanonicalName() == null) {
                log().d("Canonical name not found", caze, uri);
                return false;
            }
            return true;
        } catch (Throwable t) {
            log().d("isProcessHealthy", caze, t);
        }
        return false;
    }

    public void doCleanupCases() throws IOException, NotFoundException {

        // scan for closeable cases and runtimes
        fireEvent.doStep("cleanup");
        for (PCaseInfo caseInfo : storage.getCases(STATE_CASE.RUNNING)) {
            try {
                PCaseLock lock =
                        getCaseLockOrNull(
                                caseInfo.getId(),
                                "global:cleanup:" + caseInfo.getCanonicalName(),
                                "id",
                                caseInfo.getId());
                if (lock == null) continue;
                try (lock) {
                    boolean found = false;
                    boolean onlyStopped = true;
                    HashSet<UUID> allRuntime = new HashSet<>();
                    HashSet<UUID> usedRuntime = new HashSet<>();

                    for (PNodeInfo nodeId : storage.getFlowNodes(caseInfo.getId(), null)) {
                        try {
                            PNode node = lock.getFlowNode(nodeId);
                            if (node.getType() == TYPE_NODE.RUNTIME
                                    && node.getState() == STATE_NODE.WAITING) {
                                allRuntime.add(node.getId());
                            } else if (node.getState() != STATE_NODE.CLOSED
                                    && node.getState() != STATE_NODE.ZOMBIE) {
                                found = true;
                                usedRuntime.add(node.getRuntimeId());
                                if (node.getState() != STATE_NODE.STOPPED) onlyStopped = false;
                            }
                        } catch (Throwable t) {
                            log().w("cleanup node {1} failed", nodeId, t);
                        }
                    }

                    // close unused runtimes
                    allRuntime.removeIf(u -> usedRuntime.contains(u));
                    for (UUID rId : allRuntime) {
                        try {
                            lock.closeRuntime(rId);
                        } catch (Throwable t) {
                            log().w("close runtime {1} failed", rId, t);
                            fireEvent.error(rId, t);
                        }
                    }
                    if (!found) {
                        // close case without active node
                        lock.closeCase(false, 0, "");
                    } else if (onlyStopped) {
                        try {
                            // severe case if only STOPPED nodes exists - this will stop execution
                            // of the node until resume.
                            PCase caze = lock.getCase();
                            caze.setState(STATE_CASE.SEVERE);
                            storage.saveCase(caze);
                        } catch (MException e) {
                            log().w("close case {1} failed", caseInfo, e);
                        }
                    }
                }
            } catch (Throwable te) {
                log().w("cleanup for case {1} failed", caseInfo, te);
            }
        }

        fireEvent.doStep("cleanup finished");
    }

    private class FlowNodeExecutor implements Runnable {

        public volatile boolean finished = false;
        public boolean outtimed = false;

        @SuppressWarnings("unused")
        public Thread thread;

        private PNode node;
        long start = System.currentTimeMillis();
        private EngineCaseLock lock;

        public FlowNodeExecutor(EngineCaseLock lock, PNode node) {
            this.lock = lock;
            this.node = node;
        }

        public boolean isFinished() {
            if (finished || outtimed) return true;
            try {
                if (MPeriod.isTimeOut(start, node.getActivityTimeout())) {
                    fireEvent.error("activity timeout", node);
                    outtimed = true;
                    return true;
                }
            } catch (Throwable t) {
                t.printStackTrace(); // should not happen
            }
            return false;
        }

        @Override
        public void run() {
            try (IScope scope = MTracing.enter(lock.getSpan(), "run")) {
                lock.owner = Thread.currentThread();
                start = System.currentTimeMillis();
                try {
                    lock.doFlowNode(node);
                } catch (Throwable t) {
                    try {
                        log().e("run node {1} failed", node, t);
                        fireEvent.error(node, t);
                    } catch (Throwable t2) {
                    }
                }
                lock.close();
                synchronized (executing) {
                    executing.remove(node.getId());
                }
            } catch (Throwable t) {
                log().e("run failed for {1}", node, t);
            }
            finished = true;
        }
    }

    public List<UUID> getExecuting() {
        synchronized (executing) {
            return new LinkedList<UUID>(executing);
        }
    }

    public void resaveFlowNode(UUID nodeId) throws MException, IOException {
        try (CaseLock lock = getCaseLockByNode(nodeId, "resaveFlowNode")) {
            PCase caze = lock.getCase();
            PNode node = lock.getFlowNode(nodeId);
            EngineContext context = createContext(lock, caze, node);

            {
                node.getSchedulers().clear();
                HashMap<String, Long> list = context.getENode().getSchedulerList();
                if (list != null) node.getSchedulers().putAll(list);
            }
            {
                node.getMessageTriggers().clear();
                HashMap<String, String> list = context.getENode().getMessageList();
                if (list != null) node.getMessageTriggers().putAll(list);
            }
            {
                node.getSignalTriggers().clear();
                HashMap<String, String> list = context.getENode().getSignalList();
                if (list != null) node.getSignalTriggers().putAll(list);
            }
            storage.saveFlowNode(node);
        }
    }

    public void resaveCase(UUID caseId) throws IOException, NotFoundException, TimeoutException {
        try (CaseLock lock = getCaseLock(caseId, "resaveCase")) {
            PCase caze = lock.getCase();
            storage.saveCase(caze);
        }
    }

    public Trigger getTrigger(EngineContext context, String key) {

        if (key.startsWith("[")) {
            int index = MCast.toint(key.substring(1), -1);
            Trigger[] list = context.getENode().getTriggers();
            if (index < 0 || list.length <= index) return null;
            return list[index];
        }
        for (Trigger trigger : context.getENode().getTriggers()) {
            if (trigger.name().equals(key)) return trigger;
        }
        return null;
    }

    @Override
    public void doNodeErrorHandling(PNode closeNode, String error) throws Exception {
        try (PCaseLock lock = getCaseLock(closeNode, "doNodeErrorHandling")) {
            EngineContext context = createContext(lock, lock.getCase(), closeNode);
            lock.doNodeErrorHandling(context, closeNode, new TaskException(error, "syntetic"));
        }
    }

    // ---

    public Object doExecute(String uri) throws Exception {
        MUri u = MUri.toUri(uri);
        return execute(u);
    }

    public Object execute(MUri uri) throws Exception {
        return execute(uri, null);
    }

    /**
     * Execute the URI command. bpm:// - start case bpmm:// - send message bpms:// - send signal
     * bpme:// - send external bpmx:// - execute additional start point
     *
     * @param uri
     * @param parameters if null the parameters are taken from uri query
     * @return The result of the action, e.g. UUID for new case.
     * @throws Exception
     */
    @Override
    public Object execute(MUri uri, IProperties parameters) throws Exception {
        switch (uri.getScheme()) {
            case "bpm":
                {
                    // check access
                    String user = uri.getUsername();
                    if (user != null) {
                        String pass = uri.getPassword();
                        if (!config.aaa.validatePassword(user, pass))
                            throw new AccessDeniedException("login failed", user, uri);

                        if (!hasInitiateAccess(uri, user))
                            throw new AccessDeniedException("user is not initiator", user, uri);
                    }
                    UUID id = (UUID) start(uri, null, parameters);

                    String[] uriParams = uri.getParams();
                    if (uriParams != null && uriParams.length > 0) {
                        MProperties options = IProperties.explodeToMProperties(uriParams);
                        if (options.getBoolean(EngineConst.PARAM_PROGRESS, false)) {
                            long waitTime = 200;
                            long start = System.currentTimeMillis();
                            while (true) {
                                PCaseInfo caze = getCaseInfo(id);
                                if (EngineConst.MILESTONE_PROGRESS.equals(caze.getMilestone())) {
                                    break;
                                }
                                if (caze.getState() == STATE_CASE.CLOSED
                                        || caze.getState() == STATE_CASE.SUSPENDED)
                                    throw new MException(RC.WARNING_TEMPORARILY, "Progress not reached before close in case {1}", id);
                                if (MPeriod.isTimeOut(start, config.progressTimeout))
                                    throw new TimeoutRuntimeException(
                                            "Wait for progress timeout", id);
                                Thread.sleep(waitTime);
                            }
                        }
                    }
                    return id;
                }
            case "bpmm":
                {
                    // check access
                    String user = uri.getUsername();
                    if (user != null) {
                        String pass = uri.getPassword();
                        if (!config.aaa.validatePassword(user, pass))
                            throw new AccessDeniedException("login failed", user, uri);

                        if (!hasInitiateAccess(uri, user))
                            throw new AccessDeniedException("user is not initiator", user, uri);
                    }

                    UUID caseId = null;
                    String l = uri.getLocation();
                    if (MValidator.isUUID(l)) caseId = UUID.fromString(l);
                    String m = uri.getPath();

                    if (parameters == null) {
                        parameters = new MProperties();
                        Map<String, String> p = uri.getQuery();
                        if (p != null) parameters.putAll(p);
                    }
                    fireMessage(caseId, m, parameters);
                    return null;
                }
            case "bpms":
                {
                    // check access
                    String user = uri.getUsername();
                    if (user != null) {
                        String pass = uri.getPassword();
                        if (!config.aaa.validatePassword(user, pass))
                            throw new AccessDeniedException("login failed", user, uri);

                        if (!hasInitiateAccess(uri, user))
                            throw new AccessDeniedException("user is not initiator", user, uri);
                    }

                    String signal = uri.getPath();
                    if (parameters == null) {
                        parameters = new MProperties();
                        Map<String, String> p = uri.getQuery();
                        if (p != null) parameters.putAll(p);
                    }
                    String l = uri.getLocation();
                    UUID caseId = null;
                    if (MValidator.isUUID(l)) caseId = UUID.fromString(l);
                    return fireSignal(caseId, signal, parameters);
                }
            case "bpme":
                {
                    String l = uri.getLocation();
                    if (!MValidator.isUUID(l)) throw new MException(RC.SYNTAX_ERROR, "misspelled node id", l);
                    UUID nodeId = UUID.fromString(l);

                    String user = uri.getUsername();
                    if (user != null) {
                        String pass = uri.getPassword();
                        if (!config.aaa.validatePassword(user, pass))
                            throw new AccessDeniedException("login failed", user, uri);

                        if (!hasExecuteAccess(nodeId, user))
                            throw new AccessDeniedException("user can't execute", user, uri);
                    }

                    if (parameters == null) {
                        parameters = new MProperties();
                        Map<String, String> p = uri.getQuery();
                        if (p != null) parameters.putAll(p);
                    }

                    String taskName = uri.getPath();
                    if (MString.isEmptyTrim(taskName)) taskName = null; // ignore if is empty
                    fireExternal(nodeId, taskName, parameters);
                    return null;
                }
            case "bpmx":
                {
                    String l = uri.getLocation();
                    if (!MValidator.isUUID(l)) throw new MException(RC.SYNTAX_ERROR, "misspelled case id", l);
                    UUID caseId = UUID.fromString(l);
                    // check start point
                    PCaseInfo caze = getCaseInfo(caseId);
                    if (caze == null) throw new MException(RC.NOT_FOUND, "case {1} not found", caseId);
                    if (caze.getState() == STATE_CASE.SUSPENDED)
                        throw new MException(RC.CONFLICT, "case {1} suspended", caseId);
                    if (caze.getState() == STATE_CASE.CLOSED)
                        throw new MException(RC.CONFLICT, "case {1} closed", caseId);
                    // check access
                    String user = uri.getUsername();
                    if (user != null) {
                        String pass = uri.getPassword();
                        if (!config.aaa.validatePassword(user, pass))
                            throw new AccessDeniedException("login for {1} failed", user, uri);

                        if (!hasInitiateAccess(uri, user))
                            throw new AccessDeniedException("user {1} is not initiator", user, uri);
                    }
                    // parameters
                    if (parameters == null) {
                        parameters = new MProperties();
                        Map<String, String> p = uri.getQuery();
                        if (p != null) parameters.putAll(p);
                    }
                    // context and start
                    try (PCaseLock lock = getCaseLock(caze, "execute", "uri", uri)) {
                        EngineContext context = createContext(lock);
                        EElement start = context.getEPool().getElement(uri.getFragment());
                        if (start == null)
                            throw new MException(RC.NOT_FOUND, "start point not found", uri.getFragment());

                        return lock.createStartPoint(context, start, uri.getQuery());
                    }
                }
            case "bpma": // case action bpma://case-id/action?parameters
                {
                    String l = uri.getLocation();
                    if (!MValidator.isUUID(l)) throw new MException(RC.SYNTAX_ERROR, "misspelled case id", l);
                    UUID caseId = UUID.fromString(l);
                    return onUserCaseAction(caseId, uri.getPath(), new MProperties(uri.getQuery()));
                }
                // case "bpmq": // not implemented use executeQuery()
            default:
                throw new MException(RC.ERROR, "scheme unknown", uri.getScheme());
        }
    }

    public UUID start(String uri) throws Exception {
        MUri u = MUri.toUri(uri);
        Map<String, String> q = u.getQuery();
        MProperties properties = new MProperties();
        if (q != null) properties.putAll(q);
        return start(u, null, properties);
    }

    public UUID start(String uri, IProperties properties) throws Exception {
        MUri u = MUri.toUri(uri);
        return start(u, null, properties);
    }

    public UUID start(MUri uri, UUID closeActivity, IProperties properties) throws Exception {
        if (!EngineConst.SCHEME_REACTIVE.equals(uri.getScheme()))
            throw new UsageException(
                    "unknown uri scheme", uri, "should be", EngineConst.SCHEME_REACTIVE);

        if (properties == null) {
            properties = new MProperties();
        }
        Map<String, String> query = uri.getQuery();
        if (query != null) properties.putAll(query);

        // get process
        EProcess process = getProcess(uri);

        if (process == null) throw new NotFoundException("process unknown", uri);

        // load pool
        EPool pool = getPool(process, uri);

        if (pool == null) throw new NotFoundException("pool not found in process", uri);

        // remember options
        String[] uriParams = uri.getParams();
        MProperties options = null;
        if (uriParams != null && uriParams.length > 0) {
            options = IProperties.explodeToMProperties(uriParams);
        } else {
            options = new MProperties();
        }

        // update uri
        MutableUri u = new MutableUri(uri.toString());
        u.setLocation(process.getCanonicalName() + ":" + process.getVersion());
        u.setPath(pool.getCanonicalName());
        u.setQuery(null);
        u.setParams(null);
        MUri originalUri = uri;
        uri = u;

        // load start points
        List<EElement> startPoints = null;
        String fragment = uri.getFragment();
        if (fragment != null) {
            EElement point = pool.getElement(fragment);
            if (point == null) throw new MException(RC.NOT_FOUND, "start point not found", fragment, uri);
            if (!point.is(AStartPoint.class))
                throw new MException(RC.CONFLICT, "node is not a start point", uri);
            startPoints = new LinkedList<>();
            startPoints.add(point);
        } else {
            startPoints = pool.getStartPoints(true);
            // remove inactive startpoints from list
            startPoints.removeIf(s -> s.isInterface(InactiveStartPoint.class));
        }

        if (startPoints.size() == 0) throw new NotFoundException("no start point found", uri);

        String createdBy = config.aaa.getCurrentUserId();

        // everything fine ... start creating
        fireEvent.startCase(
                originalUri, uri, properties, process, pool, startPoints, options, createdBy);

        // the context
        EngineContext context = new EngineContext(null, this);
        context.setUri(uri.toString());
        context.setEProcess(process);
        context.setEPool(pool);

        // ID could be defined in the options, must be a uuid and unique
        UUID id = null;
        Object uuid = options.get(EngineConst.OPTION_UUID);
        if (uuid != null) {
            id = UUID.fromString(uuid.toString());
            // check if exists
            try {
                if (storage.loadCase(id) != null)
                    throw new MException(RC.CONFLICT, "case already exists with uuid", id);
            } catch (NotFoundException e) {
                // everything is fine
            }
        } else id = UUID.randomUUID();

        if (closeActivity == null && options.isProperty(EngineConst.OPTION_CLOSE_ACTIVITY)) {
            closeActivity =
                    UUID.fromString(options.getString(EngineConst.OPTION_CLOSE_ACTIVITY, null));
        }

        // create the PCase
        PCase pCase =
                new PCase(
                        id,
                        options,
                        uri.toString(),
                        pool.getName(),
                        pool.getCanonicalName(),
                        System.currentTimeMillis(),
                        createdBy,
                        STATE_CASE.NEW,
                        0,
                        closeActivity,
                        properties,
                        EngineConst.MILESTONE_START);
        context.setPCase(pCase);

        // create the APool
        APool<?> aPool = createPoolObject(pool);
        context.setAPool(aPool);

        // life cycle case pool
        if (aPool instanceof ContextRecipient) ((ContextRecipient) aPool).setContext(context);
        aPool.initializeCase(properties);
        pCase.getParameters()
                .clear(); // cleanup before first save, will remove parameters from external input

        // create and add trace information

//        SpanBuilder spanBuilder =
//                ITracer.get()
//                        .createSpan(
//                                null,
//                                "bpm:" + pCase.getCanonicalName(),
//                                "uri",
//                                originalUri.toString(),
//                                "id",
//                                id.toString(),
//                                "properties",
//                                properties);
//        if (MTracing.current() != null)
//            spanBuilder.addReference(References.FOLLOWS_FROM, ITracer.get().current().context());
        try (IScope scope = MTracing.enter("xxx")) {
            // inject tracer
//            ITracer.get()
//                    .tracer()
//                    .inject(
//                            ITracer.get().current().context(),
//                            Format.Builtin.TEXT_MAP,
//                            new TextMap() {
//
//                                @Override
//                                public Iterator<Entry<String, String>> iterator() {
//                                    return null;
//                                }
//
//                                @Override
//                                public void put(String key, String value) {
//                                    pCase.getParameters().put("__tracer." + key, value);
//                                }
//                            });
            // create case lock
            try (PCaseLock lock = getCaseLock(pCase.getId(), null)) {
                lock.setPCase(pCase);
                lock.savePCase(aPool, true);

                // create start point flow nodes
                Throwable isError = null;
                for (EElement start : startPoints) {
                    try {
                        lock.createStartPoint(context, start, null);
                    } catch (Throwable t) {
                        log().w("create start point {1} failed", start, t);
                        fireEvent.error(pCase, start, t);
                        isError = t;
//XXX                        MTracing.current().finish();
                        break;
                    }
                }
                if (isError != null) {
                    storage.deleteCaseAndFlowNodes(pCase.getId());
                  //XXX                    ITracer.get().current().finish();
                    throw new Exception(isError);
                }

                pCase.setState(STATE_CASE.RUNNING);
                lock.savePCase(aPool, false);
            }
            statisticCaseStarted++;
        }
        return pCase.getId();
    }

    public EPool getPool(EProcess process, MUri uri) throws NotFoundException {
        String poolName = uri.getPath();
        if (MString.isEmpty(poolName)) {
            poolName = process.getProcessDescription().defaultPool().getCanonicalName();
            if (poolName.equals(NoPool.class.getCanonicalName())) poolName = null;
        }
        if (MString.isEmpty(poolName))
            throw new NotFoundException("default pool not found for process", uri);

        EPool pool = process.getPool(poolName);
        return pool;
    }

    public EProcess getProcess(MUri uri) throws MException {
        // load process
        String processName = uri.getLocation();
        String processVersion = null;
        if (MString.isIndex(processName, ':')) {
            processVersion = MString.afterIndex(processName, ':');
            processName = MString.beforeIndex(processName, ':');
        }
        if (processVersion == null)
            processVersion = config.persistent.getActiveProcessVersion(processName);
        else {
            if (!config.persistent.isProcessEnabled(processName, processVersion))
                throw new MException(RC.BUSY,
                        "specified process {1} version {2} is not enabled",
                        processName,
                        processVersion,
                        uri);
        }
        if (MString.isEmpty(processVersion))
            throw new MException(RC.BUSY, "default process {1} version is disabled", processName, uri);

        EProcess process = processProvider.getProcess(processName, processVersion);
        return process;
    }

    private long newScheduledTime(PNode flow) {
        return System.currentTimeMillis() + MPeriod.MINUTE_IN_MILLISECONDS;
    }

    public APool<?> createPoolObject(EPool pool) throws MException {
        return pool.newInstance();
    }

    public AProcess createProcessObject(EProcess process) throws MException {
        return process.newInstance();
    }

    public AElement<?> createActivityObject(EElement element) throws MException {
        return element.newInstance();
    }

    /**
     * Create a runtime node from P-Object.
     *
     * @param context
     * @param runtime
     * @return Runtime object
     */
    public RuntimeNode createRuntimeObject(EngineContext context, PNode runtime) {
        String canonicalName = runtime.getCanonicalName();
        RuntimeNode out = null;
        CaseLock lock = context.getCaseLock();
        //		System.out.println("### Lock: " + lock);
        if (lock != null) {
            UUID id = runtime.getId();
            if (id != null) {
                out = lock.getRuntime(id);
                if (out != null) return out;
            }
        }
        if (RuntimeNode.class.getCanonicalName().equals(canonicalName)
                || "de.mhus.cherry.reactive.model.engine.RuntimeNode"
                        .equals(canonicalName)) // legacy
        out = new RuntimeNode(runtime.getId());
        else out = (RuntimeNode) context.getEPool().getElement(canonicalName);
        if (out == null) // fallback
        out = new RuntimeNode(runtime.getId());
        // lifecycle
        if (out instanceof ContextRecipient) ((ContextRecipient) out).setContext(context);

        out.importParameters(runtime.getParameters());

        if (lock != null) {
            UUID id = runtime.getId();
            if (id != null) {
                lock.putRuntime(id, out);
            }
        }
        return out;
    }

    /**
     * Load the runtime P-Object from storage.
     *
     * @param context
     * @param pNode
     * @return The runtime persistent object
     */
    public PNode getRuntimeForPNode(EngineContext context, PNode pNode) {
        if (pNode == null || pNode.getRuntimeId() == null) return null;
        try {
            CaseLock lock = context.getCaseLock();
            if (lock != null) return lock.getFlowNode(pNode.getRuntimeId());
            return getNodeWithoutLock(pNode.getRuntimeId());
        } catch (NotFoundException | IOException e) {
            log().w("load runtime for node {1} failed", pNode, e);
            return null;
        }
    }

    /**
     * Return the object of the swim lane.
     *
     * @param context
     * @param eNode
     * @return The Swim lane object
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     */
    public ASwimlane<?> createSwimlaneObject(EngineContext context, EElement eNode)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException,
                    InvocationTargetException, NoSuchMethodException, SecurityException {
        ASwimlane<?> out = eNode.getSwimlane().getDeclaredConstructor().newInstance();
        // lifecycle
        if (out instanceof ContextRecipient) ((ContextRecipient) out).setContext(context);
        return out;
    }

    /** Archive all closed cases. */
    public void archiveAll() {
        try {
            config.persistent.save(archive);
            for (PCaseInfo caseId : storage.getCases(STATE_CASE.CLOSED)) {
                try (CaseLock lock = getCaseLock(caseId, "archiveAll")) {
                    PCase caze = lock.getCase();
                    fireEvent.archiveCase(caze);
                    try {
                        archive.saveCase(caze);
                        for (PNodeInfo nodeId : storage.getFlowNodes(caze.getId(), null)) {
                            PNode node = lock.getFlowNode(nodeId.getId());
                            archive.saveFlowNode(node);
                        }
                        storage.deleteCaseAndFlowNodes(caze.getId());
                    } catch (Throwable t) {
                        log().e("archive case {1} failed", caseId, t);
                    }
                }
            }
        } catch (Throwable t) {
            log().e(t);
        }
    }

    /**
     * This will archive the case and delete it in from storage.
     *
     * @param caseId
     * @throws IOException
     * @throws MException
     */
    public void archiveCase(UUID caseId) throws IOException, MException {
        try {
            PCaseInfo cazeInfo = getCaseInfo(caseId);
            if (cazeInfo != null) {
                try (CaseLock lock = getCaseLock(cazeInfo, "archiveCase")) {
                    PCase caze = lock.getCase();
                    fireEvent.archiveCase(caze);
                    if (caze.getState() != STATE_CASE.CLOSED)
                        throw new MException(RC.BUSY, "case {1} is not closed", caseId);
                    archive.saveCase(caze);

                    for (PNodeInfo nodeId : storage.getFlowNodes(caseId, null)) {
                        try {
                            PNode node = lock.getFlowNode(nodeId.getId());
                            archive.saveFlowNode(node);
                        } catch (NotFoundException e) {
                            log().d("archive case {1} failed", caseId, e);
                        }
                    }

                    storage.deleteCaseAndFlowNodes(caseId);
                    return;
                }
            }
        } catch (Exception e) {
            log().d("archive case {1} failed", caseId, e);
        }
        for (PNodeInfo nodeId : storage.getFlowNodes(caseId, null)) {
            try {
                PNode node = getNodeWithoutLock(nodeId.getId());
                archive.saveFlowNode(node);
            } catch (NotFoundException e) {
                log().d("archive case {1} failed", caseId, e);
            }
        }
        storage.deleteCaseAndFlowNodes(caseId);
    }

    /**
     * Set a case and all nodes to suspended state.
     *
     * @param caseId
     * @throws IOException
     * @throws MException
     */
    public void suspendCase(UUID caseId) throws IOException, MException {
        try (CaseLock lock = getCaseLock(caseId, "suspendCase")) {
            PCase caze = lock.getCase();
            if (caze.getState() == STATE_CASE.SUSPENDED)
                throw new MException(RC.BUSY, "case {1} already suspended", caseId);
            fireEvent.suspendCase(caze);
            caze.setState(STATE_CASE.SUSPENDED);
            storage.saveCase(caze);
            for (PNodeInfo nodeId : storage.getFlowNodes(caseId, null)) {
                PNode node = lock.getFlowNode(nodeId.getId());
                if (node.getState() != STATE_NODE.SUSPENDED
                        && node.getState() != STATE_NODE.CLOSED) {
                    node.setSuspendedState(node.getState());
                    node.setState(STATE_NODE.SUSPENDED);
                    storage.saveFlowNode(node);
                }
            }
        }
    }

    /**
     * Return a suspended case and all nodes to 'normal' state.
     *
     * @param caseId
     * @throws IOException
     * @throws MException
     */
    public void resumeCase(UUID caseId) throws IOException, MException {
        try (CaseLock lock = getCaseLock(caseId, "resumeCase")) {
            PCase caze = lock.getCase();
            if (caze.getState() != STATE_CASE.SUSPENDED)
                throw new MException(RC.CONFLICT, "case {1} is not suspended", caseId);
            fireEvent.unsuspendCase(caze);
            caze.setState(STATE_CASE.RUNNING);
            storage.saveCase(caze);
            for (PNodeInfo nodeId : storage.getFlowNodes(caseId, null)) {
                PNode node = lock.getFlowNode(nodeId.getId());
                if (node.getState() == STATE_NODE.SUSPENDED
                        && node.getSuspendedState() != STATE_NODE.NEW) {
                    node.setState(node.getSuspendedState());
                    node.setSuspendedState(STATE_NODE.NEW);
                    storage.saveFlowNode(node);
                }
            }
        }
    }

    /**
     * Set flow node to stopped. Not possible for suspended nodes.
     *
     * @param nodeId
     * @throws MException
     * @throws IOException
     */
    public void cancelFlowNode(UUID nodeId) throws MException, IOException {
        try (CaseLock lock = getCaseLockByNode(nodeId, "cancelFlowNode")) {
            PNode node = lock.getFlowNode(nodeId);
            if (node.getStartState() == STATE_NODE.SUSPENDED)
                throw new MException(RC.BUSY, "node {1} is suspended", nodeId);
            fireEvent.cancelFlowNode(node);
            node.setSuspendedState(node.getState());
            node.setState(STATE_NODE.CLOSED);
            storage.saveFlowNode(node);
        }
    }

    /**
     * Set a flow node to running state. Not possible for suspended nodes.
     *
     * @param nodeId
     * @throws MException
     * @throws IOException
     */
    public void retryFlowNode(UUID nodeId) throws MException, IOException {
        try (CaseLock lock = getCaseLockByNode(nodeId, "retryFlowNode")) {
            PNode node = lock.getFlowNode(nodeId);
            if (node.getStartState() == STATE_NODE.SUSPENDED)
                throw new MException(RC.BUSY, "node {1} is suspended", nodeId);
            fireEvent.retryFlowNode(node);
            node.setSuspendedState(node.getState());
            node.setState(STATE_NODE.RUNNING);
            node.setScheduledNow();
            storage.saveFlowNode(node);
        }
    }

    /**
     * Archive case and nodes.
     *
     * @param lock
     * @throws MException
     * @throws IOException
     */
    public void prepareMigrateCase(CaseLock lock) throws MException, IOException {
        PCase caze = lock.getCase();
        if (caze.getState() != STATE_CASE.SUSPENDED && caze.getState() != STATE_CASE.CLOSED)
            throw new MException(RC.CONFLICT, "case {1} is not suspended", caze.getId());

        // load all nodes
        LinkedList<PNode> nodes = new LinkedList<>();
        for (PNodeInfo nodeId : storage.getFlowNodes(caze.getId(), null)) {
            PNode node = lock.getFlowNode(nodeId.getId());
            nodes.add(node);
        }

        // archive the case state
        archive.saveCase(caze);
        for (PNode node : nodes) archive.saveFlowNode(node);
    }

    /**
     * Copy the case from the archive to the storage. Only suspended, closed or deleted cases can be
     * restored. The restored case will be in suspended state.
     *
     * @param caseId
     * @throws IOException
     * @throws MException
     */
    public void restoreCase(UUID caseId) throws IOException, MException {

        try (PCaseLock lock = getCaseLock(caseId, "restoreCase.1")) {
            PCase caze = lock.getCase();
            if (caze != null) {
                fireEvent.archiveCase(caze);
                if (caze.getState() != STATE_CASE.CLOSED && caze.getState() != STATE_CASE.SUSPENDED)
                    throw new MException(RC.BUSY, "case {1} is not closed or suspended", caseId);
            }
        } catch (NotFoundException e) {
            log().d("restor case {1} failed", caseId, e);
        }
        PCase caze = archive.loadCase(caseId);
        fireEvent.restoreCase(caze);
        try (PCaseLock lock = getCaseLock(caseId, "restoreCase.2")) {
            caze.setState(STATE_CASE.SUSPENDED);
            storage.saveCase(caze);
            lock.resetPCase(); // reload
            caze = lock.getCase(); // reload
            for (PNodeInfo nodeId : archive.getFlowNodes(caseId, null)) {
                PNode node = lock.getFlowNode(nodeId.getId());
                // set to suspended
                if (node.getState() != STATE_NODE.CLOSED
                        && node.getState() != STATE_NODE.SUSPENDED) {
                    node.setSuspendedState(node.getState());
                    node.setState(STATE_NODE.SUSPENDED);
                }
                storage.saveFlowNode(node);
            }
        }
    }

    // -- aaa provider

    public AaaProvider getAaaProvider() {
        return config.aaa;
    }

    // -- storage

    @Override
    public Result<PCaseInfo> storageSearchCases(SearchCriterias criterias) throws IOException {
        return storage.searchCases(criterias);
    }

    @Override
    public Result<PCaseInfo> storageGetCases(STATE_CASE state) throws IOException {
        return storage.getCases(state);
    }

    @Override
    public Result<PNodeInfo> storageGetFlowNodes(UUID caseId, STATE_NODE state) throws IOException {
        return storage.getFlowNodes(caseId, state);
    }

    @Override
    public Result<PNodeInfo> storageSearchFlowNodes(SearchCriterias criterias) throws IOException {
        return storage.searchFlowNodes(criterias);
    }

    @Override
    public Result<PNodeInfo> storageGetScheduledFlowNodes(STATE_NODE state, long scheduled)
            throws IOException {
        return storage.getScheduledFlowNodes(state, scheduled, false);
    }

    @Override
    public Result<PNodeInfo> storageGetSignaledFlowNodes(STATE_NODE state, String signal)
            throws IOException {
        return storage.getSignalFlowNodes(state, signal);
    }

    @Override
    public Result<PNodeInfo> storageGetMessageFlowNodes(
            UUID caseId, STATE_NODE state, String message) throws IOException {
        return storage.getMessageFlowNodes(caseId, state, message);
    }

    public void storageDeleteCaseAndFlowNodes(UUID caseId) throws IOException {
        storage.deleteCaseAndFlowNodes(caseId);
    }

    public void storageDeleteFlowNode(UUID nodeId) throws IOException {
        storage.deleteFlowNode(nodeId);
    }

    // -- archive

    public Result<PCaseInfo> archiveSearchCases(SearchCriterias criterias) throws IOException {
        return archive.searchCases(criterias);
    }

    public Result<PNodeInfo> archiveSearchFlowNodes(SearchCriterias criterias) throws IOException {
        return archive.searchFlowNodes(criterias);
    }

    public PCase archiveLoadCase(UUID id) throws IOException, NotFoundException {
        return archive.loadCase(id);
    }

    public PNode archiveLoadFlowNode(UUID id) throws IOException, NotFoundException {
        return archive.loadFlowNode(id);
    }

    public Result<PCaseInfo> archiveGetCases(STATE_CASE state) throws IOException {
        return archive.getCases(state);
    }

    public Result<PNodeInfo> archiveGetFlowNodes(UUID caseId, STATE_NODE state) throws IOException {
        return archive.getFlowNodes(caseId, state);
    }

    public Result<PNodeInfo> archiveGetScheduledFlowNodes(STATE_NODE state, long scheduled)
            throws IOException {
        return archive.getScheduledFlowNodes(state, scheduled, false);
    }

    public Result<PNodeInfo> archiveGetSignaledFlowNodes(STATE_NODE state, String signal)
            throws IOException {
        return archive.getSignalFlowNodes(state, signal);
    }

    public Result<PNodeInfo> archiveGetMessageFlowNodes(
            UUID caseId, STATE_NODE state, String message) throws IOException {
        return archive.getMessageFlowNodes(caseId, state, message);
    }

    // --

    public EngineContext createContext(CaseLock lock) throws MException, IOException {

        PCase caze = lock.getCase();
        MUri uri = MUri.toUri(caze.getUri());
        EProcess process = getProcess(uri);
        EPool pool = getPool(process, uri);

        EngineContext context = new EngineContext(lock, this);
        context.setUri(uri.toString());
        context.setEProcess(process);
        context.setEPool(pool);
        context.setPCase(caze);
        return context;
    }

    public EngineContext createContext(PCase caze) throws MException, IOException {

        MUri uri = MUri.toUri(caze.getUri());
        EProcess process = getProcess(uri);
        EPool pool = getPool(process, uri);

        EngineContext context = new EngineContext(null, this);
        context.setUri(uri.toString());
        context.setEProcess(process);
        context.setEPool(pool);
        context.setPCase(caze);
        return context;
    }

    public EngineContext createContext(CaseLock lock, PCase caze, PNode pNode) throws MException {

        MUri uri = MUri.toUri(caze.getUri());
        EProcess process = getProcess(uri);
        EPool pool = getPool(process, uri);

        EngineContext context = new EngineContext(lock, this, pNode);
        context.setUri(uri.toString());
        context.setEProcess(process);
        context.setEPool(pool);
        context.setPCase(caze);
        return context;
    }

    public void doCloseActivity(ProcessContext<?> closedContext, UUID nodeId)
            throws IOException, MException {
        try (PCaseLock lock = getCaseLockByNode(nodeId, "doCloseActivity")) {

            PNode node = lock.getFlowNode(nodeId);
            PCase caze = lock.getCase();

            if (node.getState() != STATE_NODE.WAITING && node.getState() != STATE_NODE.SCHEDULED) {
                closedContext.getARuntime().doErrorMsg(node, "call back node is no more running");
                return;
            }

            if (caze.getState() != STATE_CASE.RUNNING) {
                closedContext.getARuntime().doErrorMsg(node, "call back case is no more running");
                return;
            }

            EngineContext context = createContext(lock, caze, node);
            AElement<?> aNode = context.getANode();
            try {
                ((CloseActivity) aNode).doClose(closedContext);
            } catch (Exception e) {
                lock.doNodeErrorHandling(context, node, e);
                log().e("doCloseActivity", nodeId, e);
            }
            lock.saveFlowNode(node);
            lock.savePCase(context);
        }
    }

    public void saveEnginePersistence() {
        try {
            config.persistent.save();
        } catch (IOException e) {
            log().e(e);
        }
    }

    public void loadEnginePersistence() {
        try {
            config.persistent = new PEngine(storage);
        } catch (IOException e) {
            log().e(e);
        }
    }

    public boolean hasReadAccess(String uri, String user) {
        if (!config.aaa.isUserActive(user)) return false;
        MUri muri = MUri.toUri(uri);
        try {
            EProcess process = getProcess(muri);
            if (process == null) {
                log().d("hasReadAccess: Process not found", uri);
                return false;
            }
            EPool pool = getPool(process, muri);
            if (pool == null) {
                log().d("hasReadAccess: Pool not found", uri);
                return false;
            }
            EngineContext context = new EngineContext(null, this);
            context.setUri(uri);
            context.setEProcess(process);
            context.setEPool(pool);

            {
                Class<? extends AActor>[] actorClasss = pool.getPoolDescription().actorRead();
                for (Class<? extends AActor> actorClass : actorClasss) {
                    AActor actor = actorClass.getDeclaredConstructor().newInstance();
                    if (actor instanceof ContextRecipient)
                        ((ContextRecipient) actor).setContext(context);
                    boolean hasAccess = actor.hasAccess(user);
                    if (hasAccess) return true;
                }
            }
            {
                Class<? extends AActor>[] actorClasss = pool.getPoolDescription().actorWrite();
                for (Class<? extends AActor> actorClass : actorClasss) {
                    AActor actor = actorClass.getDeclaredConstructor().newInstance();
                    if (actor instanceof ContextRecipient)
                        ((ContextRecipient) actor).setContext(context);
                    boolean hasAccess = actor.hasAccess(user);
                    if (hasAccess) return true;
                }
            }

            log().d("hasReadAccess: Access not found", uri);
            return false;

        } catch (Throwable t) {
            log().e(uri, user, t);
            return false;
        }
    }

    public boolean hasWriteAccess(String uri, String user) {
        if (!config.aaa.isUserActive(user)) return false;

        MUri muri = MUri.toUri(uri);
        try {
            EProcess process = getProcess(muri);
            if (process == null) {
                log().d("hasWriteAccess: Process not found", uri);
                return false;
            }
            EPool pool = getPool(process, muri);
            if (pool == null) {
                log().d("hasWriteAccess: Pool not found", uri);
                return false;
            }
            EngineContext context = new EngineContext(null, this);
            context.setUri(uri);
            context.setEProcess(process);
            context.setEPool(pool);

            {
                Class<? extends AActor>[] actorClasss = pool.getPoolDescription().actorWrite();
                for (Class<? extends AActor> actorClass : actorClasss) {
                    AActor actor = actorClass.getDeclaredConstructor().newInstance();
                    if (actor instanceof ContextRecipient)
                        ((ContextRecipient) actor).setContext(context);
                    boolean hasAccess = actor.hasAccess(user);
                    if (hasAccess) return true;
                }
            }

            log().d("hasWriteAccess: Access not found", uri);
            return false;

        } catch (Throwable t) {
            log().e(uri, user, t);
            return false;
        }
    }

    public boolean hasInitiateAccess(MUri uri, String user) {
        if (!config.aaa.isUserActive(user)) return false;
        try {

            EProcess process = getProcess(uri);
            if (process == null) {
                log().d("hasInitiateAccess: Process not found", uri);
                return false;
            }
            EPool pool = getPool(process, uri);
            if (pool == null) {
                log().d("hasInitiateAccess: Pool not found", uri);
                return false;
            }
            EngineContext context = new EngineContext(null, this);
            context.setUri(uri.toString());
            context.setEProcess(process);
            context.setEPool(pool);
            Class<? extends AActor>[] actorClasss = pool.getPoolDescription().actorInitiator();
            for (Class<? extends AActor> actorClass : actorClasss) {
                AActor actor = actorClass.getDeclaredConstructor().newInstance();
                if (actor instanceof ContextRecipient)
                    ((ContextRecipient) actor).setContext(context);
                boolean hasAccess = actor.hasAccess(user);
                if (hasAccess) return true;
            }
            log().d("hasInitiateAccess: Access not found", uri);
            return false;

        } catch (Throwable t) {
            log().e("check initial access to {1} for user {2} failed", uri, user, t);
            return false;
        }
    }

    public boolean hasExecuteAccess(UUID nodeId, String user) {
        if (!config.aaa.isUserActive(user)) return false;
        if (config.aaa.hasAdminAccess(user)) return true;

        try {
            // find actor
            PNode node = getNodeWithoutLock(nodeId);
            PCase caze = getCaseWithoutLock(node.getCaseId());
            String uri = caze.getUri();

            MUri muri = MUri.toUri(uri);
            EProcess process = getProcess(muri);
            if (process == null) {
                log().d("hasExecuteAccess: Process not found", uri);
                return false;
            }
            EPool pool = getPool(process, muri);
            if (pool == null) {
                log().d("hasExecuteAccess: Pool not found", uri);
                return false;
            }
            EngineContext context = new EngineContext(null, this, node);
            context.setUri(uri);
            context.setEProcess(process);
            context.setEPool(pool);
            context.setPCase(caze);
            EElement eNode = context.getENode();
            Class<? extends AActor> actorClass = eNode.getAssignedActor(pool);

            // create actor and let check access
            AActor actor = actorClass.getDeclaredConstructor().newInstance();
            if (actor instanceof ContextRecipient) ((ContextRecipient) actor).setContext(context);
            boolean hasAccess = actor.hasAccess(user);

            return hasAccess;

        } catch (Throwable t) {
            log().e("check execute access for node {1} of user {2} failed", nodeId, user, t);
            return false;
        }
    }

    public void fireExternal(UUID nodeId, String taskName, Map<String, Object> parameters)
            throws IOException, MException {
        try (PCaseLock lock =
                getCaseLockByNode(
                        nodeId, "fireExternal", "taskName", taskName, "parameters", parameters)) {
            fireEvent.fireExternal(nodeId, taskName, parameters);
            PNode node = lock.getFlowNode(nodeId);
            if (taskName != null && !node.getName().equals(taskName)) {
                fireEvent.error("Wrong task name", taskName, nodeId);
                throw new NotFoundException("Wrong task name");
            }

            // check parameters
            if (node.getState() == STATE_NODE.WAITING
                    || node.getState() == STATE_NODE.SUSPENDED
                            && node.getSuspendedState() == STATE_NODE.WAITING) {
                try {
                    PCase caze = lock.getCase();
                    EngineContext context = createContext(lock, caze, node);
                    AActivity<?> aNode = context.getANode();
                    if (aNode instanceof ValidateParametersBeforeExecute)
                        ((ValidateParametersBeforeExecute) aNode).validateParameters(parameters);
                } catch (MException t) {
                    throw t;
                } catch (MRuntimeException t) {
                    throw t;
                } catch (Throwable t) {
                    throw new IOException(t);
                }
            }

            // Set into running mode
            if (node.getType() != TYPE_NODE.EXTERN)
                throw new NotFoundException("not external", nodeId);

            if (node.getState() == STATE_NODE.SUSPENDED) {
                if (node.getSuspendedState() != STATE_NODE.WAITING)
                    throw new NotFoundException("not waiting", nodeId);
                node.setSuspendedState(STATE_NODE.RUNNING);
            } else {
                if (node.getState() != STATE_NODE.WAITING)
                    throw new NotFoundException("not waiting", nodeId);
                node.setState(STATE_NODE.RUNNING);
                node.setScheduledNow();
            }
            node.setMessage(parameters);
            lock.saveFlowNode(node);
        }
    }

    public void fireMessage(UUID caseId, String message, Map<String, Object> parameters)
            throws Exception {

        fireEvent.fireMessage(caseId, message, parameters);
        Result<PNodeInfo> res =
                storage.getMessageFlowNodes(caseId, PNode.STATE_NODE.WAITING, message);
        for (PNodeInfo nodeInfo : res) {
            try (PCaseLock lock =
                    getCaseLock(
                            nodeInfo,
                            "fireMessage",
                            "message",
                            message,
                            "parameters",
                            parameters)) {
                PNode node = lock.getFlowNode(nodeInfo.getId());

                // check parameters
                if (node.getState() == STATE_NODE.WAITING) {
                    try {
                        PCase caze = lock.getCase();
                        EngineContext context = createContext(lock, caze, node);
                        PNode runtime = getRuntimeForPNode(context, node);
                        context.setPRuntime(runtime);
                        if (caze instanceof ContextRecipient)
                            ((ContextRecipient) caze).setContext(context);
                        AActivity<?> aNode = context.getANode();
                        if (aNode instanceof ValidateParametersBeforeExecute)
                            ((ValidateParametersBeforeExecute) aNode)
                                    .validateParameters(parameters);
                    } catch (ValidationException | UsageException t) {
                        log().d("validation failed for node {1}", node, t);
                        continue;
                    } catch (Throwable t) {
                        log().w("fire message for node {1} failed", node, t);
                        continue;
                    }
                }

                if (node.getState() == STATE_NODE.SUSPENDED) {
                    log().w("message for suspended node {1} will not be delivered", node, message);
                    continue;
                } else if (isExecuting(nodeInfo.getId())) {
                    // to late ...
                    continue;
                }

                // is task listening ? check trigger list for ""
                String taskEvent = node.getMessageTriggers().get("");
                if (taskEvent != null
                        && taskEvent.equals(message)
                        && node.getState() == STATE_NODE.WAITING
                        && node.getType() == TYPE_NODE.MESSAGE) {
                    node.setState(STATE_NODE.RUNNING);
                    node.setScheduledNow();
                    node.setMessage(parameters);
                    lock.saveFlowNode(node);
                    res.close();
                    // message delivered ... bye
                    return;
                }

                try {
                    // find a trigger with the event
                    PCase caze = lock.getCase();
                    EngineContext context = createContext(lock, caze, node);
                    for (Trigger trigger :
                            ActivityUtil.getTriggers((AActivity<?>) context.getANode())) {
                        if (trigger.type() == TYPE.MESSAGE && trigger.event().equals(message)) {
                            // found one ... start new, close current
                            PNode nextNode =
                                    lock.createActivity(
                                            context,
                                            node,
                                            context.getEPool()
                                                    .getElement(
                                                            trigger.activity().getCanonicalName()));
                            nextNode.setMessage(parameters);
                            lock.saveFlowNode(context, nextNode, null);
                            if (trigger.abort())
                                lock.closeFlowNode(context, node, STATE_NODE.CLOSED);
                            res.close();
                            return;
                        }
                    }
                } catch (Throwable e) {
                    fireEvent.error(node, e);
                    log().e("fire failed for node {1}", node, e);
                    // should not happen, it's an internal engine problem
                    try {
                        PCase caze = lock.getCase();
                        EngineContext context = createContext(lock, caze, node);
                        lock.closeFlowNode(context, node, STATE_NODE.SEVERE);
                    } catch (Throwable e2) {
                        log().e("close node {1} failed", nodeInfo, e2);
                        fireEvent.error(nodeInfo, e2);
                    }
                    continue;
                }
            }
        }
        throw new NotFoundException("node not found for message", caseId, message);
    }

    public int fireSignal(UUID caseId, String signal, Map<String, Object> parameters)
            throws NotFoundException, IOException {

        fireEvent.fireSignal(signal, parameters);
        int cnt = 0;
        for (PNodeInfo nodeInfo : storage.getSignalFlowNodes(PNode.STATE_NODE.WAITING, signal)) {
            if (caseId != null && !nodeInfo.getCaseId().equals(caseId)) continue;
            try (PCaseLock lock =
                    getCaseLock(
                            nodeInfo,
                            "fireSignal",
                            "case",
                            caseId,
                            "signal",
                            signal,
                            "parameters",
                            parameters)) {
                PNode node = lock.getFlowNode(nodeInfo.getId());
                if (node.getState()
                        == STATE_NODE
                                .SUSPENDED) { // should not happen ... searching for WAITING nodes
                    log().w("signal for suspended node will not be delivered", node, signal);
                    continue;
                } else if (isExecuting(nodeInfo.getId())) {
                    // to late ...
                    continue;
                }
                // is task listening ? check trigger list for ""
                String taskEvent = node.getSignalTriggers().get("");
                if (taskEvent != null
                        && taskEvent.equals(signal)
                        && node.getState() == STATE_NODE.WAITING
                        && node.getType() == TYPE_NODE.SIGNAL) {
                    // trigger not found - its the message
                    node.setState(STATE_NODE.RUNNING);
                    node.setScheduledNow();
                    node.setMessage(parameters);
                    lock.saveFlowNode(node);
                    cnt++;
                } else {
                    try {
                        // find a trigger with the name
                        PCase caze = lock.getCase();
                        EngineContext context = createContext(lock, caze, node);
                        for (Trigger trigger :
                                ActivityUtil.getTriggers((AActivity<?>) context.getANode())) {
                            if (trigger.type() == TYPE.SIGNAL && trigger.event().equals(signal)) {
                                // found one ... start new, close current
                                PNode nextNode =
                                        lock.createActivity(
                                                context,
                                                node,
                                                context.getEPool()
                                                        .getElement(
                                                                trigger.activity()
                                                                        .getCanonicalName()));
                                nextNode.setMessage(parameters);
                                lock.saveFlowNode(context, nextNode, null);
                                if (trigger.abort())
                                    lock.closeFlowNode(context, node, STATE_NODE.CLOSED);
                                cnt++;
                                continue;
                            }
                        }
                    } catch (MException e) {
                        fireEvent.error(node, e);
                        log().e("fire signal for node {1} failed", node, e);
                        // should not happen, it's an internal engine problem
                        try {
                            PCase caze = lock.getCase();
                            EngineContext context = createContext(lock, caze, node);
                            lock.closeFlowNode(context, node, STATE_NODE.SEVERE);
                        } catch (Throwable e2) {
                            log().e("close node {1} failed", nodeInfo, e2);
                            fireEvent.error(nodeInfo, e2);
                        }
                        continue;
                    }
                }

            } catch (Throwable t) {
                log().d("fire signal for node {1} failed", nodeInfo.getId(), t);
                // should not happen, it's an internal engine problem
                try (PCaseLock lock = getCaseLock(nodeInfo, "fireSignal.error", "error", t)) {
                    PCase caze = lock.getCase();
                    PNode node = lock.getFlowNode(nodeInfo.getId());
                    EngineContext context = createContext(lock, caze, node);
                    lock.closeFlowNode(context, node, STATE_NODE.SEVERE);
                } catch (Throwable e) {
                    log().e("close node {1} failed", nodeInfo, e);
                    fireEvent.error(nodeInfo, e);
                }
            }
        }
        return cnt;
    }

    private void fireScheduledTrigger(PCaseLock lock, PNodeInfo nodeInfo) {
        if (nodeInfo.getState() != STATE_NODE.WAITING) return;
        try {
            PNode node = lock.getFlowNode(nodeInfo.getId());
            if (isExecuting(nodeInfo.getId())) {
                // to late ...
                return;
            }
            Entry<String, Long> entry = node.getNextScheduled();
            if (entry == null) {
                // There is no need to be scheduled ....
                node.setScheduled(EngineConst.END_OF_DAYS);
                fireEvent.setScheduledToWaiting(node);
                lock.saveFlowNode(node);
                return;
            }
            String triggerName = entry.getKey();
            if (triggerName.equals("")) {
                // for secure
                node.setScheduled(-1);
                lock.saveFlowNode(node);
                return;
            }
            if (entry.getValue() > System.currentTimeMillis()) {
                // not reached ...
                node.setScheduled(entry.getValue());
                lock.saveFlowNode(node);
                return;
            }
            // find trigger
            PCase caze = lock.getCase();
            EngineContext context = createContext(lock, caze, node);
            int cnt = 0;
            for (Trigger trigger : ActivityUtil.getTriggers((AActivity<?>) context.getANode())) {
                if (trigger.type() == TYPE.TIMER
                        && (trigger.name().equals(triggerName)
                                || triggerName.startsWith("trigger.")
                                        && cnt == MCast.toint(triggerName.substring(8), -1))) {
                    // found one ... start new, close current
                    PNode nextNode =
                            lock.createActivity(
                                    context,
                                    node,
                                    context.getEPool()
                                            .getElement(trigger.activity().getCanonicalName()));
                    lock.saveFlowNode(context, nextNode, null);
                    if (trigger.abort()) lock.closeFlowNode(context, node, STATE_NODE.CLOSED);
                    return;
                }
                cnt++;
            }
            // trigger not found
            fireEvent.error("Trigger for timer not found", triggerName, node);
            node.setSuspendedState(node.getState());
            node.setState(STATE_NODE.STOPPED);
            lock.saveFlowNode(node);

        } catch (Throwable t) {
            log().e("fire scheduled trigger for node {1} failed", nodeInfo.getId(), t);
            // should not happen, it's an internal engine problem
            try {
                PNode node = lock.getFlowNode(nodeInfo.getId());
                PCase caze = lock.getCase();
                EngineContext context = createContext(lock, caze, node);
                lock.closeFlowNode(context, node, STATE_NODE.SEVERE);
            } catch (Throwable e) {
                log().e("close node {1} failed", nodeInfo, e);
                fireEvent.error(nodeInfo, e);
            }
        }
    }

    public boolean isExecuting(UUID nodeId) {
        synchronized (executing) {
            return executing.contains(nodeId);
        }
    }

    public void assignUserTask(UUID nodeId, String user) throws IOException, MException {
        try (PCaseLock lock = getCaseLockByNode(nodeId, "assignUserTask", "user", user)) {
            PNode node = lock.getFlowNode(nodeId);
            // PCase caze = lock.getCase();
            if (node.getState() != STATE_NODE.WAITING)
                throw new MException(RC.CONFLICT, "node {1} is not WAITING", nodeId);
            if (node.getType() != TYPE_NODE.USER)
                throw new MException(RC.CONFLICT, "node {1} is not a user task", nodeId);
            if (node.getAssignedUser() != null)
                throw new MException(RC.BUSY, "node {1} is already assigned to {2}", nodeId, node.getAssignedUser());
            node.setAssignedUser(user);
            storage.saveFlowNode(node);
        }
    }

    public void unassignUserTask(UUID nodeId) throws IOException, MException {
        try (PCaseLock lock = getCaseLockByNode(nodeId, "unassignUserTask")) {
            PNode node = lock.getFlowNode(nodeId);
            if (node.getState() != STATE_NODE.WAITING)
                throw new MException(RC.CONFLICT, "node {1} is not WAITING", nodeId);
            if (node.getType() != TYPE_NODE.USER)
                throw new MException(RC.CONFLICT, "node {1} is not a user task", nodeId);
            if (node.getAssignedUser() == null)
                throw new MException(RC.BUSY, "node {1} is not assigned", nodeId);
            node.setAssignedUser(null);
            storage.saveFlowNode(node);
        }
    }

    public void submitUserTask(UUID nodeId, IProperties values) throws IOException, MException {
        try (PCaseLock lock = getCaseLockByNode(nodeId, "submitUserTask", "values", values)) {
            PNode node = lock.getFlowNode(nodeId);
            PCase caze = lock.getCase();
            if (node.getState() != STATE_NODE.WAITING)
                throw new MException(RC.CONFLICT, "node {1} is not WAITING", nodeId);
            if (node.getType() != TYPE_NODE.USER)
                throw new MException(RC.CONFLICT, "node {1} is not a user task", nodeId);
            if (node.getAssignedUser() == null)
                throw new MException(RC.BUSY, "node {1} is not assigned", nodeId);

            EngineContext context = createContext(lock, caze, node);
            AElement<?> aNode = context.getANode();
            if (!(aNode instanceof AUserTask<?>))
                throw new MException(RC.CONFLICT, 
                        "node {1} activity is not a user task",
                        nodeId,
                        aNode.getClass().getCanonicalName());

            ((AUserTask<?>) aNode).doSubmit(values);

            node.setState(STATE_NODE.RUNNING);
            node.setScheduledNow();
            lock.saveFlowNode(context, node, (AActivity<?>) aNode);
        }
    }

    public MProperties onUserTaskAction(UUID nodeId, String action, IProperties values)
            throws IOException, MException {
        try (PCaseLock lock =
                getCaseLockByNode(nodeId, "onUserTaskAction", "action", action, "values", values)) {
            PNode node = lock.getFlowNode(nodeId);
            PCase caze = lock.getCase();
            if (node.getState() != STATE_NODE.WAITING)
                throw new MException(RC.CONFLICT, "node {1} is not WAITING", nodeId);
            if (node.getType() != TYPE_NODE.USER)
                throw new MException(RC.CONFLICT, "node {1} is not a user task", nodeId);
            if (node.getAssignedUser() == null)
                throw new MException(RC.BUSY, "node {1} is not assigned", nodeId);

            EngineContext context = createContext(lock, caze, node);
            AElement<?> aNode = context.getANode();
            if (!(aNode instanceof AUserTask<?>))
                throw new MException(RC.CONFLICT, 
                        "node {1} activity is not a user task",
                        nodeId,
                        aNode.getClass().getCanonicalName());

            MProperties ret = ((AUserTask<?>) aNode).doAction(action, values);

            lock.saveFlowNode(context, node, (AActivity<?>) aNode);

            return ret;
        }
    }

    /**
     * Execute a pool / case action on the running case. You need to save manually if needed:
     * lock.savePCase(context);
     *
     * @param caseId
     * @param values
     * @param action
     * @return The result of the action or null
     * @throws IOException
     * @throws MException
     */
    public MProperties onUserCaseAction(UUID caseId, String action, IProperties values)
            throws IOException, MException {
        try (PCaseLock lock =
                getCaseLock(caseId, "onUserCaseAction", "action", action, "values", values)) {
            PCase caze = lock.getCase();
            if (caze.getState() != STATE_CASE.RUNNING)
                throw new MException(RC.BUSY, "case {1} is not RUNNING", caseId);

            EngineContext context = createContext(lock, caze, null);
            APool<?> aPool = context.getPool();
            MProperties ret = aPool.onUserCaseAction(action, values);
            //            lock.savePCase(context); // do not save by default
            return ret;
        }
    }

    public AElement<?> getANode(UUID nodeId) throws IOException, MException {
        PNode node = getNodeWithoutLock(nodeId);
        PCase caze = getCaseWithoutLock(node.getCaseId());
        EngineContext context = createContext(null, caze, node);
        AElement<?> aNode = context.getANode();
        return aNode;
    }

    public PNodeInfo getFlowNodeInfo(UUID nodeId) throws Exception {
        //		synchronized (nodeCache) {
        //			PNode node = nodeCache.get(nodeId);
        //			if (node != null) {
        //				PCaseInfo caseInfo = getCaseInfo(node.getCaseId());
        //				return new PNodeInfo(caseInfo, node);
        //			}
        //		}
        return storage.loadFlowNodeInfo(nodeId);
    }

    public PNodeInfo storageGetFlowNodeInfo(UUID nodeId) throws Exception {
        return storage.loadFlowNodeInfo(nodeId);
    }

    public PCaseInfo getCaseInfo(UUID caseId) throws Exception {
        //		synchronized (caseCache) {
        //			PCase caze = caseCache.get(caseId);
        //			if (caze != null)
        //				return new PCaseInfo(caze);
        //		}
        return storage.loadCaseInfo(caseId);
    }

    public PCaseInfo storageGetCaseInfo(UUID caseId) throws Exception {
        return storage.loadCaseInfo(caseId);
    }

    public boolean isCaseLock(UUID caseId) {
        return lockProvider.isCaseLocked(caseId);
    }

    @Override
    public PCaseLock getCaseLockByNode(UUID nodeId, String operation, Object... tagPairs)
            throws MException {
        try {
            PNodeInfo nodeInfo = getFlowNodeInfo(nodeId);
            return getCaseLock(nodeInfo.getCaseId(), operation, tagPairs);
        } catch (MException e) {
            throw e;
        } catch (Exception e) {
            throw new MException(RC.STATUS.ERROR, nodeId, e);
        }
    }

    @Override
    public PCaseLock getCaseLock(PNodeInfo nodeInfo, String operation, Object... tagPairs)
            throws TimeoutException {
        return getCaseLock(nodeInfo.getCaseId(), operation, tagPairs);
    }

    @Override
    public PCaseLock getCaseLock(PCaseInfo caseInfo, String operation, Object... tagPairs)
            throws TimeoutException {
        return getCaseLock(caseInfo.getId(), operation, tagPairs);
    }

    @Override
    public PCaseLock getCaseLock(PNode node, String operation, Object... tagPairs)
            throws TimeoutException {
        return getCaseLock(node.getCaseId(), operation, tagPairs);
    }

    @Override
    public PCaseLock getCaseLockOrNull(PNodeInfo nodeInfo, String operation, Object... tagPairs) {
        return getCaseLockOrNull(nodeInfo.getCaseId(), operation, tagPairs);
    }

    @Override
    public PCaseLock getCaseLockOrNull(UUID caseId, String operation, Object... tagPairs) {
        synchronized (caseLocks) {
            EngineCaseLock lock = caseLocks.get(caseId);
            if (lock != null) {
                if (lock.owner == Thread.currentThread())
                    return new CaseLockProxy(lock, fireEvent, operation, tagPairs);
            }
        }
        Lock systemLock = lockProvider.lockOrNull(caseId);
        if (systemLock == null) return null;
        EngineCaseLock lock = new EngineCaseLock(caseId, systemLock, operation, tagPairs);
        synchronized (caseLocks) {
            caseLocks.put(caseId, lock);
        }
        return lock;
    }

    @Override
    public PCaseLock getCaseLock(UUID caseId, String operation, Object... tagPairs)
            throws TimeoutException {
        synchronized (caseLocks) {
            EngineCaseLock lock = caseLocks.get(caseId);
            if (lock != null) {
                if (lock.owner == Thread.currentThread())
                    return new CaseLockProxy(lock, fireEvent, operation, tagPairs);
            }
        }
        Lock systemLock = lockProvider.lock(caseId);
        if (systemLock == null) throw new TimeoutException("lock is null", caseId);
        EngineCaseLock lock = new EngineCaseLock(caseId, systemLock, operation, tagPairs);
        synchronized (caseLocks) {
            caseLocks.put(caseId, lock);
        }
        return lock;
    }

    /*
     * Methods from InternalEngine
     */

    @Override
    public RuntimeNode doExecuteStartPoint(ProcessContext<?> context, EElement eMyStartPoint)
            throws Exception {
        EngineContext eContext = (EngineContext) context;
        try (PCaseLock lock =
                getCaseLock(
                        context.getPCase().getId(),
                        "doExecuteStartPoint",
                        "point",
                        eMyStartPoint)) {
            UUID flowId = lock.createStartPoint(eContext, eMyStartPoint, null);
            PNode pNode = lock.getFlowNode(flowId);
            PCase caze = lock.getCase();
            EngineContext newContext = createContext(lock, caze, pNode);
            RuntimeNode runtime = newContext.getARuntime();
            return runtime;
        }
    }

    public void storageUpdateFull(PCase caze) throws IOException {
        storage.updateFullCase(caze);
    }

    public void storageUpdateFull(PNode node) throws IOException {
        storage.updateFullFlowNode(node);
    }

    @Override
    public PCase getCaseWithoutLock(UUID caseId) throws NotFoundException, IOException {
        return storage.loadCase(caseId);
    }

    @Override
    public PNode getNodeWithoutLock(UUID nodeId) throws NotFoundException, IOException {
        return storage.loadFlowNode(nodeId);
    }

    public List<EngineCaseLock> getCaseLocks() {
        return new LinkedList<>(caseLocks.values());
    }

    public class EngineCaseLock extends CaseLockImpl
            implements PCaseLock {

        public Thread owner;
        private UUID caseId;
        private PCase cazex;
        private HashMap<UUID, PNode> nodeCache = new HashMap<>();
        private HashMap<UUID, RuntimeNode> runtimeCache = new HashMap<>();
        private Lock lock;
        private String stacktrace;

        EngineCaseLock(UUID caseId, Lock lock, String operation, Object... tagPairs) {
            super(true, operation, tagPairs);
            owner = Thread.currentThread();
            fireEvent.lock(this, caseId);
            this.lock = lock;
            this.caseId = caseId;
            stacktrace =
                    MCast.toString(
                            "Lock " + caseId + " " + Thread.currentThread().getId(),
                            Thread.currentThread().getStackTrace());
            try {
                startSpan(getCase());
                if (span != null) {
                    span.setTag("type", "engine");
                    span.setTag("caseId", caseId.toString());
                    span.setTag("stacktrace", stacktrace);
                }
            } catch (Throwable t) {
                log().d("configure span with case {1} failed", caseId, t.toString());
            }
        }

        @Override
        public Lock getLock() {
            return lock;
        }

        @Override
        public String getStartStacktrace() {
            return stacktrace;
        }

        @Override
        public void resetPCase() {
            cazex = null;
        }

        @Override
        public void setPCase(PCase pCase) throws MException {
            try {
                if (span != null) span.log("setPCase " + pCase);
            } catch (Throwable t) {
            }
            if (cazex != null) throw new MException(RC.CONFLICT, "Case already set", caseId);
            if (!pCase.getId().equals(caseId))
                throw new MException(RC.CONFLICT, "Case has wrong id", caseId, pCase.getId());
            cazex = pCase;
        }

        @Override
        public void close() {
            synchronized (caseLocks) {
                if (lock != null) {
                    if (owner != Thread.currentThread()) {
                        log().w("closing by stranger", owner, Thread.currentThread());
                    }
                    fireEvent.release(this, caseId);
                    // XXX if (!lock.unlock())
                    lock.unlockHard(); // need hard, another thread could call close
                }
                lock = null;
                caseLocks.remove(caseId);
            }
            super.close();
        }

        @Override
        protected void finalize() {
            if (lock != null) {
                log().w("did not close lock", caseId, stacktrace);
                close();
            }
        }

        @Override
        public PCase getCase() throws NotFoundException, IOException {
            if (cazex == null) cazex = storage.loadCase(caseId);
            //            synchronized (caseCache) {
            //                PCase caze = caseCache.get(caseId);
            //                if (caze == null) {
            //                    caseCache.put(caseId, caze);
            //                }
            return cazex;
            //            }
        }

        @Override
        public PNode getFlowNode(UUID nodeId) throws NotFoundException, IOException {
            try {
                if (span != null) span.log("getFlowNode " + nodeId);
            } catch (Throwable t) {
            }
            synchronized (nodeCache) {
                PNode node = nodeCache.get(nodeId);
                if (node == null) {
                    node = storage.loadFlowNode(nodeId);
                    if (!node.getCaseId().equals(caseId))
                        throw new NotFoundException("node not in case", nodeId, caseId);
                    nodeCache.put(nodeId, node);
                }
                return node;
            }
        }

        @Override
        public void closeCase(boolean hard, int code, String msg)
                throws IOException, NotFoundException {
            try {
                if (span != null) span.log("closeCase " + hard + " " + code + " " + msg);
            } catch (Throwable t) {
            }
            PCase caze = getCase();

            statisticCaseClosed++;
            fireEvent.closeCase(caze, hard);
            EngineContext context = null;
            if (!hard) {
                try {
                    MUri uri = MUri.toUri(caze.getUri());
                    EProcess process = getProcess(uri);
                    EPool pool = getPool(process, uri);

                    context = new EngineContext(null, Engine.this);
                    context.setUri(uri.toString());
                    context.setEProcess(process);
                    context.setEPool(pool);
                    context.setPCase(caze);

                    APool<?> aPool = context.getPool();
                    aPool.closeCase();
                    Map<String, Object> newParameters = aPool.exportParameters();
                    caze.getParameters().putAll(newParameters);

                    if (aPool instanceof IndexValuesProvider) {
                        caze.setIndexValues(((IndexValuesProvider) aPool).createIndexValues(false));
                    } else {
                        caze.setIndexValues(null);
                    }

                } catch (Throwable t) {
                    log().e("close case {1} failed", caze, t);
                    fireEvent.error(caze, t);
                }
            }
            caze.close(code, msg);
            //                synchronized (caseCache) {
            storage.saveCase(caze);
            //                    caseCache.put(caze.getId(), caze);
            //                }

            if (!hard && caze.getCloseActivity() != null) {
                UUID closeId = caze.getCloseActivity();
                if (closeId != null) {
                    try {
                        doCloseActivity(context, closeId);
                    } catch (Throwable t) {
                        log().e("close activity {2} for case {1} failed", caze, closeId, t);
                    }
                }
            }
        }

        @Override
        public void saveFlowNode(PNode flow) throws IOException, NotFoundException {
            try {
                if (span != null) span.log("saveFlowNode " + flow);
            } catch (Throwable t) {
            }
            if (!flow.getCaseId().equals(caseId))
                throw new IOException("flow node is not part of the case " + flow + " " + caseId);
            fireEvent.saveFlowNode(flow, null);
            //            try {
            //                try (CaseLock lock = getCaseLock(flow, "saveFlowNode")) {
            //                synchronized (nodeCache) {
            storage.saveFlowNode(flow);
            //                    nodeCache.put(flow.getId(), flow);
            flow.updateStartState();
            //                }
            //                }
            //            } catch (TimeoutException te) {
            //                log().w(flow, te);
            //            }
        }

        @Override
        public void closeRuntime(UUID nodeId) throws MException, IOException {
            try {
                if (span != null) span.log("closeRuntime " + nodeId);
            } catch (Throwable t) {
            }
            PNode pNode = getFlowNode(nodeId);
            fireEvent.closeRuntime(pNode);
            PCase caze = null;
            try {
                caze = getCase();
            } catch (Throwable t) {
                log().e("close runtime {1} failed", pNode.getCaseId(), t);
                fireEvent.error(pNode, t);
                return; // ignore - try next time
            }

            if (caze.getState() != STATE_CASE.RUNNING) {
                pNode.setScheduled(newScheduledTime(pNode));
                return;
            }

            // create context
            EngineContext context = createContext(this, caze, pNode);

            RuntimeNode aNode = createRuntimeObject(context, pNode);
            aNode.close();

            pNode.setState(STATE_NODE.CLOSED);
            saveRuntime(pNode, aNode);

            // close pending activities
            UUID closeId = aNode.getCloseActivity();
            if (closeId != null) {
                try {
                    doCloseActivity(context, closeId);
                } catch (Throwable t) {
                    log().e("close activity {2} for node {1} failed", aNode, closeId, t);
                }
            }

            // cleanup locks
            leaveRestrictedArea(null, pNode);
        }

        @Override
        public void closeFlowNode(ProcessContext<?> context, PNode pNode, STATE_NODE state)
                throws IOException, NotFoundException {
            try {
                if (span != null) span.log("closeFlowNode " + pNode + " " + state);
            } catch (Throwable t) {
            }
            if (!pNode.getCaseId().equals(caseId))
                throw new IOException("flow node is not part of the case " + pNode + " " + caseId);
            fireEvent.closeFlowNode(pNode, state);

            boolean ownContext = context == null;
            if (context == null) context = new EngineContext(this, Engine.this, pNode);

            fireEvent.closedActivity(context.getARuntime(), pNode);

            if (context.getPCase() != null) {
                //                synchronized (context.getLock()) {
                pNode.setState(state);
                //                  synchronized (nodeCache) {
                storage.saveFlowNode(pNode);
                //                      nodeCache.put(pNode.getId(), pNode);
                //                  }
                savePCase(context);
                //                }
            } else {
                pNode.setState(state);
                //              synchronized (nodeCache) {
                storage.saveFlowNode(pNode);
                //                  nodeCache.put(pNode.getId(), pNode);
                //              }
            }

            if (ownContext) saveRuntime(context.getPRuntime(), context.getARuntime());
        }

        /**
         * Save the runtime to storage. If runtme object is given, the parameters will be stored.
         *
         * @param pRuntime
         * @param aRuntime
         * @throws IOException
         */
        @Override
        public void saveRuntime(PNode pRuntime, RuntimeNode aRuntime) throws IOException {
            try {
                if (span != null) span.log("saveRuntime " + pRuntime);
            } catch (Throwable t) {
            }
            if (aRuntime != null) {
                Map<String, Object> parameters = aRuntime.exportParamters();
                if (parameters != null) {
                    pRuntime.getParameters().putAll(parameters);
                }
            }
            fireEvent.saveRuntime(this, pRuntime, aRuntime);
            synchronized (nodeCache) {
                storage.saveFlowNode(pRuntime);
                nodeCache.put(pRuntime.getId(), pRuntime);
            }
        }

        @Override
        public void savePCase(ProcessContext<?> context) throws IOException, NotFoundException {
            savePCase(context.getPool(), false);
        }

        @Override
        public void savePCase(APool<?> aPool, boolean init) throws IOException, NotFoundException {
            try {
                if (span != null) span.log("savePCase " + init);
            } catch (Throwable t) {
            }
            PCase pCase = getCase();
            if (aPool != null) {
                Map<String, Object> newParameters = aPool.exportParameters();
                pCase.getParameters().putAll(newParameters);

                if (aPool instanceof IndexValuesProvider) {
                    pCase.setIndexValues(((IndexValuesProvider) aPool).createIndexValues(init));
                } else {
                    pCase.setIndexValues(null);
                }
                fireEvent.saveCase(pCase, aPool);
            }
            //              synchronized (caseCache) {
            //                  caseCache.put(pCase.getId(), pCase);
            storage.saveCase(pCase);
            //              }
            //            }
        }

        @Override
        public void doNodeErrorHandling(ProcessContext<?> context, PNode pNode, Throwable t) {
            try {
                if (span != null) span.log("doNodeErrorHandling " + pNode + " " + t);
            } catch (Throwable tt) {
            }
            fireEvent.doNodeErrorHandling(this, context, pNode, t);

            if (t instanceof TechnicalException) {
                try {
                    closeFlowNode(context, pNode, STATE_NODE.FAILED);
                } catch (Exception e) {
                    log().e("node error handling for node {1} failed", pNode, e);
                    fireEvent.error(pNode, e);
                }
                return;
            }

            if (t instanceof EngineException) {
                try {
                    closeFlowNode(context, pNode, STATE_NODE.STOPPED);
                } catch (Exception e) {
                    log().e("close node {1} failed", pNode, e);
                    fireEvent.error(pNode, e);
                }
                return;
            }

            EElement eNode = context.getENode();
            Trigger defaultError = null;
            Trigger professionalError = null;
            Trigger errorHandler = null;
            for (Trigger trigger : eNode.getTriggers()) {
                if (trigger.type() == TYPE.DEFAULT_ERROR) 
                    defaultError = trigger;
                else if (trigger.type() == TYPE.PROFESSIONAL_ERROR)
                    professionalError = trigger;
                else if (trigger.type() == TYPE.ERROR) {
                    if (t instanceof TaskException) {
                        if (trigger.name().equals(((TaskException) t).getTrigger()))
                            errorHandler = trigger;
                    } else
                    if (t instanceof IResult) {
                        if ( String.valueOf( ((IResult)t).getReturnCode() ).equals(trigger.name()) )
                            errorHandler = trigger;
                    }
                }
            }
            if (errorHandler == null) {
                if (professionalError != null && t instanceof IResult && RC.isProfessionalError( ((IResult)t).getReturnCode() ))
                    errorHandler = professionalError;
                else
                    errorHandler = defaultError;
            }
            if (errorHandler != null) {
                // create new activity
                EElement start =
                        context.getEPool().getElement(errorHandler.activity().getCanonicalName());
                try {
                    createActivity(context, pNode, start);
                    // close node
                    closeFlowNode(context, pNode, STATE_NODE.CLOSED);
                    return;
                } catch (Exception e) {
                    log().e(e);
                    fireEvent.error(pNode, start, e);
                }
            } else {
                // set node in error
                pNode.setState(STATE_NODE.FAILED);
                pNode.setExitMessage(t.toString());
                try {
                    saveFlowNode(pNode);
                } catch (NotFoundException | IOException e) {
                    log().e("save node {1} failed", pNode, e);
                }
            }
        }

        @Override
        public PNode createActivity(ProcessContext<?> context, PNode previous, EElement start)
                throws Exception {
            try {
                if (span != null) span.log("createActivity " + previous + " " + start);
            } catch (Throwable t) {
            }

            UUID caseId = context.getPCase().getId();
            UUID runtimeId = previous.getRuntimeId();

            Class<? extends AActor> actor = start.getAssignedActor(context.getEPool());

            // create flow node
            PNode flow =
                    new PNode(
                            UUID.randomUUID(),
                            caseId,
                            start.getName(),
                            start.getCanonicalName(),
                            System.currentTimeMillis(),
                            0,
                            STATE_NODE.NEW,
                            STATE_NODE.NEW,
                            start.getSchedulerList(),
                            start.getSignalList(),
                            start.getMessageList(),
                            false,
                            TYPE_NODE.NODE,
                            null,
                            null,
                            null,
                            runtimeId,
                            EngineConst.TRY_COUNT,
                            actor == null ? null : actor.getName(),
                            0);
            // flow.setScheduledNow();
            fireEvent.createActivity(
                    context.getARuntime(), flow, context.getPCase(), previous, start);
            context = new EngineContext((EngineContext) context, flow);

            doNodeLifecycle(context, flow);

            return flow;
        }

        @Override
        public void doNodeLifecycle(ProcessContext<?> context, PNode flow) throws Exception {
            try {
                if (span != null) span.log("doNodeLifecycle " + flow);
            } catch (Throwable t) {
            }

            boolean init =
                    flow.getStartState() == STATE_NODE.NEW; // this means the node is not executed!
            context = new EngineContext((EngineContext) context, flow);

            // lifecycle flow node
            EElement start = context.getENode();
            AActivity<?> activity = (AActivity<?>) createActivityObject(start);
            ((EngineContext) context).setANode(activity);
            RuntimeNode runtime = context.getARuntime();
            if (init) fireEvent.initStart(runtime, flow, start, activity);
            else fireEvent.executeStart(runtime, flow, start, activity);
            if (activity instanceof ContextRecipient)
                ((ContextRecipient) activity).setContext(context);
            activity.importParameters(flow.getParameters());

            try {
                if (init) {
                    activity.initializeActivity();

                    if (activity instanceof IndexValuesProvider) {
                        flow.setIndexValues(
                                ((IndexValuesProvider) activity).createIndexValues(true));
                    } else {
                        flow.setIndexValues(null);
                    }

                } else {
                    flow.setLastRunDate(System.currentTimeMillis());
                    try {
                        context.getPool().beforeExecute(activity);
                        activity.doExecuteActivity();
                    } finally {
                        context.getPool().afterExecute(activity);
                    }
                }
                // secure switch state away from NEW
                if (flow.getState() == STATE_NODE.NEW) {
                    flow.setState(STATE_NODE.RUNNING);
                    flow.setScheduledNow();
                } else if (flow.getStartState() == STATE_NODE.RUNNING
                        && flow.getState() == STATE_NODE.RUNNING) {
                    flow.setTryCount(flow.getTryCount() - 1);
                    if (flow.getTryCount() <= 0) {
                        flow.setSuspendedState(STATE_NODE.RUNNING);
                        flow.setState(STATE_NODE.STOPPED);
                    } else {
                        flow.setScheduled(newScheduledTime(flow));
                    }
                }
            } catch (Throwable t) {
                log().w("node lifecycle {1} failed", flow, t);
                // remember
                fireEvent.error(flow, t);
                if (init) fireEvent.initFailed(runtime, flow);
                else fireEvent.executeFailed(runtime, flow);
                doNodeErrorHandling(context, flow, t);
                return;
            }
            if (init) flow.getParameters().clear();

            // save
            saveFlowNode(context, flow, activity);

            if (init) fireEvent.initStop(runtime, flow);
            else fireEvent.executeStop(runtime, flow);
        }

        @Override
        public UUID createStartPoint(
                ProcessContext<?> context, EElement start, Map<String, ?> runtimeParam)
                throws Exception {
            try {
                if (span != null) span.log("createStartPoint " + start);
            } catch (Throwable t) {
            }

            // some checks
            if (!start.is(AStartPoint.class))
                throw new MException(RC.NOT_FOUND, "activity is not a start point", context, start);

            if (!context.getEPool().isElementOfPool(start))
                throw new MException(RC.CONFLICT, "start point is not part of the pool", context, start);

            // collect information
            ActivityDescription desc = start.getActivityDescription();
            Class<? extends RuntimeNode> runtimeClass = desc.runtime();

            fireEvent.createStartPoint(getCase(), start);

            Class<? extends AActor> actor = start.getAssignedActor(context.getEPool());

            // create runtime
            PNode runtime =
                    new PNode(
                            UUID.randomUUID(),
                            caseId,
                            "runtime",
                            runtimeClass.getCanonicalName(),
                            System.currentTimeMillis(),
                            0,
                            STATE_NODE.WAITING,
                            STATE_NODE.WAITING,
                            null,
                            null,
                            null,
                            false,
                            TYPE_NODE.RUNTIME,
                            null,
                            new HashMap<>(),
                            null,
                            null,
                            0,
                            null,
                            0);

            if (runtimeParam != null) runtime.getParameters().putAll(runtimeParam);

            fireEvent.createRuntime(getCase(), start, runtime);
            //          synchronized (nodeCache) {
            storage.saveFlowNode(runtime);
            //              nodeCache.put(runtime.getId(), runtime);
            //          }
            ((EngineContext) context).setPRuntime(runtime);
            UUID runtimeId = runtime.getId();

            // create flow node
            PNode flow =
                    new PNode(
                            UUID.randomUUID(),
                            caseId,
                            start.getName(),
                            start.getCanonicalName(),
                            System.currentTimeMillis(),
                            0,
                            STATE_NODE.NEW,
                            STATE_NODE.NEW,
                            start.getSchedulerList(),
                            start.getSignalList(),
                            start.getMessageList(),
                            false,
                            TYPE_NODE.NODE,
                            null,
                            null,
                            null,
                            runtimeId,
                            EngineConst.TRY_COUNT,
                            actor == null ? null : actor.getName(),
                            0);
            flow.setScheduledNow();
            fireEvent.createStartNode(context.getARuntime(), flow, context.getPCase(), start);
            context = new EngineContext((EngineContext) context, flow);
            doNodeLifecycle(context, flow);
            return flow.getId();
        }

        @Override
        public void saveFlowNode(ProcessContext<?> context, PNode flow, AActivity<?> activity)
                throws IOException, NotFoundException {
            try {
                if (span != null) span.log("saveFlowNode " + flow);
            } catch (Throwable t) {
            }
            fireEvent.saveFlowNode(flow, activity);
            if (activity != null) {
                try {
                    Map<String, Object> newParameters = activity.exportParamters();
                    flow.getParameters().putAll(newParameters);

                    if (activity instanceof IndexValuesProvider) {
                        flow.setIndexValues(
                                ((IndexValuesProvider) activity).createIndexValues(false));
                    } else {
                        flow.setIndexValues(null);
                    }

                } catch (Throwable t) {
                    log().e(t);
                    fireEvent.error(flow, activity, t);
                    // set failed
                    flow.setSuspendedState(flow.getState());
                    flow.setState(STATE_NODE.STOPPED);
                }
            }
            //                synchronized (nodeCache) {
            storage.saveFlowNode(flow);
            //                    nodeCache.put(flow.getId(), flow);
            flow.updateStartState();
            //                }

            if (context != null) savePCase(context);
        }

        @Override
        public void doFlowNode(PNode pNode) {
            try {
                if (span != null) span.log("doFlowNode " + pNode);
            } catch (Throwable t) {
            }
            fireEvent.doFlowNode(pNode);
            try {

                PCase caze = null;
                try {
                    caze = getCase();
                } catch (Throwable t) {
                    log().e("get case {1} failed", pNode.getCaseId(), t);
                    fireEvent.error(pNode, t);
                    return; // ignore - try next time
                }

                if (caze == null) {
                    // node without case ... puh
                    fireEvent.error("node without case", pNode, pNode.getCaseId());
                    closeFlowNode(null, pNode, STATE_NODE.STOPPED);
                    return;
                }
                if (caze.getState() != STATE_CASE.RUNNING) {
                    pNode.setScheduled(newScheduledTime(pNode));
                    fireEvent.doFlowNodeScheduled(pNode);
                    return;
                }

                // create context
                EngineContext context = createContext(this, caze, pNode);

                // check for timer trigger
                Entry<String, Long> nextScheduled = pNode.getNextScheduled();
                if (nextScheduled != null && !nextScheduled.getKey().equals("")) {
                    // do trigger
                    Trigger trigger = getTrigger(context, nextScheduled.getKey());
                    if (trigger == null) {
                        // set to error
                        log().e("Unknown trigger", pNode, nextScheduled.getKey());
                        fireEvent.error("Unknown trigger", pNode, nextScheduled.getKey());
                        closeFlowNode(context, pNode, STATE_NODE.STOPPED);
                        return;
                    }
                    Class<? extends AActivity<?>> next = trigger.activity();
                    EElement eNext = context.getEPool().getElement(next.getCanonicalName());
                    if (context.getARuntime().getConnectCount() > EngineConst.MAX_CREATE_ACTIVITY) {
                        fireEvent.error("max activities reached", caze);
                        closeCase(
                                true,
                                EngineConst.ERROR_CODE_MAX_CREATE_ACTIVITY,
                                "max activities reached");
                        return;
                    }
                    createActivity(context, pNode, eNext);
                    // close this
                    closeFlowNode(context, pNode, STATE_NODE.CLOSED);
                    return;
                }

                // do lifecycle
                doNodeLifecycle(context, pNode);
            } catch (Throwable t) {
                log().e("do flow node {1} failed", pNode, t);
                fireEvent.error(pNode, t);
            }
        }

        @Override
        public String toString() {
            return MSystem.toString(this, caseId);
        }

        @Override
        public UUID getCaseId() {
            return caseId;
        }

        @Override
        public RuntimeNode getRuntime(UUID nodeId) {
            synchronized (runtimeCache) {
                return runtimeCache.get(nodeId);
            }
        }

        @Override
        public void putRuntime(UUID id, RuntimeNode runtime) {
            try {
                if (span != null) span.log("putRuntime " + id);
            } catch (Throwable t) {
            }
            synchronized (runtimeCache) {
                runtimeCache.put(id, runtime);
            }
        }

        @Override
        public long getOwnerThreadId() {
            return owner == null ? -1 : owner.getId();
        }
    }

    public Lock acquireCleanupMaster() {
        return lockProvider.acquireCleanupMaster();
    }

    public Lock acquirePrepareMaster() {
        return lockProvider.acquirePrepareMaster();
    }

    public long getStatisticCaseClosed() {
        return statisticCaseClosed;
    }

    public long getStatisticRounds() {
        return statisticRounds;
    }

    @Override
    public boolean enterRestrictedArea(String resource, ProcessContext<?> context) {

        String runtimeId = context.getPRuntime().getId().toString();

        Lock engineLock = lockProvider.acquireEngineMaster();
        try {
            String lock = config.persistent.get(EngineConst.AREA_PREFIX + resource);
            if (lock == null) {
                config.persistent.set(EngineConst.AREA_PREFIX + resource, runtimeId);
                return true;
            } else return false;
        } catch (Throwable t) {
            fireEvent.error(context.getANode(), t);
        } finally {
            // lockProvider.releaseEngineMaster();
            if (engineLock != null) engineLock.unlockHard();
        }
        return false;
    }

    @Override
    public void leaveRestrictedArea(String resource, ProcessContext<?> context) {
        leaveRestrictedArea(resource, context == null ? null : context.getPRuntime());
    }

    public void leaveRestrictedArea(String resource, PNode runtime) {

        // lockProvider.acquireEngineMaster(); // this will cause a dead lock - it's no deed to lock
        // in this moment
        try {
            if (MString.isEmpty(resource)) {
                String runtimeId = runtime.getId().toString();
                for (String lock : findLocksForRuntime(runtimeId)) {
                    config.persistent.set(EngineConst.AREA_PREFIX + lock, null);
                    try {
                        fireMessage(null, EngineConst.AREA_PREFIX + lock, null);
                    } catch (NotFoundException e) {
                    }
                }
            } else {
                config.persistent.set(EngineConst.AREA_PREFIX + resource, null);
                try {
                    fireMessage(null, EngineConst.AREA_PREFIX + resource, null);
                } catch (NotFoundException e) {
                }
            }
        } catch (Throwable t) {
            log().e(t);
        } finally {
            // lockProvider.releaseEngineMaster();
        }
    }

    private List<String> findLocksForRuntime(String runtimeId) throws IOException {
        LinkedList<String> out = new LinkedList<>();
        config.persistent.reload();
        for (Entry<String, String> entry : config.persistent.getParameters().entrySet())
            if (entry.getKey().startsWith(EngineConst.AREA_PREFIX)
                    && entry.getValue().equals(runtimeId))
                out.add(entry.getKey().substring(EngineConst.AREA_PREFIX.length()));
        return out;
    }

    public long getStatisticCaseStarted() {
        return statisticCaseStarted;
    }

    @Deprecated
    public StorageProvider getStorage() {
        return storage;
    }
}
