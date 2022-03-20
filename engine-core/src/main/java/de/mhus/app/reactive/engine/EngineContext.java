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

import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.error.TimeoutException;
import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.core.tool.MSystem;

import de.mhus.app.reactive.model.activity.AActivity;
import de.mhus.app.reactive.model.activity.AElement;
import de.mhus.app.reactive.model.activity.APool;
import de.mhus.app.reactive.model.activity.ASwimlane;
import de.mhus.app.reactive.model.annotations.Output;
import de.mhus.app.reactive.model.annotations.Trigger;
import de.mhus.app.reactive.model.engine.AaaProvider;
import de.mhus.app.reactive.model.engine.CaseLock;
import de.mhus.app.reactive.model.engine.ContextRecipient;
import de.mhus.app.reactive.model.engine.EElement;
import de.mhus.app.reactive.model.engine.EEngine;
import de.mhus.app.reactive.model.engine.EPool;
import de.mhus.app.reactive.model.engine.EProcess;
import de.mhus.app.reactive.model.engine.PCase;
import de.mhus.app.reactive.model.engine.PCaseLock;
import de.mhus.app.reactive.model.engine.PNode;
import de.mhus.app.reactive.model.engine.ProcessContext;
import de.mhus.app.reactive.model.engine.RuntimeNode;

public class EngineContext extends MLog implements ProcessContext<APool<?>> {

    private Engine engine;

    private PCase pCase; // Persistent case object
    private EPool ePool; // pool descriptor, defined in PCase, provided by ProcessProvider
    private APool<?> aPool; // Pool Model object

    private PNode pNode; // Persistent flow node object
    private EElement eNode; // Flow Node descriptor, defined in PNode, provided by EPool
    private AElement<?> aNode; // Flow Node Model object

    private String uri;

    private EProcess eProcess;

    private PNode pRuntime;

    private RuntimeNode aRuntime;

    private ASwimlane<APool<?>> aLane;

    private CaseLock lock;

    public EngineContext(CaseLock lock, Engine engine) {
        this.engine = engine;
        this.lock = lock;
    }

    public EngineContext(CaseLock lock, Engine engine, PNode pNode) {
        this.engine = engine;
        this.pNode = pNode;
        this.lock = lock;
    }

    public EngineContext(EngineContext parent, PNode pNode) {
        this.engine = parent.engine;
        this.eProcess = parent.eProcess;
        this.aRuntime = parent.aRuntime;
        this.pRuntime = parent.pRuntime;
        this.pCase = parent.pCase;
        this.uri = parent.uri;
        this.ePool = parent.ePool;
        this.aPool = parent.aPool;

        this.pNode = pNode;
        this.lock = parent.lock;
    }

    //	synchronized void setPNode(PNode pNode) {
    //		this.pNode = pNode;
    //		eNode = null;
    //		pRuntime = null;
    //		aRuntime = null;
    //		aLane = null;
    //	}

