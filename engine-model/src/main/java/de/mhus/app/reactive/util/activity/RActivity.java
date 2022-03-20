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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.core.pojo.PojoAttribute;
import org.summerclouds.common.core.pojo.PojoModel;

import de.mhus.app.reactive.model.activity.AActivity;
import de.mhus.app.reactive.model.annotations.PropertyDescription;
import de.mhus.app.reactive.model.engine.ContextRecipient;
import de.mhus.app.reactive.model.engine.ProcessContext;
import de.mhus.app.reactive.model.util.ActivityUtil;
import de.mhus.app.reactive.util.bpmn2.RPool;

public class RActivity<P extends RPool<?>> extends MLog implements AActivity<P>, ContextRecipient {

    public static String RETRY = "[RETRY]\u0001";

    private PojoModel pojoModel;
    private ProcessContext<P> context;

    @SuppressWarnings("unchecked")
    @Override
    public void setContext(ProcessContext<?> context) {
        this.context = (ProcessContext<P>) context;
    }

    @Override
    public ProcessContext<P> getContext() {
        return context;
    }

    @Override
    public void initializeActivity() throws Exception {}

    @Override
    public void doExecuteActivity() throws Exception {}

    @Override
    public Map<String, Object> exportParamters() {
        HashMap<String, Object> out = new HashMap<>();
        for (PojoAttribute<?> attr : getPojoModel()) {
            PropertyDescription desc = attr.getAnnotation(PropertyDescription.class);
            if (!desc.persistent()) continue;
            try {
                Object value = attr.get(this);
                if (value != null) out.put(attr.getName(), value);
            } catch (IOException e) {
                log().d("put attribute {1} failed", attr, e);
            }
        }
        return out;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void importParameters(Map<String, Object> parameters) {
        for (PojoAttribute attr : getPojoModel()) {
            PropertyDescription desc =
                    (PropertyDescription) attr.getAnnotation(PropertyDescription.class);
            if (!desc.persistent()) continue;
            try {
                Object value = parameters.get(attr.getName());
                if (value != null) attr.set(this, value, false);
            } catch (IOException e) {
                log().d("set attribute {1} failed", attr, e);
            }
        }
    }

    public synchronized PojoModel getPojoModel() {
        if (pojoModel == null) pojoModel = ActivityUtil.createPojoModel(this.getClass());
        return pojoModel;
    }

    protected void error(Object... msg) {
        log().e("report {1} error {2}", getContext().getPNode(), msg);
        try {
            getContext().getARuntime().doErrorMsg(getContext().getPNode(), msg);
        } catch (Throwable t) {
        }
    }

    protected void debug(Object... msg) {
        log().d("report {1} debug {2}", getContext().getPNode(), msg);
        try {
            getContext().getARuntime().doDebugMsg(getContext().getPNode(), msg);
        } catch (Throwable t) {
        }
    }
}
