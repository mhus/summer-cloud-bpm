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
package de.mhus.app.reactive.model.engine;

import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;

import org.summerclouds.common.core.concurrent.Lock;
import org.summerclouds.common.core.error.NotFoundException;

public interface CaseLock extends Closeable {

    PCase getCase() throws NotFoundException, IOException;

    PNode getFlowNode(UUID id) throws NotFoundException, IOException;

    default PNode getFlowNode(PNodeInfo nodeInfo) throws NotFoundException, IOException {
        return getFlowNode(nodeInfo.getId());
    }

    void closeCase(boolean hard, int code, String msg) throws IOException, NotFoundException;

    @Override
    public void close();

    void saveFlowNode(PNode node) throws IOException, NotFoundException;

    RuntimeNode getRuntime(UUID nodeId);

    void putRuntime(UUID id, RuntimeNode runtime);

    Lock getLock();

    String getStartStacktrace(); // TODO remove, is already in lock owner
}
