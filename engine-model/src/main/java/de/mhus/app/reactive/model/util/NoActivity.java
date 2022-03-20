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

import de.mhus.app.reactive.model.activity.AActivity;
import de.mhus.app.reactive.model.engine.ProcessContext;

public class NoActivity implements AActivity<NoPool> {

    @Override
    public void initializeActivity() throws Exception {
        throw new NotSupportedException();
    }

    @Override
    public void doExecuteActivity() throws Exception {
        throw new NotSupportedException();
    }

    @Override
    public Map<String, Object> exportParamters() {
        throw new NotSupportedException();
    }

    @Override
    public void importParameters(Map<String, Object> parameters) {
        throw new NotSupportedException();
    }

    @Override
    public ProcessContext<NoPool> getContext() {
        throw new NotSupportedException();
    }
}
