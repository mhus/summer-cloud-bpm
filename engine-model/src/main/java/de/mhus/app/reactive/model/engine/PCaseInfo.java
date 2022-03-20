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
import java.util.UUID;

import de.mhus.app.reactive.model.engine.PCase.STATE_CASE;

public class PCaseInfo implements Externalizable {

    private UUID id;
    private String uri;
    private String canonicalName;
    private STATE_CASE state;
    private String customId;
    private String[] indexValues;
    private String customerId;
    private long created;
    private long modified;
    private int priority;
    private int score;
    private String milestone;

    public PCaseInfo() {}

    public PCaseInfo(
            UUID id,
            String uri,
            String canonicalName,
            STATE_CASE state,
            String customId,
            String customerId,
            long created,
            long modified,
            int priority,
            int score,
            String[] indexValues,
            String milestone) {
        this.id = id;
        this.uri = uri;
        this.canonicalName = canonicalName;
        this.state = state;
        this.customId = customId;
        this.customerId = customerId;
        this.indexValues = indexValues;
        this.created = created;
        this.modified = modified;
        this.priority = priority;
        this.score = score;
        this.milestone = milestone;
    }

    public PCaseInfo(PCase caze) {
        this(
                caze.getId(),
                caze.getUri(),
                caze.getCanonicalName(),
                caze.getState(),
                caze.getCustomId(),
                caze.getCustomerId(),
                0,
                0,
                0,
                0,
                null,
                caze.getMilestone());
    }

    public UUID getId() {
        return id;
    }

    public String getUri() {
        return uri;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public STATE_CASE getState() {
        return state;
    }

    public String getCustomId() {
        return customId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getIndexValue(int index) {
        if (indexValues == null || index < 0 || index >= indexValues.length) return null;
        return indexValues[index];
    }

    public long getCreated() {
        return created;
    }

    public long getModified() {
        return modified;
    }

    public int getPriority() {
        return priority;
    }

    public int getScore() {
        return score;
    }

    public String getMilestone() {
        return milestone;
    }

    @Override
    public String toString() {
        return "PCaseInfo:" + id + "," + state + "," + uri;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(1);
        out.writeObject(id);
        out.writeObject(uri);
        out.writeObject(canonicalName);
        out.writeObject(state);
        out.writeObject(customId);
        out.writeObject(indexValues);
        out.writeObject(customerId);
        out.writeLong(created);
        out.writeLong(modified);
        out.writeInt(priority);
        out.writeInt(score);
        out.writeObject(milestone);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        if (in.readInt() != 1) throw new IOException("Wrong object version");
        id = (UUID) in.readObject();
        uri = (String) in.readObject();
        canonicalName = (String) in.readObject();
        state = (STATE_CASE) in.readObject();
        customId = (String) in.readObject();
        indexValues = (String[]) in.readObject();
        customerId = (String) in.readObject();
        created = in.readLong();
        modified = in.readLong();
        priority = in.readInt();
        score = in.readInt();
        milestone = (String) in.readObject();
    }
}
