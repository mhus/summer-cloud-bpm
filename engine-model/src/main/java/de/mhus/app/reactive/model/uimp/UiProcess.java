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
import java.util.Locale;

import org.summerclouds.common.core.node.MProperties;
import org.summerclouds.common.core.tool.MSystem;

import de.mhus.app.reactive.model.engine.EngineConst;
import de.mhus.app.reactive.model.ui.IProcess;

public class UiProcess implements IProcess, Externalizable {

    private static final long serialVersionUID = 1L;
    private MProperties properties = null;
    private Locale locale;

    public UiProcess() {}

    public UiProcess(MProperties properties, Locale locale) {
        this.properties = properties;
        this.locale = locale;
    }

    @Override
    public String getDisplayName(String uri, String canonicalName) {
        if (locale != null) {
            String out =
                    properties.getString(
                            uri
                                    + (canonicalName == null ? "" : "/" + canonicalName)
                                    + "#displayName?"
                                    + locale.getLanguage(),
                            null);
            if (out != null) return out;
        }
        return properties.getString(
                uri + (canonicalName == null ? "" : "/" + canonicalName) + "#displayName",
                canonicalName);
    }

    @Override
    public String getDescription(String uri, String canonicalName) {
        if (locale != null) {
            String out =
                    properties.getString(
                            uri
                                    + (canonicalName == null ? "" : "/" + canonicalName)
                                    + "#description?"
                                    + locale.getLanguage(),
                            null);
            if (out != null) return out;
        }
        return properties.getString(
                uri + (canonicalName == null ? "" : "/" + canonicalName) + "#description", "");
    }

    public MProperties getProperties() {
        return properties;
    }

    //	@Override
    //	public String getIndexDisplayName(int index, String uri, String canonicalName) {
    //		Locale locale = engine.getLocale();
    //		if (locale != null) {
    //			String out = properties.getString(uri + (canonicalName == null ? "" : "/" + canonicalName)
    // + "#index"+index+"?" + locale.getLanguage(), null);
    //			if (out != null) return out;
    //		}
    //		return properties.getString(uri + (canonicalName == null ? "" : "/" + canonicalName) +
    // "#index"+index, "Index" + index);
    //	}

    @Override
    public String getPropertyName(String uri, String canonicalName, String property) {
        if (property.startsWith(EngineConst.UI_CASE_PREFIX)) canonicalName = null;
        if (locale != null) {
            String out =
                    properties.getString(
                            uri
                                    + (canonicalName == null ? "" : "/" + canonicalName)
                                    + "#"
                                    + property
                                    + "?"
                                    + locale.getLanguage(),
                            null);
            if (out != null) return out;
        }
        return properties.getString(
                uri + (canonicalName == null ? "" : "/" + canonicalName) + "#" + property,
                property);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(1);
        out.writeObject(locale);
        out.writeObject(properties);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        if (in.readInt() != 1) throw new IOException("Wrong object version");
        locale = (Locale) in.readObject();
        properties = (MProperties) in.readObject();
    }

    @Override
    public String toString() {
        return MSystem.toString(this, properties);
    }
}
