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
import java.util.UUID;

import org.summerclouds.common.core.error.NotFoundException;

import de.mhus.app.reactive.model.engine.PCase.STATE_CASE;
import de.mhus.app.reactive.model.engine.PNode.STATE_NODE;

public interface EEngine {

    Result<PCaseInfo> storageSearchCases(SearchCriterias criterias) throws IOException;

    Result<PCaseInfo> storageGetCases(STATE_CASE state) throws IOException;

    Result<PNodeInfo> storageGetFlowNodes(UUID caseId, STATE_NODE state) throws IOException;

    Result<PNodeInfo> storageSearchFlowNodes(SearchCriterias criterias) throws IOException;

    Result<PNodeInfo> storageGetScheduledFlowNodes(STATE_NODE state, long scheduled)
            throws IOException;

    Result<PNodeInfo> storageGetSignaledFlowNodes(STATE_NODE state, String signal)
            throws IOException;

    Result<PNodeInfo> storageGetMessageFlowNodes(UUID caseId, STATE_NODE state, String message)
            throws IOException;

    PNode getNodeWithoutLock(UUID nodeId) throws NotFoundException, IOException;

    PCase getCaseWithoutLock(UUID caseId) throws NotFoundException, IOException;

    boolean enterRestrictedArea(String resource, ProcessContext<?> context);

    void leaveRestrictedArea(String resource, ProcessContext<?> context);

    // PEngine getEnginePersistence();

    // void saveFlowNode(PNode flow) throws IOException, NotFoundException;

    /**
     * Return true if the engine is ready to work.
     *
     * @return true if ready.
     */
    boolean isReady();
}
