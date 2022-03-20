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

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.summerclouds.common.core.M;
import org.summerclouds.common.core.log.MLog;
import org.summerclouds.common.core.tool.MString;
import org.summerclouds.common.core.tool.MSystem;

import de.mhus.app.reactive.model.activity.AElement;
import de.mhus.app.reactive.model.activity.APool;

public class RuntimeNode extends MLog implements AElement<APool<?>>, ContextRecipient {

    private static final String CLOSE_ACTIVITY = "closeActivity";
    private static final String MSG_PREFIX = "msg.";
    private static final String ident = "TODO";

    private Map<String, Object> parameters;

    private ProcessContext<?> context;
    private UUID nodeId;

    public RuntimeNode() {}

    public RuntimeNode(UUID uuid) {
        //	    System.out.println("### Create Runtime Note " + MSystem.getObjectId(this));
        //	    System.out.println(MCast.toString("### Create Runtime Note " +
        // MSystem.getObjectId(this) + ": " + uuid, Thread.currentThread().getStackTrace()));
        nodeId = uuid;
    }

    private void addFlowMessage(PNode flow, String name, String msg) {
        addMessage(
                EngineMessage.FLOW_PREFIX
                        + flow.getId()
                        + ","
                        + flow.getState()
                        + ","
                        + name
                        + ","
                        + msg);
    }

    private synchronized void addMessage(String msg) {
        if (parameters == null) parameters = new HashMap<>();
        int next = getNetMessageId();
        log().d("runtime message", ident, msg);
        parameters.put(
                MSG_PREFIX + next, System.currentTimeMillis() + "," + ident + "|" + msg);
        save();
    }

    private int getNetMessageId() {
        int cnt = 0;
        while (parameters.containsKey(MSG_PREFIX + cnt)) cnt++;
        return cnt;
    }

    private void addFlowConnect(UUID previousId, UUID id) {
        parameters.put("connectCount", M.to(parameters.get("connectCount"), 0) + 1);
        addMessage(EngineMessage.CONNECT_PREFIX + previousId + "," + id);
    }

    public int getConnectCount() {
        return M.to(parameters.get("connectCount"), 0);
    }

    private void addStartCreated(PNode flow) {
        addMessage(EngineMessage.START_PREFIX + flow.getId() + "," + flow.getCanonicalName());
    }

    public Map<String, Object> exportParamters() {
        //	    System.out.println("### Export " + MSystem.getObjectId(this) + " " + nodeId + ": " +
        // (parameters == null ? "" : parameters.get(CLOSE_ACTIVITY)));
        return parameters;
    }

    public void importParameters(Map<String, Object> parameters) {
        //        System.out.println("### Import " + MSystem.getObjectId(this) + " " + nodeId + ": "
        // + (parameters == null ? "" : parameters.get(CLOSE_ACTIVITY)));
        this.parameters = parameters;
    }

    public void doEvent(String name, PNode flow, int offset, Object... args) {
        if (name.equals("createActivity")) {
            PNode previous = (PNode) args[3];
            addFlowMessage(flow, name, flow.getCanonicalName());
            if (previous != null) addFlowConnect(previous.getId(), flow.getId());
        } else if (name.equals("createStartNode")) {
            addStartCreated(flow);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = offset; i < args.length; i++) MString.serialize(sb, args[i]);
            addFlowMessage(flow, name, sb.toString());
        }
    }

    public void doErrorMsg(PNode flow, Object... objects) {
        addMessage(
                EngineMessage.ERROR_PREFIX
                        + flow.getId()
                        + ","
                        + flow.getName()
                        + " "
                        + MSystem.toString("Error", objects));
        save();
    }

    public void doDebugMsg(PNode flow, Object... objects) {
        addMessage(
                EngineMessage.DEBUG_PREFIX
                        + flow.getId()
                        + ","
                        + flow.getName()
                        + " "
                        + MSystem.toString("Debug", objects));
        save();
    }

    //	public void closedActivity(PNode flow) {
    //		addFlowMessage(flow.getId(), flow.getName() + " closedActivity");
    //	}

    public void setCloseActivity(UUID id) {
        //	    System.out.println("### Set CloseActivity: " + MSystem.getObjectId(this) + " " +
        // nodeId + ": " + id);
        parameters.put(CLOSE_ACTIVITY, id.toString());
        save();
    }

    public void save() {
        if (context != null) {
            try {
                context.saveRuntime();
            } catch (IOException e) {
                log().e(e);
            }
        } else {
            log().w("Context not set in runtime");
        }
    }

    public void close() {}

    @Override
    public void setContext(ProcessContext<?> context) {
        this.context = context;
    }

    public List<EngineMessage> getMessages() {
        LinkedList<EngineMessage> out = new LinkedList<>();
        int cnt = 0;
        while (parameters.containsKey(MSG_PREFIX + cnt)) {
            out.add(new EngineMessage(String.valueOf(parameters.get(MSG_PREFIX + cnt))));
            cnt++;
        }
        return out;
    }

    public UUID getCloseActivity() {
        Object closeId = parameters.get(CLOSE_ACTIVITY);
        //		 System.out.println("Get CLose Activity " + MSystem.getObjectId(this) + " " + nodeId +
        // ": " + closeId);
        if (closeId == null) return null;
        return UUID.fromString(String.valueOf(closeId));
    }

    @Override
    public String toString() {
        return MSystem.toString(
                this, nodeId, parameters == null ? "?" : parameters.get(CLOSE_ACTIVITY));
    }
}
