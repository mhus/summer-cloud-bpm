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

import java.util.Set;
import java.util.TreeSet;

import org.summerclouds.common.core.form.DefRoot;
import org.summerclouds.common.core.form.DefaultFormInformation;
import org.summerclouds.common.core.form.IFormInformation;
import org.summerclouds.common.core.form.ModelUtil;
import org.summerclouds.common.core.node.MProperties;

import de.mhus.app.reactive.model.engine.EngineConst;
import de.mhus.app.reactive.model.ui.IEngine;

public class CaseActionList {

    private IEngine engine;
    private String caseId;
    private MProperties list;

    public CaseActionList() {}

    public CaseActionList(IEngine engine, String caseId) {
        this.engine = engine;
        this.caseId = caseId;

        try {
            list = engine.onUserCaseAction(caseId, EngineConst.ACTION_LIST, null);
            if (list == null || list.size() == 0) {
                list = null;
                return;
            }
        } catch (Throwable t) {
            t.printStackTrace(); // TODO
            list = null;
        }
    }

    public Set<String> getNames() {
        return new TreeSet<>(list.keys());
    }

    public String getTitle(String action) {
        String desc = list.getString(action, null);
        if (desc == null || desc.length() == 0) return null;
        return desc;
    }

    public IFormInformation getForm(String action) {
        String desc = list.getString(action, null);
        if (desc == null || desc.length() == 0) return null;
        try {
            MProperties values = new MProperties();
            values.put("action", action);
            MProperties ret = engine.onUserCaseAction(caseId, EngineConst.ACTION_FORM, values);
            if (ret == null || !ret.containsKey("form")) return null;
            DefRoot root = ModelUtil.fromJson(ret.getString("form", ""));
            return new DefaultFormInformation(root, null, null);
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    public MProperties onCaseAction(String action, MProperties properties) throws Exception {
        return engine.onUserCaseAction(caseId, action, properties);
    }

    public boolean isValid() {
        return list != null;
    }
}
