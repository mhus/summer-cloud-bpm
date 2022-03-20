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
import java.util.Map;
import java.util.UUID;

import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.core.pojo.Public;
import org.summerclouds.common.core.tool.MSystem;

import de.mhus.app.reactive.model.engine.PCase.STATE_CASE;
import de.mhus.app.reactive.model.engine.PCaseInfo;
import de.mhus.app.reactive.model.ui.ICase;

public class UiCase extends MLog implements ICase, Externalizable {

    private static final long serialVersionUID = 1L;
    private PCaseInfo info;
    private Map<String, String> properties;

    public UiCase() {}

    public UiCase(PCaseInfo info, Map<String, String> properties) {
        this.info = info;
        this.properties = properties;
    }

    @Override
    @Public
    public String getUri() {
        return info.getUri();
    }

    @Override
    @Public
    public String getCanonicalName() {
        return info.getCanonicalName();
    }

    @Override
    @Public
    public String getCustomId() {
        return info.getCustomId();
    }

    @Override
    @Public
    public STATE_CASE getState() {
        return info.getState();
    }

    @Override
    @Public
    public UUID getId() {
        return info.getId();
    }

    @Override
    @Public
    public String getCustomerId() {
        return info.getCustomerId();
    }

    @Override
    @Public
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    @Public
    public long getCreated() {
        return info.getCreated();
    }

    @Override
    @Public
    public long getModified() {
        return info.getModified();
    }

    @Override
    @Public
    public int getPriority() {
        return info.getPriority();
    }

    @Override
    @Public
    public int getScore() {
        return info.getScore();
    }

    @Override
    @Public
    public String getMilestone() {
        return info.getMilestone();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(1);
        out.writeObject(info);
        out.writeObject(properties);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        if (in.readInt() != 1) throw new IOException("Wrong object version");
        info = (PCaseInfo) in.readObject();
        properties = (Map<String, String>) in.readObject();
    }

    @Override
    public String toString() {
        return MSystem.toString(this, info);
    }
}
