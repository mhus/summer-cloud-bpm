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
package de.mhus.app.reactive.model.uimp;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.summerclouds.common.core.error.AlreadyBoundException;
import org.summerclouds.common.core.form.ActionHandler;
import org.summerclouds.common.core.form.DefRoot;
import org.summerclouds.common.core.form.FormControl;
import org.summerclouds.common.core.form.IFormInformation;
import org.summerclouds.common.core.tool.MSpring;
import org.summerclouds.common.core.tool.MSystem;
import org.summerclouds.common.core.util.LocalClassLoader;

public class UiFormInformation implements IFormInformation, Externalizable {

    private static final long serialVersionUID = 1L;
    private DefRoot form;
    private Class<? extends ActionHandler> actionHandler;
    private Class<? extends FormControl> formControl;

    public UiFormInformation() {}

    public UiFormInformation(
            DefRoot form,
            Class<? extends ActionHandler> actionHandler,
            Class<? extends FormControl> formControl) {
        this.form = form;
        this.actionHandler = actionHandler;
        this.formControl = formControl;
    }

    @Override
    public DefRoot getForm() {
        return form;
    }

    @Override
    public Class<? extends ActionHandler> getActionHandler() {
        return actionHandler;
    }

    @Override
    public Class<? extends FormControl> getFormControl() {
        return formControl;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(1);
        out.writeObject(form);
        if (actionHandler != null) {
            out.writeObject(actionHandler.getCanonicalName());
            out.writeObject(MSystem.getBytes(actionHandler));
        } else out.writeObject(null);
        if (formControl != null) {
            out.writeObject(formControl.getCanonicalName());
            out.writeObject(MSystem.getBytes(formControl));
        } else out.writeObject(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        if (in.readInt() != 1) throw new IOException("Wrong object version");
        form = (DefRoot) in.readObject();
        {
            String name = (String) in.readObject();
            if (name != null) {
                byte[] code = (byte[]) in.readObject();
                try {
                    LocalClassLoader cl = new LocalClassLoader(MSpring.getDefaultActivator());
                    cl.addClassCode(name, code);
                    actionHandler = (Class<? extends ActionHandler>) cl.loadClass(name);
                } catch (AlreadyBoundException e) {
                    throw new IOException("ActionHandler: " + name, e);
                }
            }
        }
        {
            String name = (String) in.readObject();
            if (name != null) {
                byte[] code = (byte[]) in.readObject();
                try {

                    LocalClassLoader cl = new LocalClassLoader(MSpring.getDefaultActivator());
                    cl.addClassCode(name, code);
                    formControl = (Class<? extends FormControl>) cl.loadClass(name);
                } catch (AlreadyBoundException e) {
                    throw new IOException("FormControl: " + name, e);
                }
            }
        }
    }
}
