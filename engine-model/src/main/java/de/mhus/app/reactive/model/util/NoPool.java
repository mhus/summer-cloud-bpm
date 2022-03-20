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
package de.mhus.app.reactive.model.util;

import java.util.Map;

import org.summerclouds.common.core.error.NotSupportedException;
import org.summerclouds.common.core.node.IProperties;
import org.summerclouds.common.core.node.MProperties;

import de.mhus.app.reactive.model.activity.AActivity;
import de.mhus.app.reactive.model.activity.APool;

@SuppressWarnings("rawtypes")
public class NoPool implements APool<NoPool> {

    @Override
    public Map<String, Object> exportParameters() {
        throw new NotSupportedException();
    }

    @Override
    public void importParameters(Map parameters) {
        throw new NotSupportedException();
    }

    @Override
    public void initializeCase(Map parameters) {
        throw new NotSupportedException();
    }

    @Override
    public void closeCase() {
        throw new NotSupportedException();
    }

    @Override
    public String[] createIndexValues(boolean init) {
        return null;
    }

    @Override
    public void beforeExecute(AActivity<?> activity) {}

    @Override
    public void afterExecute(AActivity<?> activity) {}

    @Override
    public MProperties onUserCaseAction(String action, IProperties values) {
        return null;
    }
}
