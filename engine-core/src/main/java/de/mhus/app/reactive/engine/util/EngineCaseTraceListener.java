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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.summerclouds.common.core.log.Log;
import org.summerclouds.common.core.node.IProperties;
import org.summerclouds.common.core.node.MProperties;
import org.summerclouds.common.core.util.MUri;
import org.summerclouds.common.core.util.SoftHashMap;

import de.mhus.app.reactive.model.activity.AActivity;
import de.mhus.app.reactive.model.activity.APool;
import de.mhus.app.reactive.model.engine.CaseLock;
import de.mhus.app.reactive.model.engine.EElement;
import de.mhus.app.reactive.model.engine.EPool;
import de.mhus.app.reactive.model.engine.EProcess;
import de.mhus.app.reactive.model.engine.EngineListener;
import de.mhus.app.reactive.model.engine.PCase;
import de.mhus.app.reactive.model.engine.PNode;
import de.mhus.app.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.app.reactive.model.engine.ProcessContext;
import de.mhus.app.reactive.model.engine.RuntimeNode;

public class EngineCaseTraceListener implements EngineListener {

    SoftHashMap<UUID, Log> logs = new SoftHashMap<>();

    @Override
    public void doFlowNode(PNode node) {
        Log log = getLog(node);
        log.i("doFlowNode", node);
    }

    private Log getLog(PNode node) {
        return getLog(node.getCaseId());
    }

    private Log getLog(PCase caze) {
        return getLog(caze.getId());
    }

    private synchronized Log getLog(UUID id) {
        Log log = logs.get(id);
        if (log == null) {
//            File file = MSystem.getFile(MSystem.SCOPE.LOG, "reactive_" + id + ".log");
//            log = new FileLogger(id.toString(), file);
        	log = Log.getLog("reactive_" + id);
            logs.put(id, log);
        }
        return log;
    }

    @Override
    public void setScheduledToRunning(PNode node) {
        Log log = getLog(node);
        log.i("setScheduledToRunning", node);
    }

    @Override
    public void saveCase(PCase caze, APool<?> aPool) {
        Log log = getLog(caze);
        log.i("saveCase", caze, aPool);
    }

    @Override
    public void closeRuntime(PNode node) {
        Log log = getLog(node);
        log.i("closeRuntime", node);
    }

    @Override
    public void closeCase(PCase caze, boolean hard) {
        Log log = getLog(caze);
        log.i("closeCase", caze, hard);
    }

    @Override
    public void doFlowNodeScheduled(PNode node) {
        Log log = getLog(node);
        log.i("doFlowNodeScheduled", node);
    }

    @Override
    public void error(Object... objects) {}

    @Override
    public void closeFlowNode(PNode node, STATE_NODE state) {
        Log log = getLog(node);
        log.i("doFlowNode", node);
    }

    @Override
    public void startCase(
            MUri originalUri,
            MUri uri,
            IProperties properties,
            EProcess process,
            EPool pool,
            List<EElement> startPoints,
            MProperties options,
            String createdBy) {}

    @Override
    public void createStartPoint(PCase caze, EElement start) {
        Log log = getLog(caze);
        log.i("createStartPoint", caze, start);
    }

    @Override
    public void createRuntime(PCase caze, EElement start, PNode runtime) {
        Log log = getLog(caze);
        log.i("createRuntime", caze, start, runtime);
    }

    @Override
    public void createStartNode(RuntimeNode runtime, PNode flow, PCase pCase, EElement start) {
        Log log = getLog(pCase);
        log.i("createStartNode", runtime, flow, pCase, start);
    }

    @Override
    public void createActivity(
            RuntimeNode runtimeNode, PNode flow, PCase pCase, PNode previous, EElement start) {
        Log log = getLog(pCase);
        log.i("createStartNode", runtimeNode, flow, pCase, previous, start);
    }

    @Override
    public void executeStart(
            RuntimeNode runtime, PNode flow, EElement start, AActivity<?> activity) {
        Log log = getLog(flow);
        log.i("executeStart", runtime, flow, start, activity);
    }

    @Override
    public void saveFlowNode(PNode flow, AActivity<?> activity) {
        Log log = getLog(flow);
        log.i("saveFlowNode", flow, activity);
    }

    @Override
    public void archiveCase(PCase caze) {
        Log log = getLog(caze);
        log.i("archiveCase", caze);
    }

    @Override
    public void doStep(String step) {}

    @Override
    public void suspendCase(PCase caze) {
        Log log = getLog(caze);
        log.i("suspendCase", caze);
    }

    @Override
    public void unsuspendCase(PCase caze) {
        Log log = getLog(caze);
        log.i("unsuspendCase", caze);
    }

    @Override
    public void cancelFlowNode(PNode node) {
        Log log = getLog(node);
        log.i("cancelFlowNode", node);
    }

    @Override
    public void retryFlowNode(PNode node) {
        Log log = getLog(node);
        log.i("retryFlowNode", node);
    }

    @Override
    public void migrateCase(PCase caze, String uri, String migrator) {
        Log log = getLog(caze);
        log.i("migrateCase", caze, uri, migrator);
    }

    @Override
    public void restoreCase(PCase caze) {
        Log log = getLog(caze);
        log.i("restoreCase", caze);
    }

    @Override
    public void fireMessage(UUID caseId, String message, Map<String, Object> parameters) {
        Log log = getLog(caseId);
        log.i("fireMessage", caseId, message, parameters);
    }

    @Override
    public void fireExternal(UUID nodeId, String taskName, Map<String, Object> parameters) {}

    @Override
    public void fireSignal(String signal, Map<String, Object> parameters) {}

    @Override
    public void setScheduledToWaiting(PNode node) {
        Log log = getLog(node);
        log.i("setScheduledToWaiting", node);
    }

    @Override
    public void executeFailed(RuntimeNode runtime, PNode flow) {
        Log log = getLog(flow);
        log.i("executeFailed", runtime, flow);
    }

    @Override
    public void executeStop(RuntimeNode runtime, PNode flow) {
        Log log = getLog(flow);
        log.i("executeStop", runtime, flow);
    }

    @Override
    public void closedActivity(RuntimeNode aRuntime, PNode flow) {
        Log log = getLog(flow);
        log.i("closedActivity", aRuntime, flow);
    }

    @Override
    public void initStart(RuntimeNode runtime, PNode flow, EElement start, AActivity<?> activity) {
        Log log = getLog(flow);
        log.i("initStart", runtime, flow, start, activity);
    }

    @Override
    public void initFailed(RuntimeNode runtime, PNode flow) {
        Log log = getLog(flow);
        log.i("initFailed", runtime, flow);
    }

    @Override
    public void initStop(RuntimeNode runtime, PNode flow) {
        Log log = getLog(flow);
        log.i("initStop", runtime, flow);
    }

    @Override
    public void doNodeErrorHandling(
            CaseLock lock, ProcessContext<?> context, PNode node, Throwable t) {
        Log log = getLog(node);
        log.i("doNodeErrorHandling", node, context, t);
    }

    @Override
    public void saveRuntime(CaseLock lock, PNode pRuntime, RuntimeNode aRuntime) {
        Log log = getLog(pRuntime);
        log.i("saveRuntime", pRuntime, aRuntime);
    }

    @Override
    public void lock(CaseLock pCaseLock, UUID caseId) {
        Log log = getLog(caseId);
        log.i("lock");
    }

    @Override
    public void release(CaseLock pCaseLock, UUID caseId) {
        Log log = getLog(caseId);
        log.i("release");
    }
}
