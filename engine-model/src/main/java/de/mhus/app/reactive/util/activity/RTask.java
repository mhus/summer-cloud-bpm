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

import org.summerclouds.common.core.pojo.PojoAttribute;
import org.summerclouds.common.core.pojo.PojoModel;

import de.mhus.app.reactive.model.util.ActivityUtil;
import de.mhus.app.reactive.util.bpmn2.RPool;

public abstract class RTask<P extends RPool<?>> extends RAbstractTask<P> {

    private PojoModel pojoModel;

    @Override
    public Map<String, Object> exportParamters() {
        HashMap<String, Object> out = new HashMap<>();
        for (PojoAttribute<?> attr : getPojoModel()) {
            try {
                Object value = attr.get(this);
                if (value != null) out.put(attr.getName(), value);
            } catch (IOException e) {
                log().d("export parameter {1} failed", attr, e);
            }
        }
        return out;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void importParameters(Map<String, Object> parameters) {
        for (PojoAttribute attr : getPojoModel()) {
            try {
                Object value = parameters.get(attr.getName());
                if (value != null) attr.set(this, value, false);
            } catch (IOException e) {
                log().d("import parameter {1} failed", attr, e);
            }
        }
    }

    @Override
    public synchronized PojoModel getPojoModel() {
        if (pojoModel == null) pojoModel = ActivityUtil.createPojoModel(this.getClass());
        return pojoModel;
    }
}
