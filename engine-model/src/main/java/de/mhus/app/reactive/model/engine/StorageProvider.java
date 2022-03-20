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

import org.summerclouds.common.core.error.NotFoundException;

/** @author mikehummel */
public interface StorageProvider {

    /**
     * Save or update the case into storage.
     *
     * @param caze
     * @throws IOException
     */
    void saveCase(PCase caze) throws IOException;

    /**
     * Load the case from storage'
     *
     * @param id
     * @return The requested case
     * @throws IOException
     * @throws NotFoundException
     */
    PCase loadCase(UUID id) throws IOException, NotFoundException;

    /**
     * Delete the case and all flow data from storage.
     *
     * @param id
     * @throws IOException
     */
    void deleteCaseAndFlowNodes(UUID id) throws IOException;

    /**
     * Delete the flow data from storage.
     *
     * @param id
     * @throws IOException
     */
    void deleteFlowNode(UUID id) throws IOException;

    /**
     * Save or update the flow node.
     *
     * @param flow
     * @throws IOException
     */
    void saveFlowNode(PNode flow) throws IOException;

    /**
     * Load the flow node.
     *
     * @param id
     * @return The requested node
     * @throws IOException
     * @throws NotFoundException
     */
    PNode loadFlowNode(UUID id) throws IOException, NotFoundException;

    /**
     * Load all cases with the specified state or all.
     *
     * <p>The set is only used to iterate the result. Other functionality is not needed. You can use
     * a open database handle until the end of the queue is reached.
     *
     * @param state The state or null for all states.
     * @return Set to iterate the results.
     * @throws IOException
     */
    Result<PCaseInfo> getCases(PCase.STATE_CASE state) throws IOException;

    /**
     * Load all flows for this case with the specified state or all.
     *
     * <p>The set is only used to iterate the result. Other functionality is not needed. You can use
     * a open database handle until the end of the queue is reached.
     *
     * @param caseId The id of the case.
     * @param state The state or null for all states.
     * @return list
     * @throws IOException
     */
    Result<PNodeInfo> getFlowNodes(UUID caseId, PNode.STATE_NODE state) throws IOException;

    /**
     * Returns all flow nodes with the state and a scheduled time greater zero and lesser or equals
     * 'scheduled'.
     *
     * @param state The state or null
     * @param scheduled
     * @param order order modify date descending (true) or without order - used to process oldest
     *     first
     * @return List of results
     * @throws IOException
     */
    Result<PNodeInfo> getScheduledFlowNodes(PNode.STATE_NODE state, long scheduled, boolean order)
            throws IOException;

    Result<PNodeInfo> getSignalFlowNodes(PNode.STATE_NODE state, String signal) throws IOException;

    Result<PNodeInfo> getMessageFlowNodes(UUID caseId, PNode.STATE_NODE state, String message)
            throws IOException;

    Result<PNodeInfo> searchFlowNodes(SearchCriterias criterias) throws IOException;

    /**
     * Return all current engine values.
     *
     * @return List of results
     * @throws IOException
     */
    Map<String, String> loadEngine() throws IOException;

    /**
     * Return the current value of the key.
     *
     * @param key
     * @return the value or null
     * @throws IOException
     */
    String getEngineValue(String key) throws IOException;

    /**
     * Update or Insert the key-value pair.
     *
     * @param key
     * @param value
     * @throws IOException
     */
    void setEngineValue(String key, String value) throws IOException;

    /**
     * Remove the key of exists.
     *
     * @param key
     * @throws IOException
     */
    void deleteEngineValue(String key) throws IOException;

    PNodeInfo loadFlowNodeInfo(UUID nodeId) throws IOException;

    PCaseInfo loadCaseInfo(UUID caseId) throws IOException;

    Result<PCaseInfo> searchCases(SearchCriterias criterias) throws IOException;

    boolean setNodePriority(UUID nodeId, int priority) throws IOException;

    boolean setNodeScope(UUID nodeId, int scope) throws IOException;

    boolean setCasePriority(UUID caseId, int priority) throws IOException;

    boolean setCaseScope(UUID caseId, int scope) throws IOException;

    void updateFullCase(PCase caze) throws IOException;

    void updateFullFlowNode(PNode node) throws IOException;
}
