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
package de.mhus.app.reactive.util.bpmn2;

import java.util.UUID;

import org.summerclouds.common.core.node.IProperties;
import org.summerclouds.common.core.tool.MString;
import org.summerclouds.common.core.util.MUri;
import org.summerclouds.common.core.util.MutableUri;

import de.mhus.app.reactive.model.activity.AActivity;
import de.mhus.app.reactive.model.activity.ASubProcess;
import de.mhus.app.reactive.model.annotations.SubDescription;
import de.mhus.app.reactive.model.engine.EngineConst;
import de.mhus.app.reactive.model.engine.InternalEngine;
import de.mhus.app.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.app.reactive.model.engine.ProcessContext;
import de.mhus.app.reactive.model.errors.EngineException;
import de.mhus.app.reactive.model.util.ActivityUtil;
import de.mhus.app.reactive.util.activity.RActivity;

/*man bpmn
 * Execute a sub process. Implementation need to map parameters to and from the sub process. While
 * the sub process is running, this process is not affected by changing parameters.
 */
public abstract class RSubProcess<P extends RPool<?>> extends RActivity<P>
        implements ASubProcess<P> {

    @Override
    public void doExecuteActivity() throws Exception {

        // get and check data
        SubDescription desc = getContext().getENode().getSubDescription();
        if (desc == null)
            throw new EngineException("sub process without SubDescription definition");

        String uri = desc.uri();
        if (MString.isEmpty(uri)) throw new EngineException("sub process without uri");

        InternalEngine iEngine = (InternalEngine) getContext().getEEngine();
        IProperties parameters = mapParametersForNewPool();
        MUri mUri = MUri.toUri(uri);
        ((MutableUri) mUri)
                .setParams(
                        new String[] {
                            EngineConst.OPTION_CLOSE_ACTIVITY
                                    + "="
                                    + getContext().getPNode().getId()
                        });
        UUID id = (UUID) iEngine.execute(mUri, parameters);
        if (id == null) throw new EngineException("Can't execute sub process");

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
     * Return parameter set for new pool.
     *
     * @return parameters or null if not necessary
     */
    protected abstract IProperties mapParametersForNewPool();

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
     * Map parameters back from executed case.
     *
     * @return The next task or null for default
     */
    protected abstract String doExecuteAfterSub(ProcessContext<?> closingContext);
}
