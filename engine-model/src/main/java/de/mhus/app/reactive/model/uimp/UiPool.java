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
import java.lang.reflect.InvocationTargetException;

import org.summerclouds.common.core.form.IFormInformation;
import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.core.node.MProperties;
import org.summerclouds.common.core.tool.MSystem;

import de.mhus.app.reactive.model.annotations.PoolDescription;
import de.mhus.app.reactive.model.ui.IPool;
import de.mhus.app.reactive.model.util.NoForm;

public class UiPool extends MLog implements IPool, Externalizable {

    private static final long serialVersionUID = 1L;
    private MProperties properties = null;
    private String pUri;
    private PoolDescription pd;

    public UiPool() {}

    public UiPool(String pUri, PoolDescription pd, MProperties properties) {
        this.pUri = pUri;
        this.pd = pd;
        this.properties = properties;
    }

    @Override
    public String getDisplayName() {
        return properties.getString(pUri + "#displayName", null);
    }

    @Override
    public String getDescription() {
        return properties.getString(pUri + "#description", null);
    }

    @Override
    public IFormInformation getInitialForm() {
        Class<? extends IFormInformation> form = pd.initialForm();
        if (form == null || form.getCanonicalName().equals(NoForm.class.getCanonicalName()))
            return null;
        try {
            return form.getDeclaredConstructor().newInstance();
        } catch (InstantiationException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException
                | NoSuchMethodException
                | SecurityException e) {
            log().e(e);
        }
        return null;
    }

    @Override
    public IFormInformation getDisplayForm() {
        Class<? extends IFormInformation> form = pd.displayForm();
        if (form == null || form.getCanonicalName().equals(NoForm.class.getCanonicalName()))
            return null;
        try {
            return form.getDeclaredConstructor().newInstance();
        } catch (InstantiationException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException
                | NoSuchMethodException
                | SecurityException e) {
            log().e(e);
        }
        return null;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(1);
        out.writeObject(pUri);
        out.writeObject(pd);
        out.writeObject(properties);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        if (in.readInt() != 1) throw new IOException("Wrong object version");
        pUri = (String) in.readObject();
        pd = (PoolDescription) in.readObject();
        properties = (MProperties) in.readObject();
    }

    @Override
    public String toString() {
        return MSystem.toString(this, pUri);
    }
}
