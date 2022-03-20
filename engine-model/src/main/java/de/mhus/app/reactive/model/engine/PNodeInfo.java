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

import de.mhus.app.reactive.model.engine.PNode.STATE_NODE;
import de.mhus.app.reactive.model.engine.PNode.TYPE_NODE;

public class PNodeInfo implements Externalizable {

    private UUID id;
    private UUID caseId;
    private String canonicalName;
    private String assigned;
    private STATE_NODE state;
    private TYPE_NODE type;
    private String customId;
    private String uri;
    private String[] indexValues;
    private String customerId;
    private long created;
    private long modified;
    private int priority;
    private int score;
    private String actor;
    private long due;

    public PNodeInfo() {}

    public PNodeInfo(
            UUID id,
            UUID caseId,
            String canonicalName,
            String assigned,
            STATE_NODE state,
            TYPE_NODE type,
            String uri,
            String customId,
            String customerId,
            long created,
            long modified,
            int priority,
            int score,
            String actor,
            long due,
            String[] indexValues) {
        this.id = id;
        this.caseId = caseId;
        this.canonicalName = canonicalName;
        this.assigned = assigned;
        this.state = state;
        this.type = type;
        this.uri = uri;
        this.customId = customId;
        this.customerId = customerId;
        this.indexValues = indexValues;
        this.created = created;
        this.modified = modified;
        this.priority = priority;
        this.score = score;
        this.actor = actor;
        this.due = due;
    }

    public PNodeInfo(PCaseInfo cazeInfo, PNode node) {
        this(
                node.getId(),
                node.getCaseId(),
                node.getCanonicalName(),
                node.getAssignedUser(),
                node.getState(),
                node.getType(),
                cazeInfo.getUri(),
                cazeInfo.getCustomId(),
                cazeInfo.getCustomerId(),
                0,
                0,
                0,
                0,
                node.getActor(),
                node.getDue(),
                null);
    }

    public UUID getId() {
        return id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public String getAssigned() {
        return assigned;
    }

    public STATE_NODE getState() {
        return state;
    }

    public TYPE_NODE getType() {
        return type;
    }

    @Override
    public String toString() {
        return "PNodeInfo:"
                + id
                + " "
                + caseId
                + " "
                + canonicalName
                + " "
                + assigned
                + " "
                + state
                + " "
                + type;
    }

    public String getUri() {
        return uri;
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

    public String getActor() {
        return actor;
    }

    public long getDue() {
        return due;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(2);
        out.writeObject(id);
        out.writeObject(caseId);
        out.writeObject(canonicalName);
        out.writeObject(assigned);
        out.writeObject(state);
        out.writeObject(type);
        out.writeObject(customId);
        out.writeObject(uri);
        out.writeObject(indexValues);
        out.writeObject(customerId);
        out.writeLong(created);
        out.writeLong(modified);
        out.writeInt(priority);
        out.writeInt(score);
        out.writeObject(actor);
        out.writeLong(due);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int v = in.readInt();
        if (v < 1 || v > 2) throw new IOException("Wrong object version");
        id = (UUID) in.readObject();
        caseId = (UUID) in.readObject();
        canonicalName = (String) in.readObject();
        assigned = (String) in.readObject();
        state = (STATE_NODE) in.readObject();
        type = (TYPE_NODE) in.readObject();
        customId = (String) in.readObject();
        uri = (String) in.readObject();
        indexValues = (String[]) in.readObject();
        customerId = (String) in.readObject();
        created = in.readLong();
        modified = in.readLong();
        priority = in.readInt();
        score = in.readInt();
        actor = (String) in.readObject();
        if (v >= 2) {
            due = in.readLong();
        } else {
            due = 0;
        }
    }
}
