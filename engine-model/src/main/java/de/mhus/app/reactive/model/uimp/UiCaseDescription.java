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

import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.core.tool.MSystem;

import de.mhus.app.reactive.model.ui.ICaseDescription;
import de.mhus.app.reactive.model.ui.IProcess;

public class UiCaseDescription extends MLog implements ICaseDescription, Externalizable {

    private static final long serialVersionUID = 1L;
    private IProcess process;
    private String uri;

    public UiCaseDescription() {}

    public UiCaseDescription(String uri, IProcess process) {
        this.uri = uri;
        this.process = process;
    }

    @Override
    public String getDisplayName() {
        return process.getDisplayName(uri, null);
    }

    @Override
    public String getDescription() {
        return process.getDescription(uri, null);
    }

    @Override
    public String getPropertyName(String property) {
        return process.getPropertyName(uri, null, property);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(1);
        out.writeObject(uri);
        out.writeObject(process);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        if (in.readInt() != 1) throw new IOException("Wrong object version");
        uri = (String) in.readObject();
        process = (IProcess) in.readObject();
    }

    @Override
    public String toString() {
        return MSystem.toString(this, uri);
    }
}
