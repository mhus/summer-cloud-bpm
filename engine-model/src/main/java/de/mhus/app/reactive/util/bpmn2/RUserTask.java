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

import java.util.Map.Entry;

import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.core.form.ActionHandler;
import org.summerclouds.common.core.form.FormControl;
import org.summerclouds.common.core.node.IProperties;
import org.summerclouds.common.core.node.MProperties;
import org.summerclouds.common.core.pojo.Hidden;
import org.summerclouds.common.core.pojo.PojoAttribute;
import org.summerclouds.common.core.pojo.PojoModel;

import de.mhus.app.reactive.model.activity.AUserTask;
import de.mhus.app.reactive.model.annotations.PropertyDescription;
import de.mhus.app.reactive.model.engine.PCase;
import de.mhus.app.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.app.reactive.model.engine.PNode.TYPE_NODE;
import de.mhus.app.reactive.model.util.ActivityUtil;
import de.mhus.app.reactive.util.activity.RAbstractTask;

/*man bpmn
 * User task. Handles Form activities.
 */
public abstract class RUserTask<P extends RPool<?>> extends RAbstractTask<P>
        implements AUserTask<P> {

    public static final String FORM_CUSTOM_ID = "_customId";
    public static final String FORM_CUSTOMER_ID = "_customerId";
    public static final String FORM_MILESTONE = "_milestone";
    public static final String FORM_CASE_ID = "_caseId";

    @Override
    public void initializeActivity() throws Exception {
        getContext().getPNode().setState(STATE_NODE.WAITING);
        getContext().getPNode().setType(TYPE_NODE.USER);
    }

    @Override
    public String doExecute() {
        return null;
    }

    @Override
    @Hidden
    public IProperties getFormValues() throws MException {

        //		DefRoot form = createForm().build().getRoot();

        P pool = getContext().getPool();
        PojoModel modelTask = ActivityUtil.createFormPojoModel(getClass());
        PojoModel modelPool = ActivityUtil.createFormPojoModel(pool.getClass());
        MProperties out = new MProperties();

        for (PojoAttribute<?> attr : modelTask) {
            String name = attr.getName();
            PropertyDescription desc = attr.getAnnotation(PropertyDescription.class);
            if (desc == null) continue;
            if (!desc.readable()) continue;
            if (!desc.writable()) {
                out.put(name + ".editable", false);
            }
            try {
                out.put(name, attr.get(this));
            } catch (Throwable t) {
                log().w("get node attr {1} failed", this, attr, t);
            }
        }

        for (PojoAttribute<?> attr : modelPool) {
            String name = attr.getName();
            if (out.containsKey(name)) continue;
            PropertyDescription desc = attr.getAnnotation(PropertyDescription.class);
            if (desc == null) continue;
            if (!desc.readable()) continue;
            if (!desc.writable()) {
                out.put(name + ".editable", false);
            }
            try {
                out.put(name, attr.get(pool));
            } catch (Throwable t) {
                log().w("get pool attr {1} failed", this, attr, t);
            }
        }

        // add default values
        {
            PCase caze = getContext().getPCase();
            out.setString(FORM_CUSTOM_ID, caze.getCustomId());
            out.setString(FORM_CUSTOMER_ID, caze.getCustomerId());
            out.setString(FORM_MILESTONE, caze.getMilestone());
            out.setString(FORM_CASE_ID, caze.getId().toString());
        }

        /*
        		// return only in the form defined values
        		for (IDefDefinition item : form.definitions()) {
        			if (item instanceof FmElement) {
        				FmElement ele = (FmElement)item;
        				String name = ele.getProperty("name");
        				String namePrefix = name + ".";
        				// first pool
        				for (PojoAttribute<?> attr : modelPool) {
        					String aName = attr.getName();
        					if (aName.equals(name) || aName.startsWith(namePrefix)) {
        						try {
        							out.put(attr.getName(), attr.get(pool));
        							if (aName.equals(name)) {
        								PropertyDescription desc = attr.getAnnotation(PropertyDescription.class);
        								if (!desc.writable()) {
        									out.put(name + ".editable", false);
        								}
        							}
        						} catch (Throwable t) {
        							log().w(this,attr,t);
        						}
        					}
        				}
        				//overwrite with task
        				for (PojoAttribute<?> attr : modelTask) {
        					String aName = attr.getName();
        					if (aName.equals(name) || aName.startsWith(namePrefix)) {
        						try {
        							out.put(attr.getName(), attr.get(this));
        							if (aName.equals(name)) {
        								PropertyDescription desc = attr.getAnnotation(PropertyDescription.class);
        								if (!desc.writable()) {
        									out.put(name + ".editable", false);
        								}
        							}
        						} catch (Throwable t) {
        							log().w(this,attr,t);
        						}
        					}
        				}

        			}
        		}
        */
        return out;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void doSubmit(IProperties values) throws MException {
        P pool = getContext().getPool();
        PojoModel modelTask = ActivityUtil.createFormPojoModel(getClass());
        PojoModel modelPool = ActivityUtil.createFormPojoModel(pool.getClass());

        // DefRoot form = getForm().build();

        for (Entry<String, Object> entry : values.entrySet()) {
            String name = entry.getKey();
            if (name.startsWith("_")) // ignore internal
            continue;

            Object value = entry.getValue();
            if (modelTask.hasAttribute(name)) {
                PojoAttribute<Object> attr = modelTask.getAttribute(name);
                PropertyDescription desc = attr.getAnnotation(PropertyDescription.class);
                if (desc.writable()) {
                    try {
                        attr.set(this, value, false);
                    } catch (Throwable t) {
                        log().w("set {2} to node failed", this, attr, t);
                    }
                }
            } else if (modelPool.hasAttribute(name)) {
                PojoAttribute<Object> attr = modelPool.getAttribute(name);
                PropertyDescription desc = attr.getAnnotation(PropertyDescription.class);
                if (desc.writable()) {
                    try {
                        attr.set(pool, value, false);
                    } catch (Throwable t) {
                        log().w("set {2} to pool failed", this, attr, t);
                    }
                }
            }
        }

        doSubmit();
    }

    protected abstract void doSubmit() throws MException;

    @Override
    public MProperties doAction(String action, IProperties values) {
        return null;
    }

    @Override
    public Class<? extends FormControl> getFormControl() {
        return null;
    }

    @Override
    public Class<? extends ActionHandler> getActionHandler() {
        return null;
    }
}
