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
package de.mhus.app.reactive.model.ui;

import java.io.Serializable;
import java.util.UUID;

import de.mhus.app.reactive.model.engine.EngineMessage;

public interface IModel extends Serializable {

    /**
     * Return the predecessor if exists.
     *
     * @return Description or null
     */
    INodeDescription getPredecessor();

    /**
     * Return possible outputs
     *
     * @return Descriptions
     */
    INodeDescription[] getOutputs();

    /**
     * Return information about the current node
     *
     * @return Description
     */
    INodeDescription getNode();

    /**
     * Return the node id if exists
     *
     * @return The id or null
     */
    UUID getNodeId();

    /**
     * Return the runtime messages if exists
     *
     * @return The messages or null
     */
    EngineMessage[] getRuntimeMessages();
}
