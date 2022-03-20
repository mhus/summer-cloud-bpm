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
package de.mhus.app.reactive.util.activity;

import de.mhus.app.reactive.model.activity.AActivity;
import de.mhus.app.reactive.model.activity.ASubProcess;
import de.mhus.app.reactive.model.annotations.SubDescription;
import de.mhus.app.reactive.model.engine.EElement;
import de.mhus.app.reactive.model.engine.InternalEngine;
import de.mhus.app.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.app.reactive.model.engine.ProcessContext;
import de.mhus.app.reactive.model.engine.RuntimeNode;
import de.mhus.app.reactive.model.errors.EngineException;
import de.mhus.app.reactive.model.util.ActivityUtil;
import de.mhus.app.reactive.util.bpmn2.RPool;

/**
 * Execute another StartPoint of the same pool and wait until it ends. If you use InactiveStartPoint
 * the will also be executed. You can realize sub sequences using this Element.
 *
 * <p>The SubStart will run in the same case but within a separate Runtime.
 *
 * <p>Define the start point using the SubDescription
 *
 * @author mikehummel
 * @param <P>
 */
public class RSubStart<P extends RPool<?>> extends RActivity<P> implements ASubProcess<P> {

    @Override
    public void doExecuteActivity() throws Exception {

        // get and check data
        SubDescription desc = getClass().getAnnotation(SubDescription.class);
        if (desc == null) throw new EngineException("sub start without SubDescription definition");
        String myStartPointName = desc.start().getCanonicalName();
        EElement eMyStartPoint = getContext().getEPool().getElement(myStartPointName);
        if (eMyStartPoint == null)
            throw new EngineException(
                    "sub start point '"
                            + myStartPointName
                            + "' not found in pool "
                            + getPool().getClass());

        // start sub node and set close activity
        InternalEngine iEngine = (InternalEngine) getContext().getEEngine();
        RuntimeNode runtime = iEngine.doExecuteStartPoint(getContext(), eMyStartPoint);
        prepareNewRuntime(runtime);
        runtime.setCloseActivity(getContext().getPNode().getId());
        runtime.save();

        if (desc.waiting()) {
            // set this node to wait
            getContext().getPNode().setState(STATE_NODE.WAITING);
        } else {
            // next
            String nextName = DEFAULT_OUTPUT;
            Class<? extends AActivity<?>> next = ActivityUtil.getOutputByName(this, nextName);
            if (next == null)
                throw new EngineException(
                        "Output Activity not found: "
                                + nextName
                                + " in "
                                + getClass().getCanonicalName());
            getContext().createActivity(next);
            getContext().getPNode().setState(STATE_NODE.CLOSED);
        }
    }

    /**
     * Prepare new runtime object before save if needed
     *
     * @param runtime
     */
    protected void prepareNewRuntime(RuntimeNode runtime) {}

    @Override
    public void doClose(ProcessContext<?> closingContext) throws Exception {
        // execute next activity
        String nextName = doExecuteAfterSub(closingContext);
        if (nextName == null) nextName = DEFAULT_OUTPUT;
        if (!nextName.equals(RETRY)) {
            Class<? extends AActivity<?>> next = ActivityUtil.getOutputByName(this, nextName);
            if (next == null)
                throw new EngineException(
                        "Output Activity not found: "
                                + nextName
                                + " in "
                                + getClass().getCanonicalName());
            getContext().createActivity(next);
            getContext().getPNode().setState(STATE_NODE.CLOSED);
        }
    }

    /**
     * Overwrite to do something after the sub is finished and before the next task is started.
     *
     * @return The next task or null for default
     */
    protected String doExecuteAfterSub(ProcessContext<?> closingContext) {
        return null;
    }
}
