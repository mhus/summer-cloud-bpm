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
import java.util.UUID;

import org.summerclouds.common.core.error.UsageException;
import org.summerclouds.common.core.tool.MPeriod;

public class PNode implements Externalizable {

    // NEW: before save to store
    // READY: ready to proceess
    // SCHEDULED: Waiting for the scheduleing time
    // WAITING: Waiting to be done, e.g. (Human Tasks, Messages, Signals)
    // FAILED: An error occured, Node is stopped
    // SUSPEDNED: Suspended by e.g. admin
    // STOPPED: Some kind of SUSPENDED
    // CLOSED: The task is done and can be archived with the case, needed for history

    public enum STATE_NODE {
        NEW,
        RUNNING,
        SCHEDULED,
        WAITING,
        FAILED,
        SUSPENDED,
        STOPPED,
        CLOSED,
        ZOMBIE,
        SEVERE
    };

    public enum TYPE_NODE {
        NODE,
        USER,
        RUNTIME,
        MESSAGE,
        SIGNAL,
        EXTERN
    };

    // ident of this flow node
    protected UUID id;
    // ident of the case
    private UUID caseId;
    // name of this low node (e.g. activity)
    protected String name;
    // class name of the node
    protected String canonicalName;
    // created
    protected long creationDate;
    // last running time
    protected long lastRunDate;
    // state
    protected STATE_NODE state = STATE_NODE.NEW;
    protected STATE_NODE startState = state;
    // state
    protected STATE_NODE suspendedState = STATE_NODE.NEW;
    // list of schedulers and the source, only the nearest scheduling time is needed to be triggered
    protected HashMap<String, Long> schedulers;
    // list of singnals to wait for
    protected HashMap<String, String> signalTriggers;
    // list of messages to wait for
    protected HashMap<String, String> messageTriggers;
    // if set to true the process will not switch to the next activities and enter STOPPED state
    protected boolean stopAfterExecute;
    // type
    protected TYPE_NODE type = TYPE_NODE.NODE;
    // Internal message to describe the exit reason, e.g. exception, event called ...
    protected String exitMessage;

    protected Map<String, Object> parameters;

    protected String assignedUser;

    protected UUID runtimeNode;

    protected int tryCount = EngineConst.DEFAULT_TRY_COUNT;
    private long activityTimeout = EngineConst.DEFAULT_ACTIVITY_TIMEOUT.value();
    private Map<String, Object> message;
    // will not be stored
    private String[] indexValues;
    private String actor;
    // due date
    private long due;

    public PNode() {}

    public PNode(
            UUID id,
            UUID caseId,
            String name,
            String canonicalName,
            long creationDate,
            long lastRunDate,
            STATE_NODE state,
            STATE_NODE suspendedState,
            HashMap<String, Long> schedulers,
            HashMap<String, String> signalTriggers,
            HashMap<String, String> messageTriggers,
            boolean stopAfterExecute,
            TYPE_NODE type,
            String exitMessage,
            Map<String, Object> parameters,
            String assignedUser,
            UUID runtimeNode,
            int tryCount,
            String actor,
            long dueDate) {
        super();
        this.id = id;
        this.caseId = caseId;
        this.name = name;
        this.canonicalName = canonicalName;
        this.creationDate = creationDate;
        this.lastRunDate = lastRunDate;
        this.state = state;
        this.startState = state;
        this.suspendedState = suspendedState;
        this.schedulers = schedulers;
        this.signalTriggers = signalTriggers;
        this.messageTriggers = messageTriggers;
        this.stopAfterExecute = stopAfterExecute;
        this.type = type;
        this.exitMessage = exitMessage;
        this.parameters = parameters;
        this.assignedUser = assignedUser;
        this.runtimeNode = runtimeNode;
        this.tryCount = tryCount;
        this.actor = actor;
        this.setDue(dueDate);
    }

