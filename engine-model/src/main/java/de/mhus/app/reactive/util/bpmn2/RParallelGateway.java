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

import java.util.LinkedList;
import java.util.UUID;

import de.mhus.app.reactive.model.activity.AParallelGateway;
import de.mhus.app.reactive.model.annotations.Output;
import de.mhus.app.reactive.model.engine.CaseLock;
import de.mhus.app.reactive.model.engine.EEngine;
import de.mhus.app.reactive.model.engine.PNode;
import de.mhus.app.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.app.reactive.model.engine.PNodeInfo;
import de.mhus.app.reactive.model.engine.ProcessContext;
import de.mhus.app.reactive.model.util.ActivityUtil;
import de.mhus.app.reactive.util.activity.RGateway;

/*man bpmn
 * Execute all Output(s) in parallel.
 */
public class RParallelGateway<P extends RPool<?>> extends RGateway<P>
        implements AParallelGateway<P> {

    @Override
    public void initializeActivity() throws Exception {

        ProcessContext<P> context = getContext();
        EEngine engine = context.getEEngine();
        // by default set to wait
        context.getPNode().setState(STATE_NODE.WAITING);

        // all inputs are done?
        int size = ActivityUtil.getInputs(this).length;

        // find waiting gateways
        String myName = context.getPNode().getCanonicalName();
        UUID myId = context.getPNode().getId();
        LinkedList<UUID> current = new LinkedList<>();
        for (PNodeInfo info :
                engine.storageGetFlowNodes(context.getPNode().getCaseId(), STATE_NODE.WAITING)) {
            if (info.getCanonicalName().equals(myName) && !info.getId().equals(myId) /* paranoia */)
                current.add(info.getId());
        }
        context.getARuntime().doEvent("join_status", context.getPNode(), 0, current.size(), size);
        if (current.size() >= (size - 1)) { // decrease 1 for myself
            // close all others, activate me !!!
            context.getPNode().setState(STATE_NODE.RUNNING);
            CaseLock lock = getContext().getCaseLock();
            for (UUID id : current) {
                PNode node = lock.getFlowNode(id);
                node.setState(STATE_NODE.CLOSED);
                lock.saveFlowNode(node);
            }
        }
    }

    @Override
    public Output[] doExecute() throws Exception {
        return ActivityUtil.getOutputs(this);
    }
}
