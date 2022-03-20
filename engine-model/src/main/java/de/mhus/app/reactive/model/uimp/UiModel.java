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
package de.mhus.app.reactive.model.uimp;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.UUID;

import org.summerclouds.common.core.tool.MSystem;

import de.mhus.app.reactive.model.engine.EngineMessage;
import de.mhus.app.reactive.model.ui.IModel;
import de.mhus.app.reactive.model.ui.INodeDescription;

public class UiModel implements IModel, Externalizable {

    private static final long serialVersionUID = 1L;
    private UUID nodeId;
    private INodeDescription[] outputs;
    private EngineMessage[] messages;
    private INodeDescription predecessor;
    private INodeDescription node;

    public UiModel() {}

    public UiModel(
            UUID nodeId,
            INodeDescription[] outputs,
            EngineMessage[] messages,
            INodeDescription predecessor,
            INodeDescription node)
            throws Exception {
        this.nodeId = nodeId;
        this.outputs = outputs;
        this.messages = messages;
        this.predecessor = predecessor;
        this.node = node;
    }

    @Override
    public INodeDescription getPredecessor() {
        return predecessor;
    }

    @Override
    public INodeDescription[] getOutputs() {
        return outputs;
    }

    @Override
    public INodeDescription getNode() {
        return node;
    }

    @Override
    public UUID getNodeId() {
        return nodeId;
    }

    @Override
    public EngineMessage[] getRuntimeMessages() {
        return messages;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(1);
        out.writeObject(nodeId);
        out.writeObject(outputs);
        out.writeObject(messages);
        out.writeObject(predecessor);
        out.writeObject(node);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        if (in.readInt() != 1) throw new IOException("Wrong object version");
        nodeId = (UUID) in.readObject();
        outputs = (INodeDescription[]) in.readObject();
        messages = (EngineMessage[]) in.readObject();
        predecessor = (INodeDescription) in.readObject();
        node = (INodeDescription) in.readObject();
    }

    @Override
    public String toString() {
        return MSystem.toString(this, nodeId);
    }
}