    public PNode(PNode clone) {
        this.id = clone.getId();
        this.caseId = clone.getCaseId();
        this.name = clone.getName();
        this.canonicalName = clone.getCanonicalName();
        this.creationDate = clone.getCreationDate();
        this.lastRunDate = clone.getLastRunDate();
        this.state = clone.getState();
        this.startState = clone.getStartState();
        this.suspendedState = clone.getSuspendedState();
        this.schedulers = new HashMap<>(clone.getSchedulers());
        this.signalTriggers = new HashMap<>(clone.getSignalTriggers());
        this.messageTriggers = new HashMap<>(clone.getMessageTriggers());
        this.stopAfterExecute = clone.isStopAfterExecute();
        this.type = clone.getType();
        this.exitMessage = clone.getExitMessage();
        this.parameters = new HashMap<>(clone.getParameters());
        this.assignedUser = clone.getAssignedUser();
        this.runtimeNode = clone.getRuntimeId();
        this.tryCount = clone.getTryCount();
        if (clone.getMessage() != null) this.message = new HashMap<>(clone.getMessage());
        this.actor = clone.getActor();
        this.due = clone.getDue();
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public long getLastRunDate() {
        return lastRunDate;
    }

    public STATE_NODE getState() {
        return state;
    }

    public STATE_NODE getStartState() {
        return startState;
    }

    public STATE_NODE getSuspendedState() {
        return suspendedState;
    }

    public HashMap<String, Long> getSchedulers() {
        if (schedulers == null) schedulers = new HashMap<>();
        return schedulers;
    }

    public HashMap<String, String> getSignalTriggers() {
        if (signalTriggers == null) signalTriggers = new HashMap<>();
        return signalTriggers;
    }

    public HashMap<String, String> getMessageTriggers() {
        if (messageTriggers == null) messageTriggers = new HashMap<>();
        return messageTriggers;
    }

    public boolean isStopAfterExecute() {
        return stopAfterExecute;
    }

    public TYPE_NODE getType() {
        return type;
    }

    public String getExitMessage() {
        return exitMessage;
    }

    public Map<String, Object> getParameters() {
        if (parameters == null) parameters = new HashMap<>();
        return parameters;
    }

    public String getAssignedUser() {
        return assignedUser;
    }

    public UUID getRuntimeId() {
        return runtimeNode;
    }

    public Map.Entry<String, Long> getNextScheduled() {
        if (schedulers == null) return null;
        Map.Entry<String, Long> out = null;
        for (Entry<String, Long> entry : schedulers.entrySet())
            if (out == null || entry.getValue() < out.getValue()) out = entry;
        return out;
    }

    //	public Map.Entry<String, Long> getNextTriggerScheduled() {
    //		if (schedulers == null) return null;
    //		Map.Entry<String, Long> out = null;
    //		for (Entry<String, Long> entry : schedulers.entrySet())
    //			if (!entry.getKey().equals("") && (out == null || entry.getValue() < out.getValue()))
    //				out = entry;
    //		return out;
    //	}

    public String getSignalsAsString() {
        if (signalTriggers == null) return "";
        StringBuilder out = new StringBuilder();
        for (String sig : signalTriggers.values()) {
            sig = sig.replace(';', '_');
            out.append(";").append(sig);
        }
        if (out.length() > 0) out.append(";");
        return out.toString();
    }

    public String getMessagesAsString() {
        if (messageTriggers == null) return "";
        StringBuilder out = new StringBuilder();
        for (String msg : messageTriggers.values()) {
            msg = msg.replace(';', '_');
            out.append(";").append(msg);
        }
        if (out.length() > 0) out.append(";");
        return out.toString();
    }

    public static String getSignalAsString(String entity) {
        entity = entity.replace(';', '_');
        return ";" + entity + ";";
    }

    public static String getMessageAsString(String entity) {
        entity = entity.replace(';', '_');
        return ";" + entity + ";";
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public void setState(STATE_NODE state) {
        this.state = state;
    }

    @Override
    public String toString() {
        Entry<String, Long> next = getNextScheduled();
        String time = "now";
        if (state == STATE_NODE.CLOSED
                || state == STATE_NODE.FAILED
                || state == STATE_NODE.STOPPED
                || state == STATE_NODE.SUSPENDED) {
            time = "-";
        } else if (next != null)
            time =
                    next.getKey()
                            + "@"
                            + MPeriod.getIntervalAsString(
                                    (next.getValue() - System.currentTimeMillis()));
        return "PNode: " + getCanonicalName() + " " + state + " " + time + " " + id;
    }

    public void setScheduled(long scheduled) {
        if (scheduled < 0) getSchedulers().remove("");
        else getSchedulers().put("", scheduled);
    }

    public void setMessageEvent(String message) {
        if (message == null) getMessageTriggers().remove("");
        else getMessageTriggers().put("", message);
    }

    public void setSignalEvent(String message) {
        if (message == null) getSignalTriggers().remove("");
        else getSignalTriggers().put("", message);
    }

    public void setLastRunDate(long lastRunDate) {
        this.lastRunDate = lastRunDate;
    }

    public void setSuspendedState(STATE_NODE suspendedState) {
        this.suspendedState = suspendedState;
    }

    public void setStopAfterExecute(boolean stopAfterExecute) {
        this.stopAfterExecute = stopAfterExecute;
    }

    public void setType(TYPE_NODE type) {
        if (type == TYPE_NODE.RUNTIME) throw new UsageException("can't set type to RUNTIME");
        this.type = type;
    }

    public void setExitMessage(String exitMessage) {
        this.exitMessage = exitMessage;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public void setAssignedUser(String assignedUser) {
        this.assignedUser = assignedUser;
    }

    public void setRuntimeNode(UUID runtimeNode) {
        this.runtimeNode = runtimeNode;
    }

    public int getTryCount() {
        return tryCount;
    }

    public void setTryCount(int tryCount) {
        this.tryCount = tryCount;
    }

    public void updateStartState() {
        this.startState = state;
    }

    public long getActivityTimeout() {
        return activityTimeout;
    }

    public void setActivityTimeout(long activityTimeout) {
        this.activityTimeout = activityTimeout;
    }

    public void setMessage(Map<String, Object> message) {
        this.message = message;
    }

    public Map<String, Object> getMessage() {
        return message;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(2);

        out.writeObject(id);
        out.writeObject(caseId);
        out.writeObject(name);
        out.writeObject(canonicalName);
        out.writeLong(creationDate);

        out.writeLong(lastRunDate);
        out.writeObject(state);
        out.writeObject(suspendedState);
        out.writeObject(schedulers);
        out.writeObject(signalTriggers);

        out.writeObject(messageTriggers);
        out.writeBoolean(stopAfterExecute);
        out.writeObject(type);
        out.writeObject(exitMessage);
        out.writeObject(parameters);

        out.writeObject(assignedUser);
        out.writeObject(runtimeNode);
        out.writeInt(tryCount);
        out.writeObject(message);
        out.writeObject(actor);

        out.writeLong(due);

        out.flush();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        int version = in.readInt();
        if (version < 1 || version > 2) throw new IOException("Wrong version: " + version);

        id = (UUID) in.readObject();
        caseId = (UUID) in.readObject();
        name = (String) in.readObject();
        canonicalName = (String) in.readObject();
        creationDate = in.readLong();

        lastRunDate = in.readLong();
        state = (STATE_NODE) in.readObject();
        startState = state;
        suspendedState = (STATE_NODE) in.readObject();
        schedulers = (HashMap<String, Long>) in.readObject();
        signalTriggers = (HashMap<String, String>) in.readObject();

        messageTriggers = (HashMap<String, String>) in.readObject();
        stopAfterExecute = in.readBoolean();
        type = (TYPE_NODE) in.readObject();
        exitMessage = (String) in.readObject();
        parameters = (Map<String, Object>) in.readObject();

        assignedUser = (String) in.readObject();
        runtimeNode = (UUID) in.readObject();
        tryCount = in.readInt();
        message = (Map<String, Object>) in.readObject();
        actor = (String) in.readObject();

        if (version >= 2) {
            due = in.readLong();
        } else {
            due = 0;
        }
    }

    public void setScheduledNow() {
        setScheduled(0);
    }

    public void setIndexValues(String[] indexValues) {
        this.indexValues = indexValues;
    }

    public String[] getIndexValues() {
        return indexValues;
    }

    public String getActor() {
        return actor == null ? "" : actor;
    }

    public long getDue() {
        return due;
    }

    public void setDue(long dueDate) {
        this.due = dueDate;
    }

    public void setDueDays(int days) {
        if (days < 0) resetDue();
        else this.due = System.currentTimeMillis() + days * MPeriod.DAY_IN_MILLISECONDS;
    }

    public void resetDue() {
        this.due = 0;
    }
}
