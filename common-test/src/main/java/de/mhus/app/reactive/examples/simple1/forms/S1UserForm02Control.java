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
package de.mhus.app.reactive.examples.simple1.forms;

import java.util.Date;

import org.summerclouds.common.core.form.DataSource;
import org.summerclouds.common.core.form.FormControlAdapter;
import org.summerclouds.common.core.form.UiComponent;
import org.summerclouds.common.core.tool.MDate;

public class S1UserForm02Control extends FormControlAdapter {

    @Override
    public boolean newValue(UiComponent component, Object newValue) {

        if (component.getName().equals("ctext1")) {
            try {
                UiComponent c = form.getBuilder().getComponent("ctext2");
                form.getDataSource().setObject(c, DataSource.VALUE, newValue);
                c.doUpdateValue();
            } catch (Throwable t) {
                log().e("set ctext1 failed", component, newValue, t);
            }
        } else if (component.getName().equals("ctext2")) {
            try {
                UiComponent c = form.getBuilder().getComponent("ctext1");
                form.getDataSource().setObject(c, DataSource.VALUE, newValue);
                c.doUpdateValue();
            } catch (Throwable t) {
                log().e("set ctext2 failed", component, newValue, t);
            }
        } else if (component.getName().equals("cgender")) {
            String v = String.valueOf(newValue);
            UiComponent vMale = form.getBuilder().getComponent("cmale");
            UiComponent vFemale = form.getBuilder().getComponent("cfemale");
            try {
                vMale.setVisible("MR".equals(v));
                vFemale.setVisible("MRS".equals(v));
            } catch (Throwable t) {
                log().e("set cgender failed", component, newValue, t);
            }
        }

        return super.newValue(component, newValue);
    }

    @Override
    public void setup() {
        super.setup();
        try {
            UiComponent vGender = form.getBuilder().getComponent("cgender");
            String v = form.getDataSource().getString(vGender, DataSource.VALUE, "");
            UiComponent vMale = form.getBuilder().getComponent("cmale");
            UiComponent vFemale = form.getBuilder().getComponent("cfemale");
            vMale.setVisible("MR".equals(v));
            vFemale.setVisible("MRS".equals(v));
        } catch (Throwable t) {
            log().e(t);
        }
    }

    @Override
    public void doAction(String action, Object... params) {
        if (action.equals("now")) {
            UiComponent v = form.getBuilder().getComponent("cnowtext");
            try {
                form.getDataSource()
                        .setObject(v, DataSource.VALUE, MDate.toDateTimeSecondsString(new Date()));
                v.doUpdateValue();
                return;
            } catch (Throwable t) {
                log().e(t);
            }
        }
        super.doAction(action, params);
    }
}
