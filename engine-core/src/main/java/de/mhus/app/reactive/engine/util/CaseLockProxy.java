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
import java.util.Map;
import java.util.UUID;

import org.summerclouds.common.core.concurrent.Lock;
import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.error.NotFoundException;
import org.summerclouds.common.core.tool.MCast;
import org.summerclouds.common.core.tool.MSystem;

import de.mhus.app.reactive.model.activity.AActivity;
import de.mhus.app.reactive.model.activity.APool;
import de.mhus.app.reactive.model.engine.EElement;
import de.mhus.app.reactive.model.engine.EngineListener;
import de.mhus.app.reactive.model.engine.PCase;
import de.mhus.app.reactive.model.engine.PCaseLock;
import de.mhus.app.reactive.model.engine.PNode;
import de.mhus.app.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.app.reactive.model.engine.ProcessContext;
import de.mhus.app.reactive.model.engine.RuntimeNode;

public class CaseLockProxy extends CaseLockImpl implements PCaseLock {

    PCaseLock instance;
    private UUID caseId;
    private EngineListener fireEvent;
    private String stacktrace;

    public CaseLockProxy(
            PCaseLock instance, EngineListener fireEvent, String operation, Object... tagPairs) {
        super(false, operation, tagPairs);
        caseId = instance.getCaseId();
        fireEvent.lock(this, caseId);
        this.instance = instance;
        this.fireEvent = fireEvent;
        stacktrace = MCast.toString("Proxy " + caseId + " " + Thread.currentThread().getId());
        try {
            startSpan(getCase());
            if (span != null) {
                span.setTag("type", "proxy");
                span.setTag("caseId", caseId.toString());
                span.setTag("stacktrace", stacktrace);
            }
        } catch (Throwable t) {
            log().d("configure span failed for case {1}", caseId, t);
        }
    }

    @Override
    public PCase getCase() throws NotFoundException, IOException {
        return instance.getCase();
    }

    @Override
    public PNode getFlowNode(UUID id) throws NotFoundException, IOException {
        try {
            if (span != null) span.log("getFlowNode " + id);
        } catch (Throwable t) {
        }
        return instance.getFlowNode(id);
    }

    @Override
    public void closeCase(boolean hard, int code, String msg)
            throws IOException, NotFoundException {
        try {
            if (span != null) span.log("closeCase " + hard + " " + code + " " + msg);
        } catch (Throwable t) {
        }
        instance.closeCase(hard, code, msg);
    }

    @Override
    public void close() {
        if (instance == null) return;
        fireEvent.release(this, caseId);
        instance = null;
        fireEvent = null;
        super.close();
    }

    @Override
    public void saveFlowNode(PNode node) throws IOException, NotFoundException {
        try {
            if (span != null) span.log("saveFlowNode " + node);
        } catch (Throwable t) {
        }
        instance.saveFlowNode(node);
    }

    @Override
    public void closeRuntime(UUID nodeId) throws MException, IOException {
        try {
            if (span != null) span.log("closeRuntime " + nodeId);
        } catch (Throwable t) {
        }
        instance.closeRuntime(nodeId);
    }

    @Override
    public void closeFlowNode(ProcessContext<?> context, PNode pNode, STATE_NODE state)
            throws IOException, NotFoundException {
        try {
            if (span != null) span.log("closeFlowNode " + pNode + " " + state);
        } catch (Throwable t) {
        }
        instance.closeFlowNode(context, pNode, state);
    }

    @Override
    public void saveRuntime(PNode pRuntime, RuntimeNode aRuntime) throws IOException {
        try {
            if (span != null) span.log("saveRuntime " + pRuntime);
        } catch (Throwable t) {
        }
        instance.saveRuntime(pRuntime, aRuntime);
    }

    @Override
    public void savePCase(ProcessContext<?> context) throws IOException, NotFoundException {
        try {
            if (span != null) span.log("savePCase");
        } catch (Throwable t) {
        }
        instance.savePCase(context);
    }

    @Override
    public void savePCase(APool<?> aPool, boolean init) throws IOException, NotFoundException {
        try {
            if (span != null) span.log("savePCase " + init);
        } catch (Throwable t) {
        }
        instance.savePCase(aPool, init);
    }

    @Override
    public void doNodeErrorHandling(ProcessContext<?> context, PNode pNode, Throwable t) {
        try {
            if (span != null) span.log("doNodeErrorHandling " + pNode + " " + t);
        } catch (Throwable tt) {
        }
        instance.doNodeErrorHandling(context, pNode, t);
    }

    @Override
    public PNode createActivity(ProcessContext<?> context, PNode previous, EElement start)
            throws Exception {
        try {
            if (span != null) span.log("createActivity " + previous + " " + start);
        } catch (Throwable t) {
        }
        return instance.createActivity(context, previous, start);
    }

    @Override
    public void doNodeLifecycle(ProcessContext<?> context, PNode flow) throws Exception {
        try {
            if (span != null) span.log("doNodeLifecycle " + flow);
        } catch (Throwable t) {
        }
        instance.doNodeLifecycle(context, flow);
    }

    @Override
    public UUID createStartPoint(
            ProcessContext<?> context, EElement start, Map<String, ?> runtimeParam)
            throws Exception {
        try {
            if (span != null) span.log("createStartPoint " + start);
        } catch (Throwable t) {
        }
        return instance.createStartPoint(context, start, runtimeParam);
    }

    @Override
    public void saveFlowNode(ProcessContext<?> context, PNode flow, AActivity<?> activity)
            throws IOException, NotFoundException {
        try {
            if (span != null) span.log("saveFlowNode " + flow);
        } catch (Throwable t) {
        }
        instance.saveFlowNode(context, flow, activity);
    }

    @Override
    public void doFlowNode(PNode pNode) {
        try {
            if (span != null) span.log("doFlowNode " + pNode);
        } catch (Throwable t) {
        }
        instance.doFlowNode(pNode);
    }

    @Override
    public void setPCase(PCase pCase) throws MException {
        try {
            if (span != null) span.log("setPCase " + pCase);
        } catch (Throwable t) {
        }
        instance.setPCase(pCase);
    }

    @Override
    public void resetPCase() {
        try {
            if (span != null) span.log("resetPCase");
        } catch (Throwable t) {
        }
        instance.resetPCase();
    }

    @Override
    public UUID getCaseId() {
        return caseId;
    }

    @Override
    public String toString() {
        return MSystem.toString(this, caseId);
    }

    @Override
    public RuntimeNode getRuntime(UUID nodeId) {
        return instance.getRuntime(nodeId);
    }

    @Override
    public void putRuntime(UUID id, RuntimeNode runtime) {
        try {
            if (span != null) span.log("putRuntime " + id);
        } catch (Throwable t) {
        }
        instance.putRuntime(id, runtime);
    }

    @Override
    public Lock getLock() {
        return instance.getLock();
    }

    @Override
    public String getStartStacktrace() {
        return stacktrace;
    }

    @Override
    public long getOwnerThreadId() {
        return instance.getOwnerThreadId();
    }
}
