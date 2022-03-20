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

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.error.NotFoundException;

import de.mhus.app.reactive.model.activity.AActivity;
import de.mhus.app.reactive.model.activity.APool;
import de.mhus.app.reactive.model.engine.PNode.STATE_NODE;

public interface PCaseLock extends CaseLock {

    void closeRuntime(UUID nodeId) throws MException, IOException;

    void closeFlowNode(ProcessContext<?> context, PNode pNode, STATE_NODE state)
            throws IOException, NotFoundException;

    void saveRuntime(PNode pRuntime, RuntimeNode aRuntime) throws IOException;

    void savePCase(ProcessContext<?> context) throws IOException, NotFoundException;

    void savePCase(APool<?> aPool, boolean init) throws IOException, NotFoundException;

    void doNodeErrorHandling(ProcessContext<?> context, PNode pNode, Throwable t);

    PNode createActivity(ProcessContext<?> context, PNode previous, EElement start)
            throws Exception;

    void doNodeLifecycle(ProcessContext<?> context, PNode flow) throws Exception;

    UUID createStartPoint(ProcessContext<?> context, EElement start, Map<String, ?> runtimeParam)
            throws Exception;

    void saveFlowNode(ProcessContext<?> context, PNode flow, AActivity<?> activity)
            throws IOException, NotFoundException;

    void doFlowNode(PNode pNode);

    void setPCase(PCase pCase) throws MException;

    void resetPCase();

    UUID getCaseId();

    long getOwnerThreadId();
}
