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
package de.mhus.app.reactive.util.bpmn2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.summerclouds.common.core.error.UsageException;
import org.summerclouds.common.core.form.DefRoot;
import org.summerclouds.common.core.form.IFormInformation;
import org.summerclouds.common.core.form.ModelUtil;
import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.core.node.IProperties;
import org.summerclouds.common.core.node.MProperties;
import org.summerclouds.common.core.pojo.Action;
import org.summerclouds.common.core.pojo.PojoAction;
import org.summerclouds.common.core.pojo.PojoAttribute;
import org.summerclouds.common.core.pojo.PojoModel;
import org.summerclouds.common.core.tool.MCollection;
import org.summerclouds.common.core.tool.MJson;

import de.mhus.app.reactive.model.activity.AActivity;
import de.mhus.app.reactive.model.activity.APool;
import de.mhus.app.reactive.model.annotations.ActionForm;
import de.mhus.app.reactive.model.annotations.PropertyDescription;
import de.mhus.app.reactive.model.engine.ContextRecipient;
import de.mhus.app.reactive.model.engine.EngineConst;
import de.mhus.app.reactive.model.engine.ProcessContext;
import de.mhus.app.reactive.model.util.ActivityUtil;
import de.mhus.app.reactive.model.util.NoForm;

/*man bpmn
 * Implementation of a pool. The pool will serialize the variables defined with PropertyDescription
 * to / from database.
 */
public abstract class RPool<P extends APool<?>> extends MLog implements APool<P>, ContextRecipient {

    private PojoModel pojoModel;
    protected ProcessContext<?> context;

    @Override
    public Map<String, Object> exportParameters() {
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

    /** import all parameters, convert all keys to lower case to be compatible to the pojo model */
    @Override
    public void importParameters(Map<String, Object> parameters) {
        importParameters(parameters, false);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void importParameters(Map<String, Object> parameters, boolean initial) {
        parameters = MCollection.toLowerCaseKeys(parameters);

        for (PojoAttribute attr : getPojoModel()) {
            try {
                PropertyDescription desc =
                        (PropertyDescription) attr.getAnnotation(PropertyDescription.class);
                if (desc != null && (!initial || desc.initial())) {
                    Object value = parameters.get(attr.getName());
                    if (value != null) {
                        attr.set(this, value, false);
                    } else {
                        if (initial && desc.mandatory())
                            throw new UsageException("parameter is mandatory", attr);
                    }
                }
            } catch (IOException e) {
                log().d("import parameter {1} failed", attr, e);
            }
        }
    }

    @Override
    public void initializeCase(Map<String, Object> parameters) throws Exception {
        checkInputParameters(parameters);
        importParameters(parameters, true);
        checkStartCase();
    }

    /** Check if the case can be started. All parameters will be set already. */
    protected void checkStartCase() throws Exception {};

    /**
     * Check and manipulate incoming parameters before they are written to the case. Throw an
     * exception if the parameters are not valid.
     *
     * @param parameters
     * @throws Exception
     */
    protected abstract void checkInputParameters(Map<String, Object> parameters) throws Exception;

    @Override
    public void closeCase() {}

    public synchronized PojoModel getPojoModel() {
        if (pojoModel == null) pojoModel = ActivityUtil.createPojoModel(this.getClass());
        return pojoModel;
    }

    @Override
    public void setContext(ProcessContext<?> context) {
        this.context = context;
    }

    @Override
    public void beforeExecute(AActivity<?> activity) {}

    @Override
    public void afterExecute(AActivity<?> activity) {}

    @Override
    public MProperties onUserCaseAction(String action, IProperties values) {
        try {
            if (EngineConst.ACTION_LIST.equals(action)) {
                return onUserActionList(values);
            } else if (EngineConst.ACTION_FORM.equals(action)) {
                action = values.getString("action");
                return onUserActionForm(action, values);
            }
            if (!isUserActionAllowed(action)) {
                log().d("action is not allowed", this, action);
                return null;
            }
            PojoModel model = getPojoModel();
            PojoAction method = model.getAction(action);
            Object ret = null;
            if (method == null || method.getParameterType() == null) {
                // nothing
            } else if (method.getParameterType().length == 2)
                ret = method.doExecute(this, context, values);
            else if (method.getParameterType().length == 1) ret = method.doExecute(this, values);
            else {
                log().e("onUserCaseAction", this, action, "wrong number of arguments", method);
                return null;
            }
            return (MProperties) ret;
        } catch (Throwable t) {
            log().e("onUserCaseAction", this, action, t);
            return null;
        }
    }

    protected MProperties onUserActionForm(String action, IProperties values) {
        PojoModel model = getPojoModel();
        PojoAction method = model.getAction(action);
        try {
            ActionForm actionForm = method.getAnnotation(ActionForm.class);
            if (actionForm != null && actionForm.value() != NoForm.class) {
                IFormInformation formInfo = actionForm.value().getConstructor().newInstance();
                DefRoot form = formInfo.getForm();
                if (form != null) {
                    form.build();
                    String formStr = MJson.toString(ModelUtil.toJson(form));
                    MProperties ret = new MProperties();
                    ret.setString("form", formStr);
                    return ret;
                }
            }
        } catch (Throwable t) {
            log().e("user action {2} failed", this, action, values, t);
        }
        return null;
    }

    protected MProperties onUserActionList(IProperties values) {
        MProperties ret = new MProperties();
        try {
            PojoModel model = getPojoModel();
            for (String name : model.getActionNames()) {
                if (isUserActionAllowed(name)) {
                    String title = getUserActionTitle(model, name);
                    ret.setString(name, title);
                }
            }
        } catch (Throwable t) {
            log().e("user action failed", this, values, t);
        }
        return ret;
    }

    protected String getUserActionTitle(PojoModel model, String name) {
        Action action = model.getAction(name).getAnnotation(Action.class);
        if (action == null || action.title().length() == 0) return name;
        return action.title();
    }

    /**
     * Overwrite to check if a action is allowed for the current user and in the current state of
     * the case.
     *
     * @param name
     * @return
     */
    protected boolean isUserActionAllowed(String name) {
        return true;
    }
}