    @Override
    public synchronized APool<?> getPool() {
        if (aPool == null)
            try {
                aPool = engine.createPoolObject(getEPool());
                if (aPool instanceof ContextRecipient) ((ContextRecipient) aPool).setContext(this);
                aPool.importParameters(getPCase().getParameters());
            } catch (MException e) {
                log().e(e);
            }
        return aPool;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ASwimlane<APool<?>> getSwimlane() {
        if (aLane == null) {
            try {
                aLane = (ASwimlane<APool<?>>) engine.createSwimlaneObject(this, getENode());
                if (aLane instanceof ContextRecipient) ((ContextRecipient) aLane).setContext(this);
            } catch (InstantiationException
                    | IllegalAccessException
                    | IllegalArgumentException
                    | InvocationTargetException
                    | NoSuchMethodException
                    | SecurityException e) {
                log().w(e);
            }
        }
        return aLane;
    }

    @Override
    public PCase getPCase() {
        return pCase;
    }

    void setPCase(PCase pCase) {
        this.pCase = pCase;
    }

    @Override
    public EPool getEPool() {
        return ePool;
    }

    public void setEPool(EPool ePool) {
        this.ePool = ePool;
    }

    void setAPool(APool<?> aPool) {
        this.aPool = aPool;
    }

    @Override
    public PNode getPNode() {
        return pNode;
    }

    @Override
    public synchronized EElement getENode() {
        if (eNode == null) {
            eNode = ePool.getElement(pNode.getCanonicalName());
            if (eNode == null) log().f("ENode not found in Pool", uri, pNode.getCanonicalName());
        }
        return eNode;
    }

    void setENode(EElement eNode) {
        this.eNode = eNode;
    }

    @Override
    public synchronized AActivity<?> getANode() {
        if (aNode == null) {
            try {
                aNode = engine.createActivityObject(getENode());
                if (aNode instanceof ContextRecipient) ((ContextRecipient) aNode).setContext(this);
                ((AActivity<?>) aNode).importParameters(getPNode().getParameters());
            } catch (MException e) {
                log().w(e);
            }
        }
        return (AActivity<?>) aNode;
    }

    void setANode(AElement<?> aNode) {
        this.aNode = aNode;
    }

    public Engine getEngine() {
        return engine;
    }

    @Override
    public EEngine getEEngine() {
        return engine;
    }

    void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public String getUri() {
        return uri;
    }

    public void setEProcess(EProcess eProcess) {
        this.eProcess = eProcess;
    }

    @Override
    public EProcess getEProcess() {
        return eProcess;
    }

    void setPRuntime(PNode runtime) {
        this.pRuntime = runtime;
    }

    @Override
    public PNode getPRuntime() {
        return pRuntime;
    }

    @Override
    public synchronized RuntimeNode getARuntime() {
        if (aRuntime == null) {
            if (pRuntime == null) {
                pRuntime = engine.getRuntimeForPNode(this, pNode);
                if (pRuntime == null)
                    throw new NullPointerException("PRuntime not found for " + this);
            }
            aRuntime = engine.createRuntimeObject(this, pRuntime);
            if (aRuntime instanceof ContextRecipient)
                ((ContextRecipient) aRuntime).setContext(this);
        }
        return aRuntime;
    }

    @Override
    public String toString() {
        return MSystem.toString(this, uri, pNode);
    }

    @Override
    public PNode createActivity(Class<? extends AActivity<?>> next) throws Exception {
        // check if defined
        boolean outFound = false;
        for (Output output : getENode().getOutputs())
            if (next == output.activity()) {
                outFound = true;
                break;
            }
        if (!outFound) {
            for (Trigger trigger : getENode().getTriggers())
                if (trigger.activity() == next) {
                    outFound = true;
                    break;
                }
        }
        if (!outFound) log().w("create undefined following activity", getENode(), next);

        EElement start = getEPool().getElement(next.getCanonicalName());
        PNode node = getPNode();
        try (PCaseLock lock =
                engine.getCaseLock(node, "createActivity:" + start.getCanonicalName())) {
            return lock.createActivity(this, node, start);
        }
    }

    @Override
    public void saveRuntime() throws IOException {
        try {
            try (PCaseLock lock = engine.getCaseLock(getPRuntime(), null)) {
                lock.saveRuntime(getPRuntime(), aRuntime);
            }
        } catch (TimeoutException te) {
            throw new IOException(te);
        }
    }

    @Override
    public AaaProvider getAaaProvider() {
        return engine.getAaaProvider();
    }

    @Override
    public CaseLock getCaseLock() {
        return lock;
    }

    @Override
    public void debug(Object... objects) {
        try {
            getARuntime().doDebugMsg(getPNode(), objects);
        } catch (Throwable t) {
            log().e(t);
        }
    }

    @Override
    public void error(Object... objects) {
        try {
            getARuntime().doErrorMsg(getPNode(), objects);
        } catch (Throwable t) {
            log().e(t);
        }
    }
}
