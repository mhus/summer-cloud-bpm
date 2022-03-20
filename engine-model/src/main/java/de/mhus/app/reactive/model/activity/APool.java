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
package de.mhus.app.reactive.model.activity;

import java.util.Map;

import org.summerclouds.common.core.node.IProperties;
import org.summerclouds.common.core.node.MProperties;

import de.mhus.app.reactive.model.util.IndexValuesProvider;

/**
 * Interface for pools.
 *
 * @author mikehummel
 * @param <P>
 */
public interface APool<P extends APool<?>> extends AElement<P>, IndexValuesProvider {

    Map<String, Object> exportParameters();

    void importParameters(Map<String, Object> parameters);

    void initializeCase(Map<String, Object> parameters) throws Exception;

    void closeCase();

    void beforeExecute(AActivity<?> activity);

    void afterExecute(AActivity<?> activity);

    MProperties onUserCaseAction(String action, IProperties values);
}
