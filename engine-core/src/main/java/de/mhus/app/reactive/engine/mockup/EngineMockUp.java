/**
` * Copyright (C) 2018 Mike Hummel (mh@mhus.de)
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
package de.mhus.app.reactive.engine.mockup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;

import org.summerclouds.common.core.M;
import org.summerclouds.common.core.error.NotFoundException;
import org.summerclouds.common.core.node.INode;
import org.summerclouds.common.core.node.INodeFactory;
import org.summerclouds.common.core.node.NodeList;

import de.mhus.app.reactive.engine.Engine;
import de.mhus.app.reactive.model.engine.PCase;
import de.mhus.app.reactive.model.engine.PCaseInfo;
import de.mhus.app.reactive.model.engine.PNode;
import de.mhus.app.reactive.model.engine.PNodeInfo;
import de.mhus.app.reactive.model.engine.StorageProvider;

public class EngineMockUp {

    private StorageProvider storage;

    @SuppressWarnings("unused")
    private Engine engine;

    private LinkedList<Step> steps = new LinkedList<>();
    private File file;
    private boolean recording;
    private int cnt;
    private boolean warn = false;
    private boolean verbose;

    public EngineMockUp(StorageProvider storage, Engine engine, File file)
            throws FileNotFoundException, Exception {
        this.storage = storage;
        this.engine = engine;
        this.file = file;
        recording = !file.exists();
        if (!recording) load();
    }

    public void step() throws NotFoundException, IOException {
        if (recording) record();
        else play();
    }

    protected void play() throws NotFoundException, IOException {
        Step step = steps.removeFirst();
        cnt++;
        if (step.getNr() != cnt) throw new IOException("Wrong Step Number " + step.getNr());
        for (PCaseInfo info : storage.getCases(null)) {
            PCase caze = storage.loadCase(info.getId());
            step.check(warn, verbose, cnt, caze);
        }
        for (PNodeInfo info : storage.getFlowNodes(null, null)) {
            PNode node = storage.loadFlowNode(info.getId());
            step.check(warn, verbose, cnt, node);
        }
    }

    protected void record() throws NotFoundException, IOException {
        Step step = new Step();
        for (PCaseInfo info : storage.getCases(null)) {
            PCase caze = storage.loadCase(info.getId());
            step.add(caze);
        }
        for (PNodeInfo info : storage.getFlowNodes(null, null)) {
            PNode node = storage.loadFlowNode(info.getId());
            step.add(node);
        }

        steps.add(step);
    }

    public void close() throws Exception {
        if (recording) save();
    }

    protected void load() throws FileNotFoundException, Exception {
        INode config = M.l(INodeFactory.class).read(file);
        steps.clear();
        for (INode nstep : config.getArray("step")) {
            Step step = new Step(nstep);
            steps.add(step);
        }
    }

    protected void save() throws Exception {
        cnt = 0;
        INode config = M.l(INodeFactory.class).create();
        NodeList array = config.createArray("step");
        for (Step step : steps) {
            cnt++;
            INode child = array.createObject();
            step.save(cnt, child);
        }
        M.l(INodeFactory.class).write(config, file);
    }

    public boolean isWarn() {
        return warn;
    }

    public void setWarn(boolean warn) {
        this.warn = warn;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
