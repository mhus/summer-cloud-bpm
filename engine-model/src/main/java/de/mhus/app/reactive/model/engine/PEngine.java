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
package de.mhus.app.reactive.model.engine;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.summerclouds.common.core.tool.MCollection;
import org.summerclouds.common.core.tool.MString;

public class PEngine implements Externalizable {

    protected Map<String, String> parameters;
    private StorageProvider storage;

    //    public PEngine() {
    //        parameters = new HashMap<>();
    //    }

    public PEngine(StorageProvider storage) throws IOException {
        this.storage = storage;
        reload();
    }

    public void reload() throws IOException {
        synchronized (this) {
            if (storage != null) parameters = storage.loadEngine();
        }
    }

    //    public PEngine(PEngine clone) {
    //        parameters = new HashMap<>(clone.getParameters());
    //    }

    public Map<String, String> getParameters() {
        synchronized (this) {
            if (parameters == null) parameters = new HashMap<>();
            return parameters;
        }
    }

    @Override
    public String toString() {
        return "Engine: " + parameters;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(1);
        out.writeObject(parameters);
        out.flush();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int version = in.readInt();
        if (version != 1) throw new IOException("Wrong version: " + version);
        parameters = (Map<String, String>) in.readObject();
    }

    /**
     * Enabled means the version can be instantiated.
     *
     * @param name
     * @return true if it's allowed
     */
    public boolean isProcessEnabled(String name) {
        String v = MString.afterIndex(name, ':');
        String n = MString.beforeIndex(name, ':');
        String[] versions =
                String.valueOf(getParameters().getOrDefault("process:" + n + ":versions", ""))
                        .split(",");
        return MCollection.contains(versions, v);
    }

    public boolean isProcessEnabled(String name, String version) {
        String[] versions =
                String.valueOf(getParameters().getOrDefault("process:" + name + ":versions", ""))
                        .split(",");
        return MCollection.contains(versions, version);
    }

    public void enableProcessVersion(String deployedName) throws IOException {
        // add version
        String v = MString.afterIndex(deployedName, ':');
        String n = MString.beforeIndex(deployedName, ':');
        String[] versions =
                String.valueOf(getParameters().getOrDefault("process:" + n + ":versions", ""))
                        .split(",");
        if (!MCollection.contains(versions, v)) {
            versions = MCollection.append(versions, v);
            set("process:" + n + ":versions", MString.join(versions, ','));
        }
    }

    public void disableProcessVersion(String deployedName) throws IOException {
        // add version
        String v = MString.afterIndex(deployedName, ':');
        String n = MString.beforeIndex(deployedName, ':');
        String[] versions =
                String.valueOf(getParameters().getOrDefault("process:" + n + ":versions", ""))
                        .split(",");
        int pos = MCollection.indexOf(versions, v);
        if (pos > -1) {
            versions = MCollection.remove(versions, pos, 1);
            set("process:" + n + ":versions", MString.join(versions, ','));
        }
    }

    /**
     * Active means this is the default version for a new process instance
     *
     * @param deployedName
     * @throws IOException
     */
    public void activateProcessVersion(String deployedName) throws IOException {
        String v = MString.afterIndex(deployedName, ':');
        String n = MString.beforeIndex(deployedName, ':');
        set("process:" + n + ":active", v);
    }

    public void deactivateProcessVersion(String deployedName) throws IOException {
        String v = MString.afterIndex(deployedName, ':');
        String n = MString.beforeIndex(deployedName, ':');
        String cur = get("process:" + n + ":active");
        if (v.equals(cur)) set("process:" + n + ":active", null);
    }

    public String getActiveProcessVersion(String processName) {
        if (processName.indexOf(':') >= 0) processName = MString.beforeIndex(processName, ':');
        return String.valueOf(getParameters().get("process:" + processName + ":active"));
    }

    public boolean isProcessActive(String deployedName) {
        String v = MString.afterIndex(deployedName, ':');
        String n = MString.beforeIndex(deployedName, ':');
        return v.equals(String.valueOf(getParameters().get("process:" + n + ":active")));
    }

    public String get(String key) throws IOException {
        synchronized (this) {
            String value = storage.getEngineValue(key);
            if (value == null) parameters.remove(key);
            else parameters.put(key, value);
            return value;
        }
    }

    public String set(String key, String value) throws IOException {
        synchronized (this) {
            if (value == null) {
                storage.deleteEngineValue(key);
                parameters.remove(key);
            } else {
                storage.setEngineValue(key, value);
                parameters.put(key, value);
            }
            return value;
        }
    }

    public void save() throws IOException {
        save(storage);
    }

    public void save(StorageProvider storage) throws IOException {
        synchronized (this) {
            for (Entry<String, String> entry : parameters.entrySet()) {
                storage.setEngineValue(entry.getKey(), entry.getValue());
            }
            for (String key : storage.loadEngine().keySet()) {
                if (!parameters.containsKey(key)) storage.deleteEngineValue(key);
            }
        }
    }
}
