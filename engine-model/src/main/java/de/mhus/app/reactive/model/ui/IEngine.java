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

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.error.NotFoundException;
import org.summerclouds.common.core.form.FormControl;
import org.summerclouds.common.core.form.IFormInformation;
import org.summerclouds.common.core.node.IProperties;
import org.summerclouds.common.core.node.MProperties;

import de.mhus.app.reactive.model.engine.EngineMessage;
import de.mhus.app.reactive.model.engine.SearchCriterias;

// for serialization and remote access reasons - do not implement methods with the same name
public interface IEngine {

    List<INode> searchNodes(SearchCriterias criterias, int page, int size, String... propertyNames)
            throws NotFoundException, IOException;

    List<ICase> searchCases(SearchCriterias criterias, int page, int size, String... propertyNames)
            throws NotFoundException, IOException;

    IProcess getProcess(String uri) throws MException;

    IPool getPool(String uri) throws MException;

    ICase getCase(String id, String... propertyNames) throws Exception;

    // node

    INode getNode(String id, String... propertyNames) throws Exception;

    IFormInformation getNodeUserForm(String id) throws Exception;

    IProperties getNodeUserFormValues(String id) throws MException, Exception;

    Class<? extends FormControl> getNodeUserFormControl(String id) throws Exception;

    void submitUserTask(String id, IProperties values) throws Exception;

    void doUnassignUserTask(String id) throws Exception;

    void doAssignUserTask(String id) throws Exception;

    MProperties onUserTaskAction(String id, String action, MProperties values) throws Exception;

    /**
     * Set days until due or -1 to reset the value.
     *
     * @param id
     * @param days
     * @throws Exception
     */
    void setDueDays(String id, int days) throws Exception;

    // case

    default ICaseDescription getCaseDescription2(ICase caze) throws Exception {
        return getCaseDescription(caze.getUri());
    }

    default INodeDescription getNodeDescription2(INode node) throws Exception {
        return getNodeDescription(node.getUri(), node.getCanonicalName());
    }

    ICaseDescription getCaseDescription(String uri) throws Exception;

    INodeDescription getNodeDescription(String uri, String name) throws Exception;

    Locale getLocale();

    String getUser();

    Object doExecute(String uri) throws Exception;

    Object doExecute2(String uri, IProperties properties) throws Exception;

    void doArchive(UUID caseId) throws Exception;

    MProperties onUserCaseAction(String id, String action, MProperties values) throws Exception;

    /**
     * Return the surrounding model for the node.
     *
     * @param nodeId
     * @return The model
     * @throws Exception
     */
    IModel getModel(UUID nodeId) throws Exception;

    /**
     * Return all node models for the running case.
     *
     * @param caseId
     * @return all node models
     * @throws Exception
     */
    IModel[] getCaseModels(UUID caseId) throws Exception;

    /**
     * Return all runtime messages of a case.
     *
     * @param caseId
     * @return List of message trails.
     * @throws Exception
     */
    List<EngineMessage[]> getCaseRuntimeMessages(String caseId) throws Exception;

    /**
     * Return the runtime trail of a node.
     *
     * @param nodeId
     * @return Message trail of the runtime
     * @throws Exception
     */
    EngineMessage[] getNodeRuntimeMessage(String nodeId) throws Exception;

    /**
     * Will close this UI engine instance. Not the central engine. For some implementations this
     * will be helpful to release resources.
     */
    void close();

    boolean isClosed();
}
