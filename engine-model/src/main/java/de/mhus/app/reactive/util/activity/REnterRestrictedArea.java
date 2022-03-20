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
package de.mhus.app.reactive.util.activity;

import java.util.Map;

import org.summerclouds.common.core.error.ValidationException;

import de.mhus.app.reactive.model.engine.EngineConst;
import de.mhus.app.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.app.reactive.model.engine.PNode.TYPE_NODE;
import de.mhus.app.reactive.model.util.ActivityUtil;
import de.mhus.app.reactive.model.util.ValidateParametersBeforeExecute;
import de.mhus.app.reactive.util.bpmn2.RPool;

public class REnterRestrictedArea<P extends RPool<?>> extends REvent<P>
        implements ValidateParametersBeforeExecute {

    @Override
    public void initializeActivity() throws Exception {

        String resource = getResourceName();

        if (getContext().getEEngine().enterRestrictedArea(resource, getContext())) {
            getContext().getPNode().setState(STATE_NODE.RUNNING);
        } else {
            getContext().getPNode().setState(STATE_NODE.WAITING);
            getContext().getPNode().setType(TYPE_NODE.MESSAGE);
            getContext().getPNode().setMessageEvent(EngineConst.AREA_PREFIX + resource);
        }
    }

    protected String getResourceName() {
        return ActivityUtil.getEvent(this);
    }

    @Override
    public void doExecute() throws Exception {}

    @Override
    public void validateParameters(Map<String, Object> parameters) throws ValidationException {
        String resource = getResourceName();
        if (!getContext().getEEngine().enterRestrictedArea(resource, getContext())) {
            throw new ValidationException("can't aquire lock", resource);
        }
    }
}
